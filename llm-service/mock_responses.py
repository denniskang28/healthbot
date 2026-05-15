from models import ChatResponse, AiConsultationResponse, Medicine

# ── Predefined demo scripts ───────────────────────────────────────────────────
# Each script has fixed intermediate responses and a deterministic conclusion.
# Designed to match the three README demo cases exactly.

MOCK_SCRIPTS = {
    "MEDICATION": {
        "turns_en": [
            "I'm sorry to hear that. How long have you had the sore throat and congestion, and do you have a fever?",
            "Thanks for sharing. Any body aches, cough, or other symptoms besides the fever and congestion?",
        ],
        "turns_zh": [
            "您好，请问咽痛和鼻塞持续多久了？有没有发烧？",
            "明白了。除了发烧和鼻塞，有没有身体酸痛、咳嗽或其他症状？",
        ],
        "conclusion_en": (
            "Based on your symptoms, you likely have a mild upper respiratory infection (common cold). "
            "The combination of sore throat, nasal congestion, and low-grade fever is typical of a viral infection. "
            "Rest, hydration, and the medications below should have you feeling better within 7–10 days."
        ),
        "conclusion_zh": (
            "根据您的症状，您可能患有轻度上呼吸道感染（普通感冒）。"
            "咽痛、鼻塞和低烧是病毒性感染的典型表现。"
            "多休息、多饮水，按照下方处方用药，通常7-10天内可康复。"
        ),
        "recommendation": "MEDICATION",
        "prescription_en": [
            Medicine(name="Ibuprofen",       dosage="400mg", frequency="every 8 hours as needed", days=5),
            Medicine(name="Pseudoephedrine", dosage="60mg",  frequency="every 6 hours",            days=5),
            Medicine(name="Cetirizine",      dosage="10mg",  frequency="once daily at bedtime",    days=7),
        ],
        "prescription_zh": [
            Medicine(name="布洛芬",   dosage="400毫克", frequency="每8小时一次（按需）", days=5),
            Medicine(name="伪麻黄碱", dosage="60毫克",  frequency="每6小时一次",         days=5),
            Medicine(name="西替利嗪", dosage="10毫克",  frequency="每晚一次",             days=7),
        ],
        "recommendedDoctorIds": [],
    },
    "ONLINE_CONSULTATION": {
        "turns_en": [
            "I understand. How long does each headache episode typically last, and how severe is the pain (1–10)?",
            "Do you notice any nausea, sensitivity to light or sound, or visual disturbances during the headaches?",
        ],
        "turns_zh": [
            "明白。每次头痛持续多久？疼痛程度如何（1-10分）？",
            "头痛时有没有伴随恶心、畏光、畏声或视觉异常？",
        ],
        "conclusion_en": (
            "Your recurring temporal headaches lasting 2–3 hours, with nausea and light sensitivity, "
            "are consistent with migraines. "
            "This is a neurological condition that benefits from specialist evaluation and a tailored treatment plan. "
            "I recommend speaking with a neurologist online."
        ),
        "conclusion_zh": (
            "您反复出现的颞部头痛（每次2-3小时）伴有恶心和畏光，与偏头痛的症状高度吻合。"
            "偏头痛需要神经科专家评估并制定个性化治疗方案。"
            "建议在线咨询神经科医生。"
        ),
        "recommendation": "ONLINE_CONSULTATION",
        "prescription_en": None,
        "prescription_zh": None,
        "recommendedDoctorIds": [4],  # Neurology
    },
    "OFFLINE_APPOINTMENT": {
        "turns_en": [
            "That's concerning. Are you also experiencing unusual fatigue, night sweats, or changes in appetite?",
            "Have you noticed heart palpitations, increased thirst or urination, or any other new symptoms?",
        ],
        "turns_zh": [
            "这需要引起重视。您是否同时伴有异常疲劳、盗汗或食欲变化？",
            "有没有出现心悸、多饮多尿或其他新症状？",
        ],
        "conclusion_en": (
            "Unexplained weight loss combined with night sweats, fatigue, and a family history of diabetes "
            "raises the possibility of a metabolic or endocrine condition. "
            "This requires blood tests and a physical examination — it cannot be assessed remotely. "
            "Please book an in-person appointment as soon as possible."
        ),
        "conclusion_zh": (
            "不明原因的体重下降、盗汗、疲劳，加上糖尿病家族史，提示可能存在代谢或内分泌问题。"
            "这类情况需要血液检查和体格检查，无法通过远程诊断。"
            "请尽快预约线下就医。"
        ),
        "recommendation": "OFFLINE_APPOINTMENT",
        "prescription_en": None,
        "prescription_zh": None,
        "recommendedDoctorIds": [2],  # General Practice
    },
}

# ── AI Consultation fallbacks ─────────────────────────────────────────────────

MOCK_AI_CONSULTATION_EN = [
    "Hello, I'm your AI doctor. Please describe your main symptoms.",
    "I see. How long have you been experiencing these symptoms? Any fever or chills?",
    "Have you taken any medications recently? Do you have any known allergies?",
]

MOCK_AI_CONSULTATION_ZH = [
    "您好，我是您的AI医生。请描述您的主要症状。",
    "我明白了。您出现这些症状多长时间了？有发烧或发冷吗？",
    "您最近服用过任何药物吗？您有任何已知的过敏症吗？",
]

MOCK_PRESCRIPTION = [
    Medicine(name="Amoxicillin", dosage="500mg", frequency="3 times daily", days=7),
    Medicine(name="Ibuprofen",   dosage="400mg", frequency="twice daily",   days=5),
    Medicine(name="Cetirizine",  dosage="10mg",  frequency="once daily",    days=14),
]

MOCK_PRESCRIPTION_ZH = [
    Medicine(name="阿莫西林", dosage="500毫克", frequency="每日三次", days=7),
    Medicine(name="布洛芬",   dosage="400毫克", frequency="每日两次", days=5),
    Medicine(name="西替利嗪", dosage="10毫克",  frequency="每日一次", days=14),
]


# ── Public helpers ────────────────────────────────────────────────────────────

def get_mock_chat_response(message_count: int, language: str, script: str = "MEDICATION") -> ChatResponse:
    """
    message_count: 0-indexed counter from main.py (0 = first message).
    Triggers conclusion on the 3rd message (count >= 2), matching CONCLUDE_AFTER_EXCHANGES=3.
    """
    s = MOCK_SCRIPTS.get(script, MOCK_SCRIPTS["MEDICATION"])
    is_complete = message_count >= 2

    if is_complete:
        conclusion = s["conclusion_zh"] if language == "ZH" else s["conclusion_en"]
        prescription_list = s["prescription_zh"] if language == "ZH" else s["prescription_en"]
        content = (
            "根据您描述的症状，我已经有了初步判断。"
            if language == "ZH"
            else "Based on your symptoms, I have an initial assessment ready for you."
        )
        return ChatResponse(
            content=content,
            isComplete=True,
            conclusion=conclusion,
            recommendation=s["recommendation"],
            prescription=prescription_list,
            recommendedDoctorIds=s.get("recommendedDoctorIds", []),
        )

    turns = s["turns_zh"] if language == "ZH" else s["turns_en"]
    idx = min(message_count, len(turns) - 1)
    return ChatResponse(content=turns[idx])


def get_mock_ai_consultation_response(history_length: int, language: str) -> AiConsultationResponse:
    questions = MOCK_AI_CONSULTATION_ZH if language == "ZH" else MOCK_AI_CONSULTATION_EN
    exchange_count = history_length // 2

    if exchange_count >= 3:
        prescription = MOCK_PRESCRIPTION_ZH if language == "ZH" else MOCK_PRESCRIPTION
        content = (
            "根据您的症状，我已为您开具以下处方。请按时服药，如症状加重请及时就医。"
            if language == "ZH"
            else "Based on your symptoms, I've prepared the following prescription. Take medications as directed and seek care if symptoms worsen."
        )
        return AiConsultationResponse(content=content, isComplete=True, prescription=prescription)

    idx = min(exchange_count, len(questions) - 1)
    return AiConsultationResponse(content=questions[idx], isComplete=False, prescription=None)
