# Anadolu Studio / Anadolu Atlası

An end-to-end AI-assisted storytelling, media production, and video rendering platform built with **Java 21 / Spring Boot 3.5** and **React 19 / TypeScript**.

## Architecture Overview

```text
turkuproje/
├── engineering/
│   ├── apps/
│   │   ├── backend/         # Spring Boot 3.5 (Java 21, PostgreSQL + Flyway, Redis, MinIO, FFmpeg)
│   │   └── admin-web/       # React 19 + TypeScript 5.8 + Vite 6 + Vitest
│   ├── infrastructure/      # Docker & deployment manifests
│   ├── libraries/           # Shared modules
│   └── workers/             # Background processing workers
├── docs/                    # Architecture, ADRs, engineering & product documentation
├── knowledge/               # Cultural & editorial reference datasets
└── prompts/                 # Structured prompt templates
```

## Key Capabilities

- **Multi-Stage Production Pipeline:** Story research, scenario drafting, storyboard generation, character-consistent visual asset synthesis, TTS narration, and FFmpeg video assembly.
- **Pluggable AI Provider Layer (Mock & Live):** Supports `mock` mode by default for zero-cost local development and testing, with optional live gateways for OpenAI, ElevenLabs, and fal.ai (Flux Pro / Kling).
- **Credential Vault & Safety Boundaries:** Strict separation between provider credentials, runtime configuration, and execution envelopes.
- **Comprehensive Test Suite:** Backend unit/integration tests (JUnit 5 / Spring Boot Test) and frontend tests (Vitest + React Testing Library).

## Local Development

### Prerequisites
- Java 21+ & Maven 3.9+
- Node.js 20+
- Docker & Docker Compose (PostgreSQL 16, Redis 7, MinIO)
- FFmpeg (optional, for local video rendering)

### 1. Environment Setup
```bash
cp .env.example .env
```
By default, `AI_PROVIDER=mock` is active so you can run and test the entire platform locally without external API keys.

### 2. Run Backend
```bash
cd engineering/apps/backend
./mvnw test
./mvnw spring-boot:run
```
Swagger UI is available at `http://localhost:8089/swagger-ui.html`.

### 3. Run Admin Web
```bash
cd engineering/apps/admin-web
npm install
npm test
npm run dev
```
