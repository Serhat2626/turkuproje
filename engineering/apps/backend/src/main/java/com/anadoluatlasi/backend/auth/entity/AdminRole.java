package com.anadoluatlasi.backend.auth.entity;

/**
 * Admin kullanıcı rolleri.
 * Spring Security GrantedAuthority ile eşleştirilir.
 */
public enum AdminRole {
    ADMIN,
    REVIEWER,
    CREATOR;

    /**
     * Spring Security authority formatı: ROLE_ADMIN, ROLE_REVIEWER, ROLE_CREATOR
     */
    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}
