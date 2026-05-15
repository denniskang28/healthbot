import os
import json
from typing import List, Optional, Dict, Any

from models import ChatHistoryItem, ChatResponse, AiConsultationResponse, Medicine, PrescriptionResponse

PROVIDER = os.getenv("PROVIDER", "anthropic").lower()
MODEL = os.getenv("MODEL", "")

_KEY_ENVS = {
    "anthropic": "ANTHROPIC_API_KEY",
    "openai":    "OPENAI_API_KEY",
    "qwen":      "QWEN_API_KEY",
}
_DEFAULT_MODELS = {
    "anthropic": "claude-sonnet-4-6",
    "openai":    "gpt-4o",
    "qwen":      "qwen-plus",
}

API_KEY = os.getenv(_KEY_ENVS.get(PROVIDER, ""), "")
if not MODEL:
    MODEL = _DEFAULT_MODELS.get(PROVIDER, "")

_client = None


def get_client():
    global _client
    if _client is not None:
        return _client
    if not API_KEY:
        return None
    if PROVIDER == "anthropic":
        import anthropic
        _client = anthropic.Anthropic(api_key=API_KEY)
    elif PROVIDER in ("openai", "qwen"):
        from openai import OpenAI
        kwargs: Dict[str, Any] = {"api_key": API_KEY}
        if PROVIDER == "qwen":
            kwargs["base_url"] = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        _client = OpenAI(**kwargs)
    return _client


def _to_openai_tools(tools: List[Dict]) -> List[Dict]:
    """Convert Anthropic tool format to OpenAI function-calling format."""
    return [
        {
            "type": "function",
            "function": {
                "name": t["name"],
                "description": t.get("description", ""),
                "parameters": t["input_schema"],
            },
        }
        for t in tools
    ]


def _call_tool(messages: List[Dict], system: Optional[str], tools: List[Dict], max_tokens: int = 1024) -> Optional[Dict]:
    """
    Call the configured LLM with tool-forcing and return the first tool call's input dict.
    `tools` must be in Anthropic schema format; OpenAI/Qwen are converted internally.
    Returns None if the client is unavailable or no tool call was made.
    """
    cl = get_client()
    if not cl:
        return None

    if PROVIDER == "anthropic":
        kwargs: Dict[str, Any] = dict(
            model=MODEL,
            max_tokens=max_tokens,
            tools=tools,
            tool_choice={"type": "any"},
            messages=messages,
        )
        if system:
            kwargs["system"] = system
        response = cl.messages.create(**kwargs)
        for block in response.content:
            if block.type == "tool_use":
                return block.input
        return None

    elif PROVIDER in ("openai", "qwen"):
        oai_messages = []
        if system:
            oai_messages.append({"role": "system", "content": system})
        oai_messages.extend(messages)
        response = cl.chat.completions.create(
            model=MODEL,
            max_tokens=max_tokens,
            messages=oai_messages,
            tools=_to_openai_tools(tools),
            tool_choice="required",
        )
        msg = response.choices[0].message
        if msg.tool_calls:
            return json.loads(msg.tool_calls[0].function.arguments)
        return None

    return None


# ── System prompts ────────────────────────────────────────────────────────────

CHAT_SYSTEM_PROMPT = """You are a professional medical assistant for AIA Health insurance app.
Your role is to gather information about the user's symptoms through natural conversation and then provide a preliminary assessment.

## Conversation process
- Turn 1-3: Ask targeted questions to understand symptoms (type, severity, duration, associated symptoms).
- Turn 4+: Once you have enough information, provide a preliminary assessment.
  Set isComplete=true, write a clear conclusion, and choose a recommendation.

## Recommendation types (choose ONE when isComplete=true)
- ONLINE_CONSULTATION: Symptoms are significant and need a real doctor soon, but not an emergency. User can speak to an online specialist.
- OFFLINE_APPOINTMENT: Symptoms suggest a condition that requires in-person examination, lab work, or physical assessment.
- MEDICATION: Symptoms are mild, clear-cut (e.g., common cold, mild allergy, minor pain), and safe to treat with OTC or simple prescription drugs. Provide a prescription.

## Rules
- Keep each response concise (2-4 sentences).
- Respond in the same language as the user.
- Never set isComplete=true on the first 2 exchanges (history must have ≥4 messages).
- conclusion must be a professional summary of your assessment (2-3 sentences), written for the patient.
- If recommendation=MEDICATION, always provide a prescription array.
- For serious/emergency symptoms (chest pain, stroke signs, difficulty breathing) recommend OFFLINE_APPOINTMENT and urge immediate care.

Doctor specialties available (by ID):
1: Cardiology  2: General Practice  3: Dermatology  4: Neurology  5: Pediatrics"""

AI_CONSULTATION_SYSTEM = """You are an AI doctor conducting a structured medical consultation.
Your goal is to gather sufficient information about the patient's condition through targeted questions, then provide a diagnosis and prescription.

Process:
1. Ask about main symptoms (what, when, severity)
2. Ask about duration and any changes
3. Ask about relevant medical history or medications
4. After gathering 3+ rounds of information, provide diagnosis and prescription

When you have enough information (history has 6+ messages), set isComplete to true and provide a prescription.
Always add a disclaimer that this is AI-generated and recommend in-person follow-up for serious conditions.
Respond in the same language as the patient."""


# ── Tool schemas (Anthropic format) ──────────────────────────────────────────

_CHAT_TOOL = {
    "name": "determine_action",
    "description": "Respond to the user and, once enough information is gathered, provide a preliminary assessment",
    "input_schema": {
        "type": "object",
        "properties": {
            "responseText": {
                "type": "string",
                "description": "The conversational response to show the user",
            },
            "isComplete": {
                "type": "boolean",
                "description": "True only when you have gathered enough symptom information to make a preliminary assessment (requires ≥4 prior messages in history)",
            },
            "conclusion": {
                "type": "string",
                "description": "Preliminary assessment summary for the patient (2-3 sentences). Required when isComplete=true.",
            },
            "recommendation": {
                "type": "string",
                "enum": ["ONLINE_CONSULTATION", "OFFLINE_APPOINTMENT", "MEDICATION"],
                "description": "The recommended next step. Required when isComplete=true.",
            },
            "prescription": {
                "type": "array",
                "description": "List of medicines. Required when recommendation=MEDICATION.",
                "items": {
                    "type": "object",
                    "properties": {
                        "name":      {"type": "string"},
                        "dosage":    {"type": "string"},
                        "frequency": {"type": "string"},
                        "days":      {"type": "integer"},
                    },
                    "required": ["name", "dosage", "frequency", "days"],
                },
            },
            "recommendedDoctorIds": {
                "type": "array",
                "items": {"type": "integer"},
                "description": "Relevant doctor IDs (1-5) when recommendation is ONLINE_CONSULTATION or OFFLINE_APPOINTMENT",
            },
        },
        "required": ["responseText", "isComplete"],
    },
}

_CONSULTATION_TOOL = {
    "name": "consultation_response",
    "description": "Respond to the patient during the consultation",
    "input_schema": {
        "type": "object",
        "properties": {
            "content": {
                "type": "string",
                "description": "The response to show the patient",
            },
            "isComplete": {
                "type": "boolean",
                "description": "Whether the consultation is complete and prescription is ready",
            },
            "prescription": {
                "type": "array",
                "description": "List of prescribed medicines (only when isComplete is true)",
                "items": {
                    "type": "object",
                    "properties": {
                        "name":      {"type": "string"},
                        "dosage":    {"type": "string"},
                        "frequency": {"type": "string"},
                        "days":      {"type": "integer"},
                    },
                    "required": ["name", "dosage", "frequency", "days"],
                },
            },
        },
        "required": ["content", "isComplete"],
    },
}

_PRESCRIPTION_TOOL = {
    "name": "generate_prescription",
    "description": "Generate a medical prescription",
    "input_schema": {
        "type": "object",
        "properties": {
            "medicines": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "name":      {"type": "string"},
                        "dosage":    {"type": "string"},
                        "frequency": {"type": "string"},
                        "days":      {"type": "integer"},
                    },
                    "required": ["name", "dosage", "frequency", "days"],
                },
            }
        },
        "required": ["medicines"],
    },
}


# ── Public API ────────────────────────────────────────────────────────────────

async def chat(message: str, history: List[ChatHistoryItem], language: str) -> Optional[ChatResponse]:
    messages = [{"role": h.role, "content": h.content} for h in history[-12:]]
    messages.append({"role": "user", "content": message})

    inp = _call_tool(messages, CHAT_SYSTEM_PROMPT, [_CHAT_TOOL], max_tokens=1500)
    if inp is None:
        return None

    is_complete = inp.get("isComplete", False)
    recommendation = inp.get("recommendation") if is_complete else None

    prescription = None
    if recommendation == "MEDICATION" and inp.get("prescription"):
        prescription = [Medicine(**m) for m in inp["prescription"]]

    return ChatResponse(
        content=inp.get("responseText", ""),
        suggestConsultation=False,
        consultationType=None,
        suggestAppointment=False,
        recommendedDoctorIds=inp.get("recommendedDoctorIds", []),
        isComplete=is_complete,
        conclusion=inp.get("conclusion") if is_complete else None,
        recommendation=recommendation,
        prescription=prescription,
    )


async def ai_consultation(message: str, history: List[ChatHistoryItem], language: str) -> Optional[AiConsultationResponse]:
    messages = [{"role": h.role, "content": h.content} for h in history[-20:]]
    messages.append({"role": "user", "content": message})

    is_complete = len(history) >= 6
    system = AI_CONSULTATION_SYSTEM
    if is_complete:
        system += "\n\nIMPORTANT: You now have enough information. Set isComplete=true and provide a prescription."

    inp = _call_tool(messages, system, [_CONSULTATION_TOOL])
    if inp is None:
        return None

    prescription = None
    if inp.get("prescription"):
        prescription = [Medicine(**m) for m in inp["prescription"]]

    return AiConsultationResponse(
        content=inp.get("content", ""),
        isComplete=inp.get("isComplete", False),
        prescription=prescription,
    )


async def generate_prescription(summary: str, language: str) -> Optional[PrescriptionResponse]:
    prompt = f"Based on this consultation summary, generate an appropriate prescription:\n{summary}"
    if language == "ZH":
        prompt += "\nRespond with medicine names in Chinese."

    inp = _call_tool(
        [{"role": "user", "content": prompt}],
        system=None,
        tools=[_PRESCRIPTION_TOOL],
        max_tokens=512,
    )
    if inp is None:
        return None

    medicines = [Medicine(**m) for m in inp.get("medicines", [])]
    return PrescriptionResponse(medicines=medicines)
