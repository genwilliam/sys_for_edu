package com.sysforedu.backend.auth;

public final class AuthContext {
    private static final ThreadLocal<AuthPrincipal> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static AuthPrincipal current() {
        return CURRENT.get();
    }

    static void set(AuthPrincipal principal) {
        CURRENT.set(principal);
    }

    static void clear() {
        CURRENT.remove();
    }
}
