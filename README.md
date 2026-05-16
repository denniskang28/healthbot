# HealthBot —  Health Insurance Chatbot Demo

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

## Demo Chat Scripts

Use these scripts to reliably trigger each AI recommendation path during a demo.
The LLM produces a conclusion after **3 exchanges** (6 messages in history).

### Case 1 → Online Pharmacy (MEDICATION)
*Mild, clear-cut symptoms — LLM recommends medication directly.*

| Turn | You say |
|------|---------|
| 1 | My throat is a bit sore and my nose is slightly congested. |
| 2 | It started yesterday. I have a mild fever, about 37.5°C, no other symptoms. |
| 3 | I haven't taken any medication and I have no known drug allergies. |

**Expected:** LLM diagnoses common cold → prescription shown (amoxicillin, ibuprofen, etc.) + **Go to Online Pharmacy** button.

---

### Case 2 → Online Expert Consultation (ONLINE_CONSULTATION)
*Significant recurring symptoms — LLM recommends speaking with a specialist.*

| Turn | You say |
|------|---------|
| 1 | I've been getting recurring headaches for the past two weeks, mainly around my temples. |
| 2 | Each episode lasts two to three hours. I sometimes feel nauseous and light makes it worse. |
| 3 | Ibuprofen helps a little but the headaches keep coming back. No fever. |

**Expected:** LLM suspects migraine — notable but non-emergency → **Online Expert Consultation** button.

---

### Case 3 → Offline Appointment (OFFLINE_APPOINTMENT)
*Complex symptoms requiring physical examination or lab work.*

| Turn | You say |
|------|---------|
| 1 | I've lost about 5 kg over the past month without trying to lose weight. |
| 2 | I also feel unusually tired and I sweat a lot at night. |
| 3 | No loss of appetite, but I occasionally have a racing heart. There's a family history of diabetes. |

**Expected:** LLM flags possible metabolic or endocrine issue requiring blood work → **Book In-Person Appointment** button.

---

> **Tip:** If the LLM doesn't trigger a conclusion after turn 3, add *"What do you think I should do?"* to nudge it.

---

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
