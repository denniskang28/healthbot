import json
from typing import List, Optional, Dict, Any

import config_manager
from models import ChatHistoryItem, ChatResponse, AiConsultationResponse, Medicine, PrescriptionResponse

_client = None


def reload():
    """Called by config_manager after config changes — forces client re-init."""
    global _client
    _client = None


def get_provider() -> str:
    return config_manager.get()["provider"]


def get_model() -> str:
    return config_manager.get()["model"]


def get_client():
    global _client
    if _client is not None:
        return _client
    cfg = config_manager.get()
    api_key = cfg.get("apiKey", "")
    provider = cfg["provider"]
    if not api_key:
        return None
    if provider == "anthropic":
        import anthropic
        _client = anthropic.Anthropic(api_key=api_key)
    elif provider in ("openai", "qwen", "deepseek"):
        from openai import OpenAI
        kwargs: Dict[str, Any] = {"api_key": api_key}
        if provider == "qwen":
            kwargs["base_url"] = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        elif provider == "deepseek":
            kwargs["base_url"] = "https://api.deepseek.com"
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

    provider = get_provider()
    model = get_model()

    if provider == "anthropic":
        kwargs: Dict[str, Any] = dict(
            model=model,
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

    elif provider in ("openai", "qwen", "deepseek"):
        oai_messages = []
        if system:
            oai_messages.append({"role": "system", "content": system})
        oai_messages.extend(messages)
        response = cl.chat.completions.create(
            model=model,
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
Gather information about the user's symptoms through natural conversation.
Ask one focused follow-up question per turn (type, severity, duration, associated symptoms, medications).
Keep responses to 2-3 sentences. Respond in the same language as the user."""

CHAT_CONCLUDE_SYSTEM = """You are a medical assistant for AIA Health.
The user has described their symptoms over several messages. Write one sentence telling them
you have a preliminary assessment ready. Respond in the same language as the user."""

CLASSIFY_SYSTEM = """You are a medical triage specialist. Based on the symptom conversation above,
choose exactly one recommendation category using these explicit rules:

CONTINUE_CHAT — choose this FIRST if the latest user message is not about symptoms:
  • Expressions of thanks, acknowledgement, or closure ("thank you", "ok", "got it", "bye")
  • Off-topic or casual conversation unrelated to health
  • Simple confirmations after a recommendation has already been given
  → When chosen, no conclusion or prescription is needed.

MEDICATION — choose when ALL are true:
  • Acute onset (< 3 days), mild severity
  • Symptoms fit a clear common condition: cold, mild fever (<38.5°C), sore throat,
    nasal congestion, mild allergy, minor muscle/joint pain, mild gastroenteritis
  • No underlying chronic conditions or red flags mentioned
  • Safe to treat with standard OTC or first-line prescription drugs
  → If chosen, provide a specific prescription array.

OFFLINE_APPOINTMENT — choose when ANY is true:
  • Unexplained weight loss or gain
  • Night sweats or persistent fatigue without clear cause
  • Suspected metabolic or hormonal issue (diabetes, thyroid)
  • Chest pain, palpitations, or shortness of breath
  • Neurological symptoms (vision changes, numbness, coordination issues)
  • Symptoms that require physical exam, imaging, or blood/lab tests

ONLINE_CONSULTATION — choose for everything else:
  • Recurring symptoms not clearly resolved by OTC drugs
  • Moderate severity where specialist advice is needed
  • Conditions manageable remotely without immediate tests
  → Examples: recurring migraines, skin conditions, persistent cough >2 weeks, anxiety

Decision rules when unsure:
  ONLINE_CONSULTATION vs OFFLINE_APPOINTMENT → choose OFFLINE_APPOINTMENT
  MEDICATION vs ONLINE_CONSULTATION → choose ONLINE_CONSULTATION

Medical specialty — always choose the most relevant one:
  CARDIOLOGY       — chest pain, palpitations, shortness of breath, arrhythmia
  NEUROLOGY        — recurring headaches, migraines, numbness, dizziness, seizures
  DERMATOLOGY      — skin rashes, itching, eczema, acne, suspicious moles
  ORTHOPEDICS      — joint pain, back pain, bone/muscle injuries, mobility problems
  GASTROENTEROLOGY — stomach pain, nausea, vomiting, diarrhea, bloating
  RESPIRATORY      — persistent cough, asthma, wheezing, breathing difficulty
  ENDOCRINOLOGY    — unexplained weight changes, diabetes symptoms, thyroid issues
  PSYCHIATRY       — anxiety, depression, insomnia, mood disorders
  PEDIATRICS       — condition in a child or infant
  GENERAL          — mild infections, common cold, routine concerns, non-specific symptoms

Write the conclusion in the same language the user used in the conversation."""

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

_CHAT_RESPONSE_TOOL = {
    "name": "chat_response",
    "description": "Generate a conversational response to the user's health message",
    "input_schema": {
        "type": "object",
        "properties": {
            "responseText": {
                "type": "string",
                "description": "The response to show the user (2-3 sentences)",
            },
        },
        "required": ["responseText"],
    },
}

_CLASSIFY_TOOL = {
    "name": "classify_recommendation",
    "description": "Classify the patient's symptoms into a medical pathway and provide an assessment",
    "input_schema": {
        "type": "object",
        "properties": {
            "conclusion": {
                "type": "string",
                "description": "2-3 sentence preliminary assessment written for the patient",
            },
            "recommendation": {
                "type": "string",
                "enum": ["CONTINUE_CHAT", "MEDICATION", "ONLINE_CONSULTATION", "OFFLINE_APPOINTMENT"],
            },
            "prescription": {
                "type": "array",
                "description": "Required when recommendation=MEDICATION",
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
            "specialty": {
                "type": "string",
                "enum": ["GENERAL", "CARDIOLOGY", "NEUROLOGY", "DERMATOLOGY", "ORTHOPEDICS",
                         "GASTROENTEROLOGY", "RESPIRATORY", "ENDOCRINOLOGY", "PSYCHIATRY", "PEDIATRICS"],
                "description": "Primary medical specialty for this patient's condition",
            },
            "recommendedDoctorIds": {
                "type": "array",
                "items": {"type": "integer"},
                "description": "Doctor IDs (1-5) relevant to the condition",
            },
        },
        "required": ["conclusion", "recommendation", "specialty"],
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

CONCLUDE_AFTER_EXCHANGES = 3  # trigger classification after this many user messages


async def chat(message: str, history: List[ChatHistoryItem], language: str) -> Optional[ChatResponse]:
    messages = [{"role": h.role, "content": h.content} for h in history[-12:]]
    messages.append({"role": "user", "content": message})

    # Code controls timing — count how many user turns have happened including this one
    user_turns = sum(1 for h in history if h.role == "user") + 1
    force_complete = user_turns >= CONCLUDE_AFTER_EXCHANGES

    if not force_complete:
        # Normal chat: just generate a response
        inp = _call_tool(messages, CHAT_SYSTEM_PROMPT, [_CHAT_RESPONSE_TOOL], max_tokens=512)
        if inp is None:
            return None
        return ChatResponse(content=inp.get("responseText", ""))

    # Step 1: generate a "I have an assessment" response
    resp_inp = _call_tool(messages, CHAT_CONCLUDE_SYSTEM, [_CHAT_RESPONSE_TOOL], max_tokens=256)
    response_text = resp_inp.get("responseText", "") if resp_inp else ""

    # Step 2: separate focused classification call
    classify_inp = _call_tool(messages, CLASSIFY_SYSTEM, [_CLASSIFY_TOOL], max_tokens=1024)
    if classify_inp is None:
        return None

    recommendation = classify_inp.get("recommendation")

    # Non-medical follow-up (e.g. "thank you") — just continue the conversation
    if recommendation == "CONTINUE_CHAT":
        return ChatResponse(content=response_text)

    prescription = None
    if recommendation == "MEDICATION" and classify_inp.get("prescription"):
        prescription = [Medicine(**m) for m in classify_inp["prescription"]]

    return ChatResponse(
        content=response_text,
        isComplete=True,
        conclusion=classify_inp.get("conclusion"),
        recommendation=recommendation,
        specialty=classify_inp.get("specialty"),
        prescription=prescription,
        recommendedDoctorIds=classify_inp.get("recommendedDoctorIds", []),
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
