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

CHAT_SYSTEM_PROMPT = """You are a helpful medical assistant for an insurance company health app called AIA Health.
You help users with medical questions, provide general health information, and guide them to appropriate care.

When responding:
- Be empathetic, clear, and professional
- Provide helpful general information but always recommend professional medical advice for serious symptoms
- If the user describes serious, complex, or persistent symptoms, suggest they consult a doctor (DOCTOR consultation)
- If the user has a minor/simple issue, suggest AI consultation first
- If the user asks about visiting a hospital or wants to see a doctor in person, suggest appointment booking
- Keep responses concise (2-4 sentences)
- Respond in the same language as the user's message

Doctor specialties available (by ID):
1: Cardiology (heart issues, chest pain, blood pressure)
2: General Practice (common illness, fever, general concerns)
3: Dermatology (skin issues, rashes, hair)
4: Neurology (headaches, dizziness, neurological symptoms)
5: Pediatrics (children's health)"""

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
    "description": "After responding to the user, determine what action to suggest",
    "input_schema": {
        "type": "object",
        "properties": {
            "responseText": {
                "type": "string",
                "description": "The response to show the user",
            },
            "suggestConsultation": {
                "type": "boolean",
                "description": "Whether to suggest a medical consultation",
            },
            "consultationType": {
                "type": "string",
                "enum": ["AI", "DOCTOR"],
                "description": "Type of consultation to suggest (omit if no consultation)",
            },
            "suggestAppointment": {
                "type": "boolean",
                "description": "Whether to suggest booking a hospital appointment",
            },
            "recommendedDoctorIds": {
                "type": "array",
                "items": {"type": "integer"},
                "description": "List of doctor IDs to recommend (1-5)",
            },
        },
        "required": ["responseText", "suggestConsultation", "suggestAppointment"],
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
    messages = [{"role": h.role, "content": h.content} for h in history[-10:]]
    messages.append({"role": "user", "content": message})

    inp = _call_tool(messages, CHAT_SYSTEM_PROMPT, [_CHAT_TOOL])
    if inp is None:
        return None

    return ChatResponse(
        content=inp.get("responseText", ""),
        suggestConsultation=inp.get("suggestConsultation", False),
        consultationType=inp.get("consultationType"),
        suggestAppointment=inp.get("suggestAppointment", False),
        recommendedDoctorIds=inp.get("recommendedDoctorIds", []),
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
