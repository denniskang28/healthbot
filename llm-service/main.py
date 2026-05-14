import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from models import (
    ChatRequest, ChatResponse,
    AiConsultationRequest, AiConsultationResponse,
    PrescriptionRequest, PrescriptionResponse
)
import llm_client
import mock_responses

load_dotenv()

app = FastAPI(title="HealthBot LLM Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_chat_counters: dict[int, int] = {}
_consultation_counters: dict[int, int] = {}


@app.get("/health")
async def health():
    return {"status": "ok", "provider": llm_client.PROVIDER, "model": llm_client.MODEL}


@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    result = await llm_client.chat(req.message, req.history, req.language)
    if result is not None:
        return result

    count = _chat_counters.get(req.userId, 0)
    _chat_counters[req.userId] = count + 1
    return mock_responses.get_mock_chat_response(count, req.language)


@app.post("/ai-consultation", response_model=AiConsultationResponse)
async def ai_consultation(req: AiConsultationRequest):
    result = await llm_client.ai_consultation(req.message, req.history, req.language)
    if result is not None:
        return result

    history_len = len(req.history)
    return mock_responses.get_mock_ai_consultation_response(history_len, req.language)


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
