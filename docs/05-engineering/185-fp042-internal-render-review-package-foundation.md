# FP-042 — Internal Render Review Package Foundation

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-042: Internal Render Review Package |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4235 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Internal render review package foundation for stitched synthetic artifacts.
Packages safe metadata from FP-035/036/040/041 into an internal review summary.
No auto-approval. No READY_FOR_PUBLISH. No public delivery.

---

## 3. Package Statuses (5)

| Status | Meaning |
|--------|---------|
| CREATED_INTERNAL_ONLY | Created |
| ARTIFACT_METADATA_ATTACHED | Artifacts attached |
| READY_FOR_INTERNAL_REVIEW_ONLY | Ready for review |
| REJECTED_UNSAFE | Artifacts rejected |
| FAILED_SAFE | Internal error |

---

## 4. Issue Codes (13)

NONE, NO_PRIMARY_ARTIFACT, ARTIFACT_NOT_VERIFIED, ARTIFACT_REJECTED, ARTIFACT_FAILED, ARTIFACT_ROLE_UNKNOWN, TOO_MANY_ARTIFACTS, DUPLICATE_ARTIFACT_REF, MISSING_ARTIFACT_REF, LIFECYCLE_STATUS_INVALID, BUNDLE_REF_MISSING, METADATA_INCOMPLETE

---

## 5. Safe Review Package Fields

opaqueReviewPackageRef, opaqueJobRef, opaqueBundleRef, status, issueCodes, artifactSummaries (role/verification/format/duration/fps/width/height/sizeBytes), sceneCount, inputClipCount, totalDurationSeconds, maxWidth/Height/Fps, totalSizeBytes, internalOnly=true

---

## 6. Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader, lyrics, scriptBody, promptText, copyrightedContent

---

## 7. Policy

- Max artifacts per package: 10
- At least one PRIMARY_VIDEO_CLIP required
- All artifacts must be verified metadata-only
- No auto-approval
- No public delivery readiness

---

## 8. Integration

- **FP-035**: Lifecycle verification status consumed
- **FP-036**: Bundle manifest ref accepted
- **FP-040/041**: Stitched artifact metadata consumed

---

## 9. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ No auto-approval or publish readiness
- ✅ No READY_FOR_PUBLISH/COMPLETED statuses
- ✅ ProcessBuilder only in TurkuFfmpegLocalDevProcessRunner
- ✅ 0 forbidden field violations

---

## 10. Final Handoff

- **FP-042 is CLOSED**
- **FP-001 → FP-042 CLOSED** (42 features)
- Next feature may start from **FP-043**
