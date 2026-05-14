from models import ChatResponse, AiConsultationResponse, Medicine

MOCK_CHAT_RESPONSES_EN = [
    "I understand you're experiencing some health concerns. Can you tell me more about your symptoms?",
    "Based on what you've described, this could be related to several conditions. It would be helpful to know how long you've been experiencing these symptoms.",
    "Thank you for sharing that information. Given your symptoms, I'd recommend consulting with a healthcare professional for a proper diagnosis. Would you like to speak with one of our doctors online, or would you prefer an AI consultation?",
]

MOCK_CHAT_RESPONSES_ZH = [
    "我理解您有一些健康方面的担忧。能告诉我更多关于您的症状吗？",
    "根据您的描述，这可能与几种情况有关。了解您出现这些症状多长时间会有所帮助。",
    "感谢您分享这些信息。根据您的症状，我建议您咨询医疗专业人员进行适当诊断。您想在线与我们的医生交流，还是更喜欢AI问诊？",
]

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
    Medicine(name="Ibuprofen", dosage="400mg", frequency="twice daily", days=5),
    Medicine(name="Cetirizine", dosage="10mg", frequency="once daily", days=14),
]

MOCK_PRESCRIPTION_ZH = [
    Medicine(name="阿莫西林", dosage="500毫克", frequency="每日三次", days=7),
    Medicine(name="布洛芬", dosage="400毫克", frequency="每日两次", days=5),
    Medicine(name="西替利嗪", dosage="10毫克", frequency="每日一次", days=14),
]


def get_mock_chat_response(message_count: int, language: str) -> ChatResponse:
    responses = MOCK_CHAT_RESPONSES_ZH if language == "ZH" else MOCK_CHAT_RESPONSES_EN
    idx = min(message_count, len(responses) - 1)
    suggest = message_count >= 2

    if suggest and language == "ZH":
        content = responses[idx] + "\n\n建议：您可以选择AI问诊或在线医生问诊。"
    elif suggest:
        content = responses[idx] + "\n\nI recommend either an AI consultation or speaking with one of our doctors online."
    else:
        content = responses[idx]

    return ChatResponse(
        content=content,
        suggestConsultation=suggest,
        consultationType="DOCTOR" if suggest else None,
        suggestAppointment=False,
        recommendedDoctorIds=[2, 1] if suggest else [],
    )


def get_mock_ai_consultation_response(history_length: int, language: str) -> AiConsultationResponse:
    questions = MOCK_AI_CONSULTATION_ZH if language == "ZH" else MOCK_AI_CONSULTATION_EN
    exchange_count = history_length // 2

    if exchange_count >= 3:
        prescription = MOCK_PRESCRIPTION_ZH if language == "ZH" else MOCK_PRESCRIPTION
        if language == "ZH":
            content = "根据您的症状，我已为您开具以下处方。请按时服药，如症状加重请及时就医。"
        else:
            content = "Based on your symptoms, I've prepared the following prescription. Please take medications as directed and seek immediate care if symptoms worsen."
        return AiConsultationResponse(content=content, isComplete=True, prescription=prescription)

    idx = min(exchange_count, len(questions) - 1)
    return AiConsultationResponse(content=questions[idx], isComplete=False, prescription=None)
