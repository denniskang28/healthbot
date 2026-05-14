# HealthBot — AIA Health Insurance Chatbot Demo

Full-stack health chatbot demo for an insurance company, featuring AI medical consultation, online doctor video consultation, hospital appointment booking, and pharmacy.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌────────────────────┐
│  Android App    │────▶│  Java Backend    │────▶│  Python LLM Service│
│  (AIA Health)   │     │  (Port: 8080)    │     │  (Port: 8000)      │
└─────────────────┘     └──────────────────┘     └────────────────────┘
                                │
                         ┌──────▼───────┐
                         │  React Admin │
                         │  (Port: 3000)│
                         └──────────────┘
```

## Components

| Component | Tech | Description |
|-----------|------|-------------|
| `android/` | Kotlin + Retrofit | AIA Health insurance app with chatbot UI |
| `backend/` | Java Spring Boot | REST API + WebSocket (STOMP) |
| `llm-service/` | Python FastAPI | Multi-provider LLM integration |
| `admin/` | React + Ant Design | Management console |

## Quick Start

### 1. LLM Service (Python)

```bash
cd llm-service
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
```

Create a `.env` file and choose a provider:

```env
# Anthropic Claude (default)
PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...
MODEL=claude-sonnet-4-6

# --- OR OpenAI ChatGPT ---
# PROVIDER=openai
# OPENAI_API_KEY=sk-...
# MODEL=gpt-4o

# --- OR Alibaba Cloud Qwen (千问) ---
# PROVIDER=qwen
# QWEN_API_KEY=sk-...
# MODEL=qwen-plus
```

```bash
uvicorn main:app --reload --port 8000
```

Without an API key the service uses built-in mock responses — the rest of the stack still works.

### 2. Backend (Java)

```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

### 3. Admin Console (React)

```bash
cd admin
npm install
npm run dev
# Open http://localhost:3000
```

### 4. Android App

- Open `android/` in Android Studio (Hedgehog+)
- Run on emulator (AVD, API 26+)
- Backend auto-connects via `10.0.2.2:8080`

## Key User Flows

```
Home → Tap Banner → Chatbot → Chat with AI
                            → AI suggests consultation
                            ├─→ AI Consultation → Prescription → Pharmacy
                            ├─→ Doctor List → Video Call → Prescription → Pharmacy
                            └─→ Book Appointment → Confirm → Back to Chat
```

## Admin Features

- **Dashboard** — Real-time user status via WebSocket
- **Consultations** — All AI & doctor consultations with prescriptions
- **Purchases** — All pharmacy transactions
- **LLM Config** — Switch provider (Anthropic / OpenAI / Qwen), select model, update API key, edit system prompt

## Multi-language

- **Android**: Toggle EN/ZH via toolbar button (persisted in SharedPreferences)
- **Admin**: Toggle EN/中文 via header button (persisted in localStorage)

## Demo Users (pre-loaded)

| ID | Name | Language |
|----|------|----------|
| 1 | Alice Chen | EN |
| 2 | Bob Wang | ZH |
| 3 | Carol Liu | EN |

> Android app uses userId = 1 (Alice Chen) by default.

## Demo Doctors (pre-loaded)

Dr. James Wilson (Cardiology), Dr. Sarah Chen (General Practice),
Dr. Michael Park (Dermatology), Dr. Emily Zhang (Neurology), Dr. David Kim (Pediatrics)
