package com.sysforedu.backend.auth;

public record AuthPrincipal(
        String username,
        String role,
        String personId,
        String name,
        String campus,
        boolean mustChangePassword) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
