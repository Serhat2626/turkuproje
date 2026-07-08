# RP-001 — Türkü Name to Safe Storyboard Request Pipeline

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RP-001: Türkü Name Storyboard Request |
| **Phase** | RP — Real Production Story-to-Video MVP |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4572 passed, 0 failures |
| **Frontend Tests** | 2337 passed, 0 failures |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Accept a Türkü name/title and create a copyright-safe storyboard generation request. No real AI call. No lyrics/audio/video reproduction. No image generation.

---

## 3. New Backend Models

| Class | Purpose |
|-------|---------|
| TurkuNameStoryboardRequestStatus | 6 safe statuses |
| TurkuNameStoryboardRequestIssueCode | 11 issue codes |
| TurkuNameStoryboardRequest | Short title + bounded scene/duration/resolution/fps |
| TurkuNameStoryboardRequestResult | Opaque ref + safe title + copyright intent |
| TurkuNameStoryboardRequestPolicy | Title/bounds/lyrics/URL validation |
| TurkuNameStoryboardRequestService | Creates copyright-safe request from title |

---

## 4. Input Policy

- ✅ Short Türkü name/title (max 200 chars)
- ✅ Optional region/category/style
- ✅ Bounded scene count (1-30), duration (5s-10min), fps (1-60), resolution (120-3840)
- ❌ Lyrics / multiline verses → REJECTED
- ❌ URLs → REJECTED
- ❌ Audio/video/media files → Not accepted
- ❌ Path/command/log/token/provider/storage/raw/base64 → Not accepted

---

## 5. Copyright-Safe Boundary

- Output carries `copyrightSafeIntentSummary` in Turkish
- Mode: `TURKU_TITLE_INSPIRATION` — inspired by title only
- No lyrics reproduction, no audio reproduction, no video reproduction

---

## 6. Frontend

| File | Purpose |
|------|---------|
| types/index.ts | +38 lines RP-001 types |
| TurkuNameStoryboardForm.tsx | Status labels, ready check, summary, input warning |
| TurkuNameStoryboardForm.test.ts | 16 tests in 8 groups |

---

## 7. Tests

### Backend (21 new in 8 groups)
- A. Valid request
- B. Title validation (null, empty, long, lyrics, URL)
- C. Bounds validation (scene, duration, fps, resolution)
- D. Forbidden field sweep
- E. No publish statuses (enum sizes)
- F. Endpoint inventory
- G. Factories + ProcessBuilder
- H. Türkçe support

### Frontend (16 new in 8 groups)
- A-H: Status rendering, ready check, summary, warning, forbidden sweep, source safety, type safety, Türkçe

---

## 8. Test Baseline

| Suite | Count |
|-------|-------|
| Backend | 4572 |
| Frontend | 2337 |
| Combined | 6909 |

---

## 9. Final Handoff

- **RP-001 is CLOSED**
- Next: RP-002 (Storyboard Generation Boundary)
