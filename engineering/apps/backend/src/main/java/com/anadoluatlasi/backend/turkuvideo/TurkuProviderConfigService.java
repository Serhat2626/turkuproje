package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RPE-002: Maps execution gate to image provider configuration boundary.
 * All provider execution disabled. Metadata only.
 */
public class TurkuProviderConfigService {

    private static final TurkuCredentialVaultBoundary DEFAULT_VAULT =
            new TurkuCredentialVaultBoundary(
                    "G\u00f6rsel sa\u011flay\u0131c\u0131 kimlik kasas\u0131",
                    false,
                    "Kimlik bilgisi mevcut de\u011fil");

    private static final List<String> CAPABILITY_LABELS = List.of(
            "Metin-g\u00f6rsel \u00fcretim (gelecek)",
            "G\u00f6rsel varyasyon (gelecek)",
            "Stil transferi (gelecek)",
            "Y\u00fcksek \u00e7\u00f6z\u00fcn\u00fcrl\u00fck \u00f6l\u00e7ekleme (gelecek)");

    public TurkuProviderConfigResult evaluate(TurkuExecutionGateResult gate) {
        if (gate == null) {
            return new TurkuProviderConfigResult(
                    TurkuProviderConfigStatus.BLOCKED_BY_SAFETY,
                    List.of(TurkuProviderConfigIssueCode.INTERNAL_ERROR),
                    null, null, null, List.of(), DEFAULT_VAULT,
                    null, null, false,
                    "Y\u00fcr\u00fctme kap\u0131s\u0131 eksik \u2014 sa\u011flay\u0131c\u0131 yap\u0131land\u0131r\u0131lam\u0131yor");
        }

        List<TurkuProviderConfigIssueCode> issues = new ArrayList<>();
        issues.add(TurkuProviderConfigIssueCode.CREDENTIAL_NOT_PRESENT);
        issues.add(TurkuProviderConfigIssueCode.PROVIDER_TYPE_NOT_SELECTED);
        issues.add(TurkuProviderConfigIssueCode.FALLBACK_NOT_CONFIGURED);

        return new TurkuProviderConfigResult(
                TurkuProviderConfigStatus.NOT_CONFIGURED,
                issues,
                "pcfg:" + UUID.randomUUID().toString().substring(0, 8),
                "Deterministik sahte sa\u011flay\u0131c\u0131 (varsay\u0131lan)",
                TurkuImageProviderType.DETERMINISTIC_FAKE,
                CAPABILITY_LABELS,
                DEFAULT_VAULT,
                "Se\u00e7im politikas\u0131: hen\u00fcz yap\u0131land\u0131r\u0131lmad\u0131",
                "Yedek politikas\u0131: hen\u00fcz yap\u0131land\u0131r\u0131lmad\u0131",
                false,
                String.format("Sa\u011flay\u0131c\u0131 y\u00fcr\u00fctme devre d\u0131\u015f\u0131 \u2014 %d \u00f6n ko\u015ful kar\u015f\u0131lanmad\u0131", issues.size()));
    }
}
