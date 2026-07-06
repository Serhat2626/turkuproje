package com.anadoluatlasi.backend.review;

import org.springframework.stereotype.Service;

/**
 * FP-018 Task 02: Admin actor resolver service.
 *
 * <p>Resolves admin actor identity/role for review workflow actions.
 * Currently defaults to TEST_FALLBACK mode (wraps request-provided role).
 * Future production auth will use AUTHENTICATED_CONTEXT mode.</p>
 *
 * <p>Read-only. No mutation. No ReviewApplicationService dependency.
 * No JWT/session/security context exposure. No provider/storage calls.</p>
 */
@Service
public class AdminActorResolver {

    /**
     * Resolve actor from a resolution request.
     *
     * <p>TEST_FALLBACK: wraps the request-provided role as-is.
     * SYSTEM_INTERNAL: resolves only when systemInternalAllowed=true.
     * AUTHENTICATED_CONTEXT: not yet implemented (future production auth).</p>
     *
     * @param request resolution request with client-provided role and flags
     * @return resolved actor with mode marker
     * @throws AdminActorResolutionException if resolution fails safely
     */
    public ResolvedAdminActor resolve(AdminActorResolutionRequest request) {
        if (request == null) {
            throw new AdminActorResolutionException(
                    "Çözümleme isteği boş olamaz",
                    "RESOLUTION_REQUEST_NULL",
                    null);
        }

        ReviewActorRole role = request.getRequestedActorRole();
        if (role == null) {
            throw new AdminActorResolutionException(
                    "Aktör rolü belirtilmedi",
                    "ACTOR_ROLE_NULL",
                    null);
        }

        // SYSTEM resolves only with systemInternalAllowed
        if (role == ReviewActorRole.SYSTEM) {
            if (!request.isSystemInternalAllowed()) {
                throw new AdminActorResolutionException(
                        "SYSTEM rolü bu bağlamda izin verilmedi",
                        "SYSTEM_NOT_ALLOWED",
                        ReviewActorRole.SYSTEM);
            }
            return ResolvedAdminActor.builder()
                    .actorRole(ReviewActorRole.SYSTEM)
                    .actorReference(sanitizeReference(request.getRequestedActorReference(), "SYSTEM"))
                    .resolutionMode(AdminActorResolutionMode.SYSTEM_INTERNAL)
                    .safeDisplayName("Sistem")
                    .safeReasonCode(request.getSafeReasonCode())
                    .safeMessage("Dahili sistem aktörü olarak çözümlendi")
                    .build();
        }

        // Fallback resolution for CREATOR/REVIEWER/ADMIN
        if (!request.isFallbackAllowed()) {
            throw new AdminActorResolutionException(
                    "Geliştirme ortamı geri dönüşü devre dışı ve üretim kimlik doğrulaması henüz uygulanmadı",
                    "FALLBACK_NOT_ALLOWED",
                    role);
        }

        return ResolvedAdminActor.builder()
                .actorRole(role)
                .actorReference(sanitizeReference(request.getRequestedActorReference(), role.name()))
                .resolutionMode(AdminActorResolutionMode.TEST_FALLBACK)
                .safeDisplayName(safeDisplayName(role))
                .safeReasonCode(request.getSafeReasonCode())
                .safeMessage("Test/geliştirme ortamı geri dönüşü olarak çözümlendi")
                .build();
    }

    /**
     * Convenience: resolve from raw request fields (test/dev shortcut).
     */
    public ResolvedAdminActor resolveTestFallback(ReviewActorRole role, String actorReference) {
        return resolve(AdminActorResolutionRequest.builder()
                .requestedActorRole(role)
                .requestedActorReference(actorReference)
                .fallbackAllowed(true)
                .build());
    }

    /**
     * Convenience: resolve SYSTEM actor for internal operations.
     */
    public ResolvedAdminActor resolveSystemInternal(String reason) {
        return resolve(AdminActorResolutionRequest.builder()
                .requestedActorRole(ReviewActorRole.SYSTEM)
                .requestedActorReference("system-internal")
                .systemInternalAllowed(true)
                .safeReasonCode(reason)
                .build());
    }

    private String sanitizeReference(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        // Strip anything that looks like a token/credential
        String sanitized = raw.replaceAll("[<>]", "")
                .replaceAll("(?i)bearer\\s+\\S+", "[REDACTED]")
                .replaceAll("(?i)password[=:]\\S+", "[REDACTED]");
        return sanitized.length() > 200 ? sanitized.substring(0, 200) : sanitized;
    }

    private String safeDisplayName(ReviewActorRole role) {
        return switch (role) {
            case CREATOR -> "İçerik Oluşturucu";
            case REVIEWER -> "İnceleyici";
            case ADMIN -> "Yönetici";
            case SYSTEM -> "Sistem";
        };
    }
}
