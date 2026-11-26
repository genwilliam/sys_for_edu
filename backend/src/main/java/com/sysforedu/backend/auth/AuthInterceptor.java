package com.sysforedu.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if ("/api/health".equals(path) || "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)) {
            return true;
        }
        String token = authService.tokenFromAuthorization(request.getHeader("Authorization"));
        AuthPrincipal principal = authService.authenticate(token);
        if (principal == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        if (path.startsWith("/api/auth/accounts/") || "/api/auth/accounts".equals(path)) {
            requireAdmin(principal);
        }
        if (path.startsWith("/api/data/") && !HttpMethod.GET.matches(request.getMethod())
                && !principal.isAdmin()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "教师和学生账号不能修改基础业务数据");
        }
        if (path.startsWith("/api/schedule-items/") || "/api/schedule-items".equals(path)) {
            if (!HttpMethod.GET.matches(request.getMethod()) && !principal.isAdmin()) {
                throw new AuthException(HttpStatus.FORBIDDEN, "教师和学生账号不能管理排课");
            }
        }
        if (path.startsWith("/api/leave-requests") && !principal.isAdmin()
                && !"TEACHER".equals(principal.role()) && !"STUDENT".equals(principal.role())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "无权访问请假申请");
        }
        AuthContext.set(principal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        AuthContext.clear();
    }

    private static void requireAdmin(AuthPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "仅管理员可以管理账号");
        }
    }
}
