import os
from dotenv import load_dotenv
load_dotenv()  # must run before other imports so env vars are available to config_manager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from models import (
    ChatRequest, ChatResponse,
    AiConsultationRequest, AiConsultationResponse,
    PrescriptionRequest, PrescriptionResponse,
    LlmConfigRequest, LlmConfigResponse,
)
import config_manager
import llm_client
import mock_responses

app = FastAPI(title="HealthBot LLM Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_chat_counters: dict[int, int] = {}


@app.get("/health")
async def health():
    return {"status": "ok", "provider": llm_client.get_provider(), "model": llm_client.get_model()}


@app.get("/config", response_model=LlmConfigResponse)
async def get_config():
    return config_manager.safe_get()


@app.put("/config", response_model=LlmConfigResponse)
async def update_config(req: LlmConfigRequest):
    config_manager.update(req.model_dump(exclude_none=True))
    return config_manager.safe_get()


@app.delete("/chat-counter/{user_id}")
async def reset_chat_counter(user_id: int):
    _chat_counters.pop(user_id, None)
    return {"reset": True}


@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    cfg = config_manager.get()
    mock_script = cfg.get("mockScript", "MEDICATION")

    if cfg.get("mockMode"):
        count = _chat_counters.get(req.userId, 0)
        _chat_counters[req.userId] = count + 1
        return mock_responses.get_mock_chat_response(count, req.language, mock_script)

    result = await llm_client.chat(req.message, req.history, req.language)
    if result is not None:
        return result

    # LLM unavailable — fall back to mock
    count = _chat_counters.get(req.userId, 0)
    _chat_counters[req.userId] = count + 1
    return mock_responses.get_mock_chat_response(count, req.language, mock_script)


@app.post("/ai-consultation", response_model=AiConsultationResponse)
async def ai_consultation(req: AiConsultationRequest):
    result = await llm_client.ai_consultation(req.message, req.history, req.language)
    if result is not None:
        return result
    return mock_responses.get_mock_ai_consultation_response(len(req.history), req.language)


@app.post("/generate-prescription", response_model=PrescriptionResponse)
async def generate_prescription(req: PrescriptionRequest):
    result = await llm_client.generate_prescription(req.consultationSummary, req.language)
    if result is not None:
        return result
    medicines = mock_responses.MOCK_PRESCRIPTION_ZH if req.language == "ZH" else mock_responses.MOCK_PRESCRIPTION
    return PrescriptionResponse(medicines=medicines)


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
