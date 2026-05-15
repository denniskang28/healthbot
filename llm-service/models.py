from pydantic import BaseModel
from typing import Optional, List


class ChatHistoryItem(BaseModel):
    role: str
    content: str


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


class Medicine(BaseModel):
    name: str
    dosage: str
    frequency: str
    days: int


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
