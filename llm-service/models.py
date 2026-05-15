from pydantic import BaseModel
from typing import Optional, List


class ChatHistoryItem(BaseModel):
    role: str
    content: str


class Medicine(BaseModel):
    name: str
    dosage: str
    frequency: str
    days: int


class ChatRequest(BaseModel):
    userId: int
    message: str
    history: List[ChatHistoryItem] = []
    language: str = "EN"


class ChatResponse(BaseModel):
    content: str
    suggestConsultation: bool = False
    consultationType: Optional[str] = None
    suggestAppointment: bool = False
    recommendedDoctorIds: List[int] = []
    isComplete: bool = False
    conclusion: Optional[str] = None
    recommendation: Optional[str] = None   # ONLINE_CONSULTATION | OFFLINE_APPOINTMENT | MEDICATION
    prescription: Optional[List[Medicine]] = None


class AiConsultationRequest(BaseModel):
    userId: int
    consultationId: int
    message: str
    history: List[ChatHistoryItem] = []
    language: str = "EN"


class AiConsultationResponse(BaseModel):
    content: str
    isComplete: bool = False
    prescription: Optional[List[Medicine]] = None


class PrescriptionRequest(BaseModel):
    consultationSummary: str
    language: str = "EN"


class PrescriptionResponse(BaseModel):
    medicines: List[Medicine]


class LlmConfigRequest(BaseModel):
    provider: Optional[str] = None
    model: Optional[str] = None
    apiKey: Optional[str] = None
    mockMode: Optional[bool] = None
    mockScript: Optional[str] = None  # MEDICATION | ONLINE_CONSULTATION | OFFLINE_APPOINTMENT


class LlmConfigResponse(BaseModel):
    provider: str
    model: str
    apiKeyMasked: str
    mockMode: bool
    mockScript: str
