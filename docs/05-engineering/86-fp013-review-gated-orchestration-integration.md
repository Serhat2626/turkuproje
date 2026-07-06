# FP-013 Task 05 — Review-Gated Orchestration Integration for Generated Content/Assets

## Overview

This document describes the integration of `ReviewHandoffService` into all generation/orchestration
completion points via the new `GenerationReviewHandoffAdapter`. After each generation service
successfully produces output, it now automatically hands off the generated entity to the review
workflow, moving it from `AI_GENERATED` to `NEEDS_REVIEW`.

## Scope

### In Scope
- `GenerationReviewHandoffAdapter` — safe adapter wrapping `ReviewHandoffService`
- Integration into 5 generation services (Scenario, Storyboard, PlatformPackage, ContentProject, Image)
- Deferred integration for VisualAssetPlan (no `ReviewTargetType.VISUAL_ASSET_PLAN`)
- Comprehensive tests (adapter behavior, idempotency, safety, failure, endpoint regression)

### Out of Scope
- No new enum values in `ReviewTargetType` or `ReviewHandoffSource`
- No new endpoints, migrations, or schema changes
- No publish/export/CDN/auto-approval/public URL/signed URL
- No storage key or provider URL exposure

## Architecture

### GenerationReviewHandoffAdapter

**Package:** `com.anadoluatlasi.backend.review`

Safe adapter that wraps `ReviewHandoffService` for generation/orchestration completion points.
Provides type-safe factory methods per generation type.

```
GenerationReviewHandoffAdapter
├── handoffScenario(UUID, String, String) → ReviewHandoffResult
├── handoffStoryboard(UUID, String, String) → ReviewHandoffResult
├── handoffPackage(UUID, String, String) → ReviewHandoffResult
├── handoffImageAsset(UUID, String, String) → ReviewHandoffResult
└── handoffContentProject(UUID, String, String) → ReviewHandoffResult
```

Each method:
1. Creates `ReviewHandoffRequest.createSafe(...)` with correct `ReviewTargetType`, `ReviewHandoffSource`, `ReviewActorRole.SYSTEM`
2. Delegates to `ReviewHandoffService.handoffForReview(request)`
3. Logs success at INFO level (targetType, targetId, created/preserved)
4. Catches ALL exceptions at WARN level, returns null (non-fatal)

### Integration Points

| Service | Generation Type | ReviewTargetType | ReviewHandoffSource | Call Location |
|---------|----------------|------------------|---------------------|---------------|
| `AiGenerationService` | Content Draft | `CONTENT_PROJECT` | `CONTENT_PROJECT_GENERATION` | `handleSuccess()` after persist |
| `ScenarioGenerationService` | Scenario | `SCENARIO` | `SCENARIO_GENERATION` | `handleSuccess()` after persist |
| `StoryboardGenerationService` | Storyboard | `STORYBOARD` | `STORYBOARD_GENERATION` | `handleSuccess()` after persist |
| `PlatformPackageGenerationService` | Platform Package | `PACKAGE` | `PACKAGE_GENERATION` | Inside package loop, per draft |
| `ImageGenerationOrchestrationService` | Image Asset | `IMAGE_ASSET` | `IMAGE_GENERATION` | `handleProviderSuccess()` after asset creation |
| `VisualAssetPlanGenerationService` | Visual Asset Plan | **DEFERRED** | **DEFERRED** | Comment only — no ReviewTargetType |

### PlatformPackage Special Case

`PlatformPackageGenerationService` generates **4 platform packages** per invocation (YouTube Long-form,
YouTube Shorts, Instagram Reels, Facebook Reels). Each package gets its own handoff call inside the
generation loop, ensuring per-package review tracking.

### VisualAssetPlan Deferral

`VisualAssetPlanGenerationService` generates image prompt PLANS (text), not actual images.
`ReviewTargetType` does not include `VISUAL_ASSET_PLAN`. The handoff is deferred with a comment:
- Image generation handoff (`IMAGE_ASSET`) is handled at image generation completion
- Visual asset plans produce prompt texts, not reviewable media assets

## Safety Boundaries

### Enforced
- ✅ `ReviewActorRole.SYSTEM` — cannot approve, reject, or review
- ✅ `ReviewHandoffRequest.createSafe()` — all text sanitized via `ReviewTextSafety`
- ✅ Non-fatal handoff — generation succeeds even if handoff fails
- ✅ `AI_GENERATED` does NOT imply approval
- ✅ No auto-approval in any path

### Prohibited
- ❌ No publish/export/CDN
- ❌ No public/signed URL generation
- ❌ No provider URL, storage key, or secret exposure
- ❌ No stack trace in logs or responses
- ❌ No schema changes or migrations
- ❌ No new endpoints

## Test Coverage

### GenerationReviewHandoffAdapterTest

| Category | Tests | Description |
|----------|-------|-------------|
| A. Adapter Behavior | 8 | Per-type handoff, createSafe, actor reference, no auto-approval |
| B. Idempotency | 6 | Repeated handoff, NEEDS_REVIEW/IN_REVIEW/CHANGES_REQUESTED/APPROVED/REJECTED preserved |
| C. Safety Regression | 5 | No forbidden fields, adapter dependencies, no auto-approval, source safety, @Service |
| D. Failure Safety | 6 | RuntimeException/ReviewHandoffException/NPE → null, no throw, multi-package failures |
| E. Endpoint Inventory | 3 | 14 endpoints (8 POST + 6 GET), no forbidden paths, no controller annotations |
| F. Preview Safety | 4 | SafeAdminPreview/AdminReviewHandoffResponse/ReviewHandoffRequest no forbidden fields, no migration |

## Files Changed

### New
- `GenerationReviewHandoffAdapter.java` — Safe adapter
- `GenerationReviewHandoffAdapterTest.java` — 32 tests across 6 categories

### Modified (Production)
- `ScenarioGenerationService.java` — `ReviewHandoffService` → `GenerationReviewHandoffAdapter`, handoff call
- `StoryboardGenerationService.java` — Same refactor
- `PlatformPackageGenerationService.java` — Same refactor, per-package handoff in loop
- `VisualAssetPlanGenerationService.java` — Removed `ReviewHandoffService`, added deferred comment
- `AiGenerationService.java` — Added `GenerationReviewHandoffAdapter`, handoff call
- `ImageGenerationOrchestrationService.java` — Added `GenerationReviewHandoffAdapter`, handoff call
- `ReviewHandoffService.java` — Updated javadoc (wired into orchestration)

### Modified (Tests)
- `ScenarioGenerationServiceTest.java` — Constructor updated for adapter
- `StoryboardGenerationServiceTest.java` — Constructor updated for adapter
- `PlatformPackageGenerationServiceTest.java` — Constructor updated for adapter
- `VisualAssetPlanGenerationServiceTest.java` — Removed ReviewHandoffService mock
- `AiGenerationServiceTest.java` — Constructor updated for adapter
- `ImageGenerationOrchestrationServiceTest.java` — Constructor updated for adapter
- `ScenarioWorkflowSmokeTest.java` — Constructor updated for adapter

## Verification

- `mvn compile` — ✅ SUCCESS
- `mvn test` — Pending (after test updates)
- Docker build — Pending
