# HealthBot — Root Engineering Guide

## Architecture

```
Android App (Kotlin)
    │  HTTP → 10.0.2.2:8080
    ▼
Spring Boot Backend  :8080  ←→  React Admin  :3000
    │  HTTP → localhost:8000
    ▼
Python LLM Service   :8000
    │  API calls
    ▼
Anthropic / OpenAI / Qwen
```

### Component responsibilities

| Dir | Role | Key constraint |
|-----|------|----------------|
| `backend/` | REST API, business logic, persistence (H2), WebSocket | Single source of truth for all data |
| `llm-service/` | LLM provider abstraction, tool-calling, mock fallback | Stateless; never writes to DB |
| `admin/` | Real-time ops dashboard | Read-only data display + LLM config |
| `android/` | End-user insurance app | userId=1 hardcoded for demo |

---

## Feature development protocol

When adding a new feature, touch components in this order:

```
1. backend/   — define entity, repo, service, controller, DTO
2. llm-service/ — add endpoint only if AI interaction is needed
3. admin/     — add page/table if ops visibility is needed
4. android/   — add screen and Retrofit call
```

Not every feature needs all four components. Decide up front which are required.

---

## Cross-component API contract

Backend → LLM Service (HTTP POST, JSON):

```
POST /chat                  { userId, message, history[], language }
POST /ai-consultation       { userId, consultationId, message, history[], language }
POST /generate-prescription { consultationSummary, language }
GET  /health                → { status, provider, model }
```

LLM Service always returns mock responses when no API key is set — never fails hard.

Android → Backend base URL: `http://10.0.2.2:8080/` (emulator) or `http://localhost:8080/` (device).

---

## Cross-cutting conventions

### Language support (EN / ZH)
Every user-visible string must exist in **both**:
- `android/app/src/main/res/values/strings.xml` (EN)
- `android/app/src/main/res/values-zh/strings.xml` (ZH)
- `admin/src/context/LanguageContext.tsx` → `translations.EN` and `translations.ZH`

### Error handling
- **Backend**: catch exceptions in controllers, return appropriate HTTP status (never let 500 propagate to Android)
- **LLM Service**: all three endpoint functions return `None` on failure → `main.py` falls back to mock
- **Android**: all API calls wrapped in `try/catch` inside `lifecycleScope.launch`; show `Toast` on error
- **Admin**: API failures silently ignored on Dashboard; `message.error()` on save failures

### Naming
- Backend DTOs: `*Dto.java` (suffix, not prefix)
- Backend entities: no suffix, in `model/` package
- Admin pages: `*` PascalCase in `src/pages/`
- Android activities: `*Activity.kt`

### Demo data
Pre-loaded in `backend/.../config/DataInitializer.java`. Add new seed data there; never rely on runtime state for demo flows.

---

## What NOT to change without cross-component review
- `/ws` WebSocket path (Android + backend + admin all reference it)
- `userId=1` assumption in Android (hardcoded in every Activity)
- LLM service port 8000 / backend port 8080 (referenced in multiple files)
- Tool-calling response field names (`responseText`, `isComplete`, `prescription`) — backend parses these from llm-service JSON
