# HealthBot Admin Console

React + Ant Design admin console for the HealthBot system.

## Setup

```bash
cd admin
npm install
npm run dev
# Open http://localhost:3000
```

> **Note:** `vite.config.ts` includes `optimizeDeps.esbuildOptions.define: { global: 'globalThis' }` to fix a blank-page issue caused by `sockjs-client` referencing Node.js's `global` in the browser.

## Features

- **Dashboard** — Real-time user status via WebSocket (STOMP over SockJS), session stats
- **Consultations** — All AI & doctor consultation records with prescriptions
- **Purchases** — All pharmacy transaction records
- **LLM Config** — Configure the AI provider and model:
  - Provider dropdown: Anthropic (Claude), OpenAI (ChatGPT), 阿里云 (千问 Qwen)
  - Model presets auto-populated per provider
  - API Key, System Prompt, and Active toggle
  - Note: the selected provider/key must also be set as env vars in the llm-service (see `llm-service/README.md`)

## Language

Toggle EN / 中文 via the button in the top-right header. Language preference is persisted in `localStorage`.

## Requirements

Backend must be running on `http://localhost:8080`.
