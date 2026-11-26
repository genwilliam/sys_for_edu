package com.sysforedu.backend.auth;

import com.sysforedu.backend.business.BusinessDataRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final int SESSION_DAYS = 1;
    private final JdbcTemplate jdbc;
    private final BusinessDataRepository businessData;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(JdbcTemplate jdbc, BusinessDataRepository businessData) {
        this.jdbc = jdbc;
        this.businessData = businessData;
    }

    @Transactional
    public void ensureDemoAccounts() {
        ensureAccount("admin", "Admin@123", "ADMIN", null, "ACTIVE");
        ensureAccount("teacher", "Teacher@123", "TEACHER", "T001", "ACTIVE");
        ensureAccount("student", "Student@123", "STUDENT", "S001", "ACTIVE");
    }

    public AuthResponse login(AuthRequests.Login request) {
        Account account = findAccount(request.username())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        if (!matches(request.password(), account.salt(), account.passwordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!"ACTIVE".equals(account.status())) {
            throw new AuthException(HttpStatus.FORBIDDEN,
                    "PENDING".equals(account.status()) ? "教师账号正在等待管理员审核" : "该账号已被停用");
        }
        AuthPrincipal principal = principal(account);
        String token = randomToken();
        jdbc.update("DELETE FROM auth_sessions WHERE expires_at <= CURRENT_TIMESTAMP");
        jdbc.update("INSERT INTO auth_sessions (token_hash, username, expires_at) VALUES (?, ?, ?)",
                tokenHash(token), account.username(), Timestamp.valueOf(LocalDateTime.now().plusDays(SESSION_DAYS)));
        return new AuthResponse(token, principal);
    }

    public AuthPrincipal authenticate(String token) {
        if (token == null || token.isBlank()) return null;
        List<Account> accounts = jdbc.query("""
                SELECT account.username, account.password_salt, account.password_hash,
                       account.account_role, account.person_id, account.status,
                       account.must_change_password
                FROM auth_sessions session
                JOIN auth_accounts account ON account.username = session.username
                WHERE session.token_hash = ? AND session.expires_at > CURRENT_TIMESTAMP
                """, (rs, rowNum) -> new Account(rs.getString("username"), rs.getString("password_salt"),
                rs.getString("password_hash"), rs.getString("account_role"), rs.getString("person_id"),
                rs.getString("status"), rs.getBoolean("must_change_password")), tokenHash(token));
        if (accounts.isEmpty() || !"ACTIVE".equals(accounts.get(0).status())) return null;
        return principal(accounts.get(0));
    }

    public AuthPrincipal session(String token) {
        AuthPrincipal principal = authenticate(token);
        if (principal == null) throw new AuthException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        return principal;
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jdbc.update("DELETE FROM auth_sessions WHERE token_hash = ?", tokenHash(token));
        }
    }

    @Transactional
    public void changePassword(AuthPrincipal principal, String password) {
        if (password.length() < 8) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "新密码至少需要 8 位");
        }
        PasswordParts parts = passwordParts(password);
        jdbc.update("""
                UPDATE auth_accounts
                SET password_salt = ?, password_hash = ?, must_change_password = FALSE
                WHERE username = ?
                """, parts.salt(), parts.hash(), principal.username());
        jdbc.update("DELETE FROM auth_sessions WHERE username = ?", principal.username());
    }

    @Transactional
    public AuthPrincipal register(AuthRequests.Register request) {
        String domain = "TEACHER".equals(request.role()) ? "teachers" : "students";
        Map<String, Object> profile = businessData.findById(domain,
                        request.personId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "未找到对应的教师/学生档案"));
        if ("TEACHER".equals(request.role()) && !"active".equals(profile.get("status"))) {
            throw new AuthException(HttpStatus.FORBIDDEN, "仅在职教师可以注册账号");
        }
        if ("STUDENT".equals(request.role()) && !"在读".equals(profile.get("status"))) {
            throw new AuthException(HttpStatus.FORBIDDEN, "仅在读学生可以注册账号");
        }
        Object rawPhone = profile.get("phone");
        if (!(rawPhone instanceof String phone) || !phone.equals(request.phone())) {
            throw new AuthException(HttpStatus.CONFLICT, "档案编号与手机号不匹配，无法验证身份");
        }
        String accountRole = request.role();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM auth_accounts WHERE username = ?",
                Integer.class, request.username()) > 0) {
            throw new AuthException(HttpStatus.CONFLICT, "该登录账号已被使用");
        }
        if (jdbc.queryForObject("""
                SELECT COUNT(*) FROM auth_accounts WHERE account_role = ? AND person_id = ?
                """, Integer.class, accountRole, request.personId()) > 0) {
            throw new AuthException(HttpStatus.CONFLICT, "该教师/学生档案已绑定账号");
        }
        PasswordParts password = passwordParts(request.password());
        String status = "TEACHER".equals(accountRole) ? "PENDING" : "ACTIVE";
        jdbc.update("""
                INSERT INTO auth_accounts
                (username, password_salt, password_hash, account_role, person_id, status, must_change_password)
                VALUES (?, ?, ?, ?, ?, ?, FALSE)
                """, request.username(), password.salt(), password.hash(), accountRole, request.personId(), status);
        return principal(findAccount(request.username()).orElseThrow());
    }

    public List<AccountView> accounts() {
        List<AccountRow> rows = jdbc.query("""
                SELECT username, account_role, person_id, status, created_at
                FROM auth_accounts ORDER BY created_at DESC
                """, (rs, rowNum) -> new AccountRow(rs.getString("username"), rs.getString("account_role"),
                rs.getString("person_id"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime()));
        return rows.stream().map(account -> {
            String username = account.username();
            String role = account.role();
            String personId = account.personId();
            String domain = "TEACHER".equals(role) ? "teachers" : "STUDENT".equals(role) ? "students" : "";
            Map<String, Object> profile = domain.isEmpty()
                    ? Map.of("name", "系统管理员", "campus", "全部校区")
                    : businessData.findById(domain, personId).orElse(Map.of("name", username, "campus", ""));
            return new AccountView(username, role, personId, String.valueOf(profile.get("name")),
                    String.valueOf(profile.get("campus")), account.status(), account.createdAt());
        }).toList();
    }

    @Transactional
    public void setAccountStatus(String username, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "账号状态无效");
        }
        int updated = jdbc.update("""
                UPDATE auth_accounts SET status = ? WHERE username = ? AND account_role <> 'ADMIN'
                """, status, username);
        if (updated == 0) throw new AuthException(HttpStatus.NOT_FOUND, "账号不存在或不能修改管理员账号");
        if ("DISABLED".equals(status)) {
            jdbc.update("DELETE FROM auth_sessions WHERE username = ?", username);
        }
    }

    public String tokenFromAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        return authorization.substring("Bearer ".length()).trim();
    }

    private void ensureAccount(String username, String rawPassword, String role, String personId, String status) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM auth_accounts WHERE username = ?",
                Integer.class, username) > 0) return;
        PasswordParts password = passwordParts(rawPassword);
        jdbc.update("""
                INSERT INTO auth_accounts
                (username, password_salt, password_hash, account_role, person_id, status, must_change_password)
                VALUES (?, ?, ?, ?, ?, ?, TRUE)
                """, username, password.salt(), password.hash(), role, personId, status);
    }

    private java.util.Optional<Account> findAccount(String username) {
        return jdbc.query("""
                SELECT username, password_salt, password_hash, account_role, person_id,
                       status, must_change_password
                FROM auth_accounts WHERE username = ?
                """, (rs, rowNum) -> new Account(rs.getString("username"), rs.getString("password_salt"),
                rs.getString("password_hash"), rs.getString("account_role"), rs.getString("person_id"),
                rs.getString("status"), rs.getBoolean("must_change_password")), username).stream().findFirst();
    }

    private AuthPrincipal principal(Account account) {
        if ("ADMIN".equals(account.role())) {
            return new AuthPrincipal(account.username(), account.role(), null, "系统管理员", "全部校区",
                    account.mustChangePassword());
        }
        String domain = "TEACHER".equals(account.role()) ? "teachers" : "students";
        Map<String, Object> profile = businessData.findById(domain, account.personId())
                .orElseThrow(() -> new AuthException(HttpStatus.FORBIDDEN, "账号关联的档案不存在"));
        String campus = String.valueOf(profile.getOrDefault("campus", ""));
        return new AuthPrincipal(account.username(), account.role(), account.personId(),
                String.valueOf(profile.get("name")), campus, account.mustChangePassword());
    }

    private PasswordParts passwordParts(String password) {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        return new PasswordParts(encodedSalt, derive(password, salt));
    }

    private static boolean matches(String password, String encodedSalt, String expectedHash) {
        byte[] salt = Base64.getDecoder().decode(encodedSalt);
        return MessageDigest.isEqual(derive(password, salt).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private static String derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
        try {
            return Base64.getEncoder().encodeToString(
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
        } catch (Exception exception) {
            throw new IllegalStateException("密码哈希算法不可用", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String tokenHash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("会话令牌哈希算法不可用", exception);
        }
    }

    private record Account(String username, String salt, String passwordHash, String role,
                           String personId, String status, boolean mustChangePassword) {
    }

    private record AccountRow(String username, String role, String personId, String status,
                              LocalDateTime createdAt) {
    }

    private record PasswordParts(String salt, String hash) {
    }

    public record AuthResponse(String token, AuthPrincipal user) {
    }

    public record AccountView(String username, String role, String personId, String name,
                              String campus, String status, LocalDateTime createdAt) {
    }
}
