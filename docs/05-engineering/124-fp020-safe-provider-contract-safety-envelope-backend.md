# FP-020 Task 02 — Safe Provider Contract + Safety Envelope Backend

## Status

| Field | Value |
|-------|-------|
| Task | FP-020 Task 02 |
| Status | **COMPLETED** |
| Type | Backend-only |
| Frontend changes | None |
| Endpoint count | **30 = 16 POST + 14 GET** (unchanged) |
| Migration/schema/config | None |

---

## Provider Contract Summary

### New Types (11 files)

| Type | Kind | Purpose |
|------|------|---------|
| `TurkuProviderMode` | Enum (5) | DETERMINISTIC_STUB, PROVIDER_DRAFT, PROVIDER_DISABLED, PROVIDER_FAILED_SAFE, HUMAN_REVIEW_REQUIRED |
| `TurkuProviderFailureCode` | Enum (8) | Safe failure codes — no auth/credential codes |
| `TurkuProviderTraceReference` | DTO | Opaque audit trace — traceId, stage, alias, timestamp |
| `TurkuAiGenerationRequest` | DTO | Safe AI request — projectId, stage, context, templateId, safeParams |
| `TurkuAiGenerationResult` | DTO | Safe AI result — summary, confidence, review flags, trace |
| `TurkuImageGenerationRequest` | DTO | Safe image request — projectId, sceneNo, prompt, style |
| `TurkuImageGenerationResult` | DTO | Safe image result — safeInternalRef, success, trace |
| `TurkuGenerationSafetyEnvelope` | DTO | Safety wrapper — envelope per generation call |
| `TurkuProviderSafetySanitizer` | Service | Output sanitizer — JWT, URLs, scripts, base64, lyrics, injection |
| `TurkuProviderSafetyPolicy` | Service | Policy enforcer — pre/post generation invariants |
| `TurkuGenerationProviderGateway` | Interface | Provider contract — generateAi + generateImage |

### Modified Types

| Type | Change |
|------|--------|
| `TurkuVideoProject` | Added `providerMode` field (default: DETERMINISTIC_STUB) |

---

## Safety Envelope Summary

Every future provider call is wrapped in `TurkuGenerationSafetyEnvelope`:

| Field | Default | Invariant |
|-------|---------|-----------|
| humanReviewRequired | `true` | Must always be true |
| autoApproved | `false` | Must always be false |
| lyricsIncluded | `false` | Must always be false |
| copyrightClearanceVerified | `false` | Must be false (manual only) |
| outputSanitized | `false` → `true` | Set true after sanitization pass |
| promptInjectionChecked | `false` → `true` | Set true after injection check |

---

## Sanitizer/Policy Summary

### Sanitizer detects and handles:

| Pattern | Action |
|---------|--------|
| JWT (eyJ...) | Exception |
| Bearer token | Exception |
| Script tags / javascript: | Exception |
| Event handlers (onerror, onclick) | Exception |
| data:image / long base64 | Exception |
| password= / secret= / apiKey= | Exception |
| Authorization: header | Exception |
| https:// / http:// URLs | Redacted → [URL_REDACTED] |
| s3:// / gs:// storage paths | Redacted → [URL_REDACTED] |
| Lyrics markers ([Verse], [Chorus]) | Detected (containsForbiddenPattern) |
| Prompt injection ("ignore previous") | Detected (containsPromptInjection) |

### Policy enforces:

| Check | When | Violation |
|-------|------|-----------|
| autoApproved must be false | Pre + post generation | Exception |
| humanReviewRequired must be true | Pre + post generation | Exception |
| lyricsIncluded must be false | Post generation | Exception |
| copyrightClearanceVerified must be false | Post generation | Exception |

---

## Stub/Default Behavior

- `TurkuProviderMode.DETERMINISTIC_STUB` is the default for all projects.
- No real provider calls in Task 02.
- Gateway interface defined but no implementation wired yet.
- Real provider adapter deferred to Task 03/04.

---

## Safety Baseline Preserved

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No generic status update | ✅ |
| No provider/storage/raw/base64/secret exposure | ✅ |
| No unsafe HTML | ✅ |
| No token/JWT/session/principal/authorizationHeader | ✅ |
| No copyrighted lyrics/audio/video reproduction | ✅ |

---

## Deferred to Task 03

- Story/Scenario/Storyboard provider draft integration
- New generate-* endpoints
- Stub adapter implementation
- Real provider wiring
