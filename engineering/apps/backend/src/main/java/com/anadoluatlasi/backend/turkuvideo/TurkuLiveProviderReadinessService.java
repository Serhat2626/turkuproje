package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * LIVE-001: Evaluates provider credential presence and readiness.
 * No real provider call. Credential presence status only.
 */
public class TurkuLiveProviderReadinessService {

    private static final List<String> PREREQUISITES = List.of(
            "Kimlik bilgisi mevcut olmali",
            "Operator tarafindan etkinlestirilmis olmali",
            "Kill switch devre disi olmali",
            "RPE yurutme paketi hazir olmali",
            "Gorsel prompt paketi onaylanmis olmali");

    /**
     * Evaluate provider readiness.
     * @param credentialPresent whether a credential is configured (presence only, no value)
     * @param operatorEnabled whether the operator explicitly enabled the provider
     * @param killSwitch kill switch state
     */
    public TurkuLiveProviderReadinessReport evaluate(
            boolean credentialPresent, boolean operatorEnabled, TurkuKillSwitch killSwitch) {

        if (killSwitch == null) killSwitch = TurkuKillSwitch.defaultActive();

        List<TurkuLiveProviderIssueCode> issues = new ArrayList<>();
        List<String> blocked = new ArrayList<>();

        if (killSwitch.isActive()) {
            issues.add(TurkuLiveProviderIssueCode.KILL_SWITCH_ACTIVE);
            blocked.add("Kill switch aktif: " + killSwitch.getReason());
        }
        if (!credentialPresent) {
            issues.add(TurkuLiveProviderIssueCode.CREDENTIAL_NOT_PRESENT);
            blocked.add("Kimlik bilgisi mevcut degil");
        }
        if (!operatorEnabled) {
            issues.add(TurkuLiveProviderIssueCode.OPERATOR_ENABLEMENT_REQUIRED);
            blocked.add("Operator tarafindan etkinlestirilmedi");
        }

        TurkuLiveProviderStatus status;
        if (killSwitch.isActive()) {
            status = TurkuLiveProviderStatus.BLOCKED_BY_KILL_SWITCH;
        } else if (!credentialPresent) {
            status = TurkuLiveProviderStatus.DISABLED_BY_DEFAULT;
        } else if (!operatorEnabled) {
            status = TurkuLiveProviderStatus.WAITING_FOR_OPERATOR_ENABLEMENT;
        } else {
            status = TurkuLiveProviderStatus.CREDENTIAL_PRESENT;
        }

        String credLabel = credentialPresent ? "Kimlik bilgisi mevcut (deger gizli)" : "Kimlik bilgisi mevcut degil";

        return new TurkuLiveProviderReadinessReport(
                status, issues,
                credentialPresent, credLabel,
                operatorEnabled, killSwitch,
                PREREQUISITES, blocked,
                "OpenAI-uyumlu gorsel saglayici",
                String.format("Dogrulama: %d sorun, saglayici %s",
                        issues.size(), status == TurkuLiveProviderStatus.CREDENTIAL_PRESENT ? "hazir" : "hazir degil"));
    }
}
