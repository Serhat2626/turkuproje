# FP-028 — Real FFmpeg Internal Dev/Test Enablement

## 1. Status and Scope

| Field | Value |
|-------|-------|
| **Feature** | FP-028 |
| **Goal** | Enable real FFmpeg only for internal local/dev/test behind fail-closed gates |
| **Type** | Backend model + boundary contract |
| **Public Delivery** | NO |
| **Production Enablement** | NO |

## 2. Baseline Dependency

| Feature | Provides |
|---------|----------|
| FP-024 | Internal render config/workspace/execution/artifact safety |
| FP-025 | Internal render job lifecycle/progress/readiness |
| FP-026 | Human review gate (internal next step only) |
| FP-027 | Queue/retry/expiration (dispatch safety) |

## 3. Product Goal

> Bir geliştirici olarak, yerel/dev/test ortamında gerçek FFmpeg'i kontrollü şekilde çalıştırabilmek istiyorum — staging/prod/bilinmeyen ortamlarda otomatik olarak devre dışı kalmalı.

## 4. Non-Goals
- No public delivery / publish / export / download / player
- No production FFmpeg enablement
- No storage provider / CDN / signed URL
- No raw path / command line / stdout / stderr exposure
- No auto-approval
- No generic status update
- No ProcessBuilder/Runtime.exec in Task 01

## 5. Execution Gate Model

### Mode: TurkuFfmpegRealExecutionMode
- DISABLED — fail-closed default
- DRY_RUN — returns simulated safe result
- SIMULATED — returns simulated safe result with timing
- REAL_DEV_TEST_ONLY — real execution allowed only in safe environments

### Environment: TurkuFfmpegExecutionEnvironment
- LOCAL — real allowed
- DEV — real allowed
- TEST — real allowed
- STAGING — real denied
- PROD — real denied
- UNKNOWN — real denied

### Gate Rule
Real execution allowed ONLY when ALL of:
1. mode = REAL_DEV_TEST_ONLY
2. environment ∈ {LOCAL, DEV, TEST}
3. internalDevTestEnabled = true (explicit flag)
4. FP-026 review approved for internal next step (where applicable)
5. FP-027 queue dispatch safe (where applicable)

## 6. Binary Policy: TurkuFfmpegBinaryPolicy
- SYSTEM_FFMPEG_LOCAL — system ffmpeg for local dev
- TEST_FIXTURE_FFMPEG — test-only fixture
- DISABLED_BINARY — no binary available
- No raw path exposure, no executablePath field

## 7. Execution Boundary Contract

### TurkuFfmpegExecutionBoundaryResult
Safe result containing only:
- status enum (NOT_EXECUTED, DRY_RUN_COMPLETE, SIMULATED_COMPLETE, EXECUTED_INTERNAL, FAILED_SAFE, DENIED)
- reason code enum
- opaque job/queue/review refs
- bounded duration/timeout
- no stdout/stderr/raw logs/command line/paths/binary

## 8. Bounded Policy: TurkuFfmpegExecutionLimits
- timeout: 10–300s
- duration: 1–600s
- fps: 1–60
- resolution: ≤3840×2160
- output size: ≤1000MB
- fail closed on invalid

## 9. Proposed Task Breakdown

| Task | Scope |
|------|-------|
| 01 | Design + secure config + execution boundary + tests |
| 02 | Admin UI FFmpeg enablement visibility + closure |
