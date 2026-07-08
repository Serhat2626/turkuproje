# RPE-003 — Render Pipeline Configuration Boundary Design

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RPE-003: Render Pipeline Config Boundary |
| **Phase** | RPE — Real Production Execution Phase |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4746 (was 4730, +16) |
| **Frontend Tests** | 2552 (was 2532, +20) |
| **Combined** | 7298 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Goal

Design the render pipeline configuration boundary for future video production, keeping render/FFmpeg execution fully disabled and non-executable.

---

## 3. DTO/Model/Service Boundaries

### Enums
| Enum | Values | Purpose |
|------|--------|---------|
| TurkuRenderConfigStatus | 5 | NOT_CONFIGURED, CONFIGURATION_DRAFT, BLOCKED_BY_SAFETY, WAITING_FOR_OPERATOR_REVIEW, FAILED_SAFE |
| TurkuRenderConfigIssueCode | 7 | RENDER_ENVIRONMENT_MISSING, FFMPEG_NOT_AVAILABLE, OUTPUT_FORMAT_NOT_SELECTED, RESOLUTION_NOT_SELECTED, SAFETY_PREREQUISITE_NOT_MET, OPERATOR_REVIEW_PENDING, INTERNAL_ERROR |

### Models
| Model | Purpose |
|-------|---------|
| TurkuRenderEnvironmentBoundary | Environment label + configured status only |
| TurkuFfmpegCapabilityMetadata | Capability label + available flag (not execution) |
| TurkuOutputFormatPolicy | Format/resolution/fps metadata |
| TurkuRenderPipelineConfigResult | Config status + env + FFmpeg + output + prerequisites |
| TurkuRenderPipelineConfigService | Provider config → render config mapping |

---

## 4. Render Environment Boundary

- Placeholder only — no real environment path or configuration
- Label + configured status boolean
- Default: "Ortam henuz yapilandirilmadi"

---

## 5. FFmpeg Capability Metadata

5 capabilities (all unavailable):
1. Video birlestirme
2. Ses birlestirme
3. Format donusturme
4. Cozunurluk olcekleme
5. Gecis efektleri

All marked "RPE-003: henuz etkinlestirilmedi" — metadata only, not execution.

---

## 6. Output Format/Resolution Policy

Default: MP4 1920x1080 @ 24fps — metadata only, not enforced.

---

## 7. Disabled-By-Default Execution Policy

- `renderExecutionEnabled` always `false` in RPE-003
- All FFmpeg capabilities `available: false`
- Environment `configured: false`
- 5 safety prerequisites required before future enablement

---

## 8. Admin UI Behavior

- Render environment label + status
- FFmpeg capability list with availability
- Output format/resolution display
- Safety prerequisites list
- Disabled execution status
- Validation summary
- No render, FFmpeg, command, storage, delivery, download, export, publish, approval buttons

---

## 9. Changed Files

### Backend (7 prod + 1 test)
| File | Purpose |
|------|---------|
| TurkuRenderConfigStatus.java | 5 statuses |
| TurkuRenderConfigIssueCode.java | 7 issue codes |
| TurkuRenderEnvironmentBoundary.java | Environment placeholder |
| TurkuFfmpegCapabilityMetadata.java | FFmpeg metadata |
| TurkuOutputFormatPolicy.java | Output policy |
| TurkuRenderPipelineConfigResult.java | Config result DTO |
| TurkuRenderPipelineConfigService.java | Provider → render mapping |
| TurkuRenderPipelineConfigTest.java | 16 tests in 10 groups |

### Frontend (3 files)
| File | Purpose |
|------|---------|
| types/index.ts | +50 lines RPE-003 types |
| RenderPipelineConfigPreview.tsx | Env, FFmpeg, output, prerequisites |
| RenderPipelineConfigPreview.test.ts | 20 tests in 9 groups |

---

## 10. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4730 | 4746 | +16 |
| Frontend | 2532 | 2552 | +20 |
| Combined | 7262 | 7298 | +36 |

---

## 11. Recommended RPE-004

RPE-004: Storage Delivery Configuration Boundary Design
- Safe storage delivery configuration model
- Storage provider boundary placeholder
- Artifact lifecycle policy metadata
- Delivery channel metadata
- All still disabled/non-executable

---

## 12. RPE-003 Final Status

**RPE-003: CLOSED**
