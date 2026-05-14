# HealthBot LLM Service

Python FastAPI service that handles AI medical conversations. Supports **Anthropic Claude**, **OpenAI (ChatGPT)**, and **Alibaba Cloud Qwen (千问)**.

## Setup

```bash
cd llm-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Configuration

Create a `.env` file. The `PROVIDER` variable selects the LLM backend; without an API key the service falls back to mock responses.

### Anthropic Claude (default)

```env
PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...
MODEL=claude-sonnet-4-6   # or claude-opus-4-7, claude-haiku-4-5-20251001
PORT=8000
```

### OpenAI ChatGPT

```env
PROVIDER=openai
OPENAI_API_KEY=sk-...
MODEL=gpt-4o              # or gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo
PORT=8000
```

### Alibaba Cloud Qwen (千问)

```env
PROVIDER=qwen
QWEN_API_KEY=sk-...       # DashScope API key from console.aliyun.com
MODEL=qwen-plus           # or qwen-max, qwen-turbo
PORT=8000
```

> Qwen uses the DashScope OpenAI-compatible endpoint (`dashscope.aliyuncs.com/compatible-mode/v1`) so no extra config is needed beyond the key.

## Default Models per Provider

| Provider | Default model |
|----------|--------------|
| `anthropic` | `claude-sonnet-4-6` |
| `openai` | `gpt-4o` |
| `qwen` | `qwen-plus` |

## Run

```bash
uvicorn main:app --reload --port 8000
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/health` | Returns `{"status":"ok","provider":"...","model":"..."}` |
| `POST` | `/chat` | Medical chatbot conversation |
| `POST` | `/ai-consultation` | Structured AI doctor consultation |
| `POST` | `/generate-prescription` | Generate prescription from consultation summary |

## Mock Mode

Without a valid API key the service returns realistic mock responses, so the rest of the stack can run without LLM credentials.
