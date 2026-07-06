package com.anadoluatlasi.backend.review;

/**
 * FP-018 Task 02: Admin actor resolution mode.
 *
 * <p>Defines how an admin actor identity was resolved.
 * No mode trusts client-provided role as production auth.</p>
 *
 * <p>Forbidden modes that MUST NEVER be added:
 * CLIENT_TRUSTED, REQUEST_ROLE_TRUSTED, BYPASS_AUTH, FORCE_ADMIN.</p>
 */
public enum AdminActorResolutionMode {

    /**
     * Production: actor resolved from authenticated security context.
     * Not implemented yet — placeholder for future auth integration.
     */
    AUTHENTICATED_CONTEXT,

    /**
     * Test/dev: actor resolved from request-provided role as fallback.
     * Explicitly marked as non-production. Safe for current internal use.
     */
    TEST_FALLBACK,

    /**
     * System-internal: actor is the system itself.
     * No human auth needed. SYSTEM cannot approve.
     */
    SYSTEM_INTERNAL
}
