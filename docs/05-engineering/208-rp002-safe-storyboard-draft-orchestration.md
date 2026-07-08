# RP-002 — Safe Storyboard Draft Orchestration + Admin Preview

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RP-002: Storyboard Draft Orchestration |
| **Phase** | RP — Real Production Story-to-Video MVP |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4592 (was 4572, +20) |
| **Frontend Tests** | 2351 (was 2337, +14) |
| **Combined** | 6943 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Extend RP-001 Türkü name request into a deterministic, copyright-safe storyboard draft with scene list, previewed in admin UI. Fake/test-double orchestration only — no real AI, image gen, or render.

---

## 3. New Backend Models

| Class | Purpose |
|-------|---------|
| TurkuStoryboardDraftStatus | 7 safe statuses |
| TurkuStoryboardDraftIssueCode | 8 issue codes |
| TurkuStoryboardDraftScene | Scene metadata: purpose, mood, visual direction, duration |
| TurkuStoryboardDraftResult | Opaque ref + scene list + orchestration mode |
| TurkuStoryboardDraftOrchestrator | Deterministic fake scene generation |

---

## 4. Orchestration Mode

- `DETERMINISTIC_FAKE_TEST_DOUBLE` — no real AI call
- Scenes generated from fixed Turkish content arrays
- Same input → same scene structure (deterministic)
- Validates: request status, title, copyright intent, scene count, duration

---

## 5. Frontend

| File | Purpose |
|------|---------|
| types/index.ts | +46 lines RP-002 types |
| StoryboardDraftPreview.tsx | Status labels, ready check, scene card, draft summary |
| StoryboardDraftPreview.test.ts | 14 tests in 8 groups |

---

## 6. Safety Exclusions

- ❌ No lyrics/audio/video reproduction
- ❌ No prompt/script/provider payload exposure
- ❌ No real AI provider call
- ❌ No image generation
- ❌ No storage/public/signed/CDN URL
- ❌ No path/command/stdout/stderr/rawLog
- ❌ No token/secret/auth
- ❌ No unsafe HTML
- ❌ No download/export/publish controls
- ❌ No COMPLETED/PUBLISHED/PUBLICLY_AVAILABLE/READY_FOR_PUBLISH

---

## 7. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4572 | 4592 | +20 |
| Frontend | 2337 | 2351 | +14 |
| Combined | 6909 | 6943 | +34 |

---

## 8. Final Handoff

- **RP-002 is CLOSED**
- Next: RP-003
