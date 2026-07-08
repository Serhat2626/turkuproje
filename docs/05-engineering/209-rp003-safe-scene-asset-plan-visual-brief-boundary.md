# RP-003 — Safe Scene Asset Plan + Visual Brief Boundary

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RP-003: Scene Asset Plan + Visual Brief |
| **Phase** | RP — Real Production Story-to-Video MVP |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4609 (was 4592, +17) |
| **Frontend Tests** | 2365 (was 2351, +14) |
| **Combined** | 6974 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Per-scene visual asset planning from storyboard draft. Deterministic fake/test-double planner generates safe visual brief summaries (not provider-ready prompts). Blocked scenes tracked with reasons.

---

## 3. New Backend Models

| Class | Purpose |
|-------|---------|
| TurkuSceneAssetPlanStatus | 7 statuses incl. BLOCKED_CONTENT_UNSAFE |
| TurkuSceneAssetPlanIssueCode | 7 issue codes |
| TurkuSceneAssetPlanEntry | Per-scene: brief, shot, framing, atmosphere, style, safety |
| TurkuSceneAssetPlanResult | Overall plan: entries, blocked count, planner mode |
| TurkuSceneAssetPlanner | Deterministic fake planner |

---

## 4. Visual Brief vs Provider Prompt

- Visual brief = human-readable summary (e.g. "Sahne 1: Giriş — huzurlu, Anadolu tarzında")
- NOT a provider-ready prompt (no negative_prompt, cfg_scale, sampler params)
- Safe for admin preview display
- Provider prompt generation deferred to future task

---

## 5. Frontend

| File | Purpose |
|------|---------|
| types/index.ts | +45 lines RP-003 types |
| SceneAssetPlanPreview.tsx | Status labels, card render, plan summary |
| SceneAssetPlanPreview.test.ts | 14 tests in 8 groups |

---

## 6. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4592 | 4609 | +17 |
| Frontend | 2351 | 2365 | +14 |
| Combined | 6943 | 6974 | +31 |

---

## 7. Final Handoff

- **RP-003 is CLOSED**
- Next: RP-004
