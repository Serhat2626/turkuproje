# FP-036 — Local Render Manifest & Multi-Artifact Internal Bundle Foundation

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-036: Render Manifest & Multi-Artifact Internal Bundle |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4119 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. FP-035 Baseline Reference

FP-035 hardened artifact lifecycle verification with bounded limits and cleanup.
FP-036 builds on this to represent **multiple artifacts together** as a bundle manifest.

---

## 3. Purpose

Internal metadata-only manifest layer for future multi-scene/story-video render orchestration.
Allows safe reasoning about a collection of generated artifacts without exposing delivery paths,
storage details, commands, logs, binaries, provider payloads, or public URLs.

---

## 4. Bundle Statuses (6)

| Status | Meaning |
|--------|---------|
| CREATED_INTERNAL_ONLY | Bundle created |
| ARTIFACTS_ATTACHED | Artifacts added but not all verified |
| ARTIFACTS_VERIFIED | All artifacts verified |
| READY_FOR_INTERNAL_REVIEW_ONLY | All verified, ready for internal review |
| REJECTED_UNSAFE | Policy violation |
| FAILED_SAFE | Internal error |

**No READY_FOR_PUBLISH. No COMPLETED. No generic status update.**

---

## 5. Artifact Roles (6)

| Role | Allowed |
|------|:-------:|
| PRIMARY_VIDEO_CLIP | ✅ |
| POSTER_FRAME | ✅ |
| AUDIO_STUB | ✅ |
| VERIFICATION_REPORT | ✅ |
| INTERNAL_METADATA | ✅ |
| UNKNOWN | ❌ (rejected) |

---

## 6. Safe Manifest Fields

| Field | Type |
|-------|------|
| opaqueBundleRef | String |
| status | BundleStatus enum |
| artifacts | List of safe artifact refs |
| issueCodes | List of issue code enums |
| artifactCount / verifiedCount / rejectedCount | int |
| totalSizeBytes | long |
| internalOnly=true | boolean |
| publicDeliveryAvailable=false | boolean |
| autoApproved=false | boolean |

---

## 7. Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader

---

## 8. Bundle Policy Limits

| Limit | Value |
|-------|:-----:|
| Max artifacts per bundle | 10 |
| Max total size | 100MB |
| Primary video required | exactly 1 |
| UNKNOWN role | rejected |
| Rejected artifact | bundle rejected |
| Failed artifact | bundle failed safe |

---

## 9. FP-035 Integration

`TurkuRenderBundleArtifactRef.fromLifecycleReport()` converts FP-035 lifecycle reports
into bundle artifact refs seamlessly.

---

## 10. ProcessBuilder Inventory

| Class | ProcessBuilder | Runtime.exec |
|-------|:--------------:|:------------:|
| TurkuFfmpegLocalDevProcessRunner | ✅ (ONLY allowed) | ❌ |
| All other classes | ❌ | ❌ |

---

## 11. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ No real FFmpeg expansion required for this task
- ✅ Real FFmpeg execution remains LOCAL-only/manual-gated
- ✅ CI uses fake/test doubles only
- ✅ Bundle is metadata-only
- ✅ 0 forbidden field violations

---

## 12. Final Handoff

- **FP-036 is CLOSED**
- **FP-001 → FP-036 CLOSED** (36 features)
- Next feature may start from **FP-037**
