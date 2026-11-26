package com.sysforedu.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final LeaveRequestService leaveRequestService;

    public AuthController(AuthService authService, LeaveRequestService leaveRequestService) {
        this.authService = authService;
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping("/login")
    public AuthService.AuthResponse login(@Valid @RequestBody AuthRequests.Login request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody AuthRequests.Register request) {
        AuthPrincipal principal = authService.register(request);
        return new RegistrationResponse(principal.role(), principal.name(),
                "TEACHER".equals(request.role()));
    }

    @GetMapping("/session")
    public AuthPrincipal session() {
        return AuthContext.current();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authService.logout(authService.tokenFromAuthorization(request.getHeader("Authorization")));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody AuthRequests.ChangePassword request) {
        authService.changePassword(AuthContext.current(), request.newPassword());
    }

    @GetMapping("/accounts")
    public List<AuthService.AccountView> accounts() {
        return authService.accounts();
    }

    @PatchMapping("/accounts/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setAccountStatus(@PathVariable String username,
                                 @Valid @RequestBody AuthRequests.ReviewAccount request) {
        authService.setAccountStatus(username, request.status());
    }

    @GetMapping("/leave-requests")
    public List<LeaveRequestService.LeaveRequestView> leaveRequests() {
        return leaveRequestService.list(AuthContext.current());
    }

    @PostMapping("/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestService.LeaveRequestView createLeave(
            @Valid @RequestBody AuthRequests.CreateLeave request) {
        return leaveRequestService.create(AuthContext.current(), request);
    }

    @PatchMapping("/leave-requests/{id}")
    public LeaveRequestService.LeaveRequestView reviewLeave(
            @PathVariable String id, @Valid @RequestBody AuthRequests.ReviewLeave request) {
        return leaveRequestService.review(AuthContext.current(), id, request);
    }

    public record RegistrationResponse(String role, String name, boolean pendingApproval) {
    }
}
