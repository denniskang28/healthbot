package com.healthbot.app.api

import com.healthbot.app.api.models.*
import retrofit2.http.*

interface HealthBotApi {

    @POST("api/chat/{userId}/message")
    suspend fun sendMessage(
        @Path("userId") userId: Long,
        @Body request: MessageRequest
    ): ChatResponseDto

    @GET("api/chat/{userId}/history")
    suspend fun getChatHistory(@Path("userId") userId: Long): List<ChatMessageDto>

    @GET("api/doctors")
    suspend fun getDoctors(): List<Doctor>

    @GET("api/doctors/{id}")
    suspend fun getDoctor(@Path("id") id: Long): Doctor

    @POST("api/consultations")
    suspend fun createConsultation(@Body request: ConsultationRequest): Consultation

    @PUT("api/consultations/{id}/complete")
    suspend fun completeConsultation(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Consultation

    @POST("api/ai-consultation/{userId}/message")
    suspend fun sendAiConsultationMessage(
        @Path("userId") userId: Long,
        @Body request: AiConsultationRequest
    ): AiConsultationResponse

    @POST("api/prescriptions")
    suspend fun createPrescription(@Body request: PrescriptionRequest): Prescription

    @GET("api/prescriptions/{id}")
    suspend fun getPrescription(@Path("id") id: Long): Prescription

    @POST("api/purchases")
    suspend fun createPurchase(@Body request: PurchaseRequest): Purchase

    @PUT("api/purchases/{id}/complete")
    suspend fun completePurchase(@Path("id") id: Long): Purchase

    @POST("api/appointments")
    suspend fun createAppointment(@Body request: AppointmentRequest): Appointment
}
