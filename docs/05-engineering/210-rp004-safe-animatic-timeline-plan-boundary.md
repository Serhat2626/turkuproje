# RP-004 — Safe Animatic Timeline Plan Boundary + Admin Preview

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RP-004: Animatic Timeline Plan |
| **Phase** | RP — Real Production Story-to-Video MVP |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4627 (was 4609, +18) |
| **Frontend Tests** | 2378 (was 2365, +13) |
| **Combined** | 7005 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Deterministic safe animatic/timeline plan from storyboard draft + scene asset plan. Prepares future video assembly boundary without rendering, FFmpeg, audio, or image generation.

---

## 3. New Backend Models

| Class | Purpose |
|-------|---------|
| TurkuAnimaticTimelinePlanStatus | 6 statuses |
| TurkuAnimaticTimelinePlanIssueCode | 6 issue codes |
| TurkuAnimaticTimelineItem | Per-scene: start/duration/transition/camera/pacing/beat |
| TurkuAnimaticTimelinePlanResult | Overall: items list + total duration + planner mode |
| TurkuAnimaticTimelinePlanner | Deterministic fake planner |

---

## 4. Timeline Plan vs Real Render

- Timeline plan = metadata-only sequencing plan
- NOT FFmpeg execution, NOT render, NOT assembly
- Safe transition/camera/pacing metadata only
- Real assembly deferred to future task

---

## 5. Frontend

| File | Purpose |
|------|---------|
| types/index.ts | +41 lines RP-004 types |
| AnimaticTimelinePreview.tsx | Status labels, item render, timeline summary |
| AnimaticTimelinePreview.test.ts | 13 tests in 8 groups |

---

## 6. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4609 | 4627 | +18 |
| Frontend | 2365 | 2378 | +13 |
| Combined | 6974 | 7005 | +31 |

---

## 7. Final Handoff

- **RP-004 is CLOSED**
- Next: RP-005
