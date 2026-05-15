package com.healthbot.app.api.models

data class MessageRequest(val content: String)

data class ChatMessageDto(
    val id: Long,
    val userId: Long,
    val role: String,
    val content: String,
    val timestamp: String
)

data class ActionsDto(
    val suggestConsultation: Boolean = false,
    val consultationType: String? = null,
    val suggestAppointment: Boolean = false,
    val recommendedDoctorIds: List<Long> = emptyList(),
    val isComplete: Boolean = false,
    val conclusion: String? = null,
    val recommendation: String? = null,  // ONLINE_CONSULTATION | OFFLINE_APPOINTMENT | MEDICATION
    val prescription: List<Medicine>? = null
)

data class ChatResponseDto(
    val userMessage: ChatMessageDto,
    val assistantMessage: ChatMessageDto,
    val actions: ActionsDto
)

data class Doctor(
    val id: Long,
    val name: String,
    val specialty: String,
    val bio: String,
    val rating: Double,
    val available: Boolean,
    val avatarInitials: String
)

data class ConsultationRequest(
    val userId: Long,
    val doctorId: Long?,
    val type: String
)

data class Consultation(
    val id: Long,
    val userId: Long,
    val doctorId: Long?,
    val type: String,
    val status: String,
    val startTime: String,
    val endTime: String?,
    val notes: String?
)

data class Medicine(
    val name: String,
    val dosage: String,
    val frequency: String,
    val days: Int
)

data class PrescriptionRequest(
    val consultationId: Long,
    val medicines: List<Medicine>
)

data class Prescription(
    val id: Long,
    val consultationId: Long,
    val medicines: List<Medicine>,
    val createdAt: String
)

data class AiConsultationRequest(
    val consultationId: Long,
    val content: String
)

data class AiConsultationResponse(
    val message: ChatMessageDto,
    val isComplete: Boolean,
    val prescription: List<Medicine>?
)

data class PurchaseRequest(
    val prescriptionId: Long,
    val userId: Long
)

data class Purchase(
    val id: Long,
    val prescriptionId: Long,
    val userId: Long,
    val status: String,
    val totalAmount: Double,
    val purchasedAt: String?,
    val createdAt: String
)

data class AppointmentRequest(
    val userId: Long,
    val doctorId: Long,
    val scheduledTime: String,
    val hospitalName: String
)

data class Appointment(
    val id: Long,
    val userId: Long,
    val doctorId: Long,
    val doctorName: String,
    val scheduledTime: String,
    val status: String,
    val hospitalName: String,
    val createdAt: String
)
