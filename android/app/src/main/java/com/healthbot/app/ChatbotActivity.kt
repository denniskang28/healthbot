package com.healthbot.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.healthbot.app.api.RetrofitClient
import com.healthbot.app.api.models.*
import com.healthbot.app.databinding.ActivityChatbotBinding
import com.healthbot.app.databinding.BottomSheetConsultBinding
import com.healthbot.app.utils.LocaleHelper
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private val messages = mutableListOf<ChatItem>()
    private lateinit var adapter: ChatAdapter
    private val userId = 1L
    private var currentConsultationId: Long? = null

    data class ChatItem(val role: String, val content: String)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.health_assistant)

        adapter = ChatAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter

        // Check for result message from other activities
        val resultMessage = intent.getStringExtra("result_message")
        val consultationId = intent.getLongExtra("consultation_id", -1L)
        if (consultationId > 0) currentConsultationId = consultationId

        // Add welcome message
        if (messages.isEmpty()) {
            addMessage("ASSISTANT", getString(R.string.welcome_message))
        }

        if (resultMessage != null) {
            addMessage("ASSISTANT", resultMessage)
        }

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, _, _ -> sendMessage(); true }

        binding.btnLanguage.setOnClickListener {
            val newLang = if (LocaleHelper.getLanguage(this) == "en") "zh" else "en"
            LocaleHelper.setLocale(this, newLang)
            recreate()
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        binding.etMessage.setText("")
        addMessage("USER", text)
        showTyping(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.sendMessage(userId, MessageRequest(text))
                showTyping(false)
                addMessage("ASSISTANT", response.assistantMessage.content)
                handleActions(response.actions)
            } catch (e: Exception) {
                showTyping(false)
                addMessage("ASSISTANT", "Sorry, I'm having trouble connecting. Please try again.")
            }
        }
    }

    private fun handleActions(actions: ActionsDto) {
        val hasConclusion = actions.isComplete && actions.conclusion != null
        val hasLegacySuggestion = !hasConclusion && (actions.suggestConsultation || actions.suggestAppointment)
        if (!hasConclusion && !hasLegacySuggestion) return

        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetConsultBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        if (hasConclusion) {
            showConclusionSheet(sheetBinding, dialog, actions)
        } else {
            showLegacySuggestionSheet(sheetBinding, dialog, actions)
        }

        sheetBinding.btnLater.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showConclusionSheet(sheetBinding: BottomSheetConsultBinding, dialog: BottomSheetDialog, actions: ActionsDto) {
        sheetBinding.layoutConclusion.visibility = View.VISIBLE
        sheetBinding.tvAssessmentLabel.text = getString(R.string.assessment_label)
        sheetBinding.tvConclusion.text = actions.conclusion

        when (actions.recommendation) {
            "ONLINE_CONSULTATION" -> {
                sheetBinding.btnOnlineConsult.visibility = View.VISIBLE
                sheetBinding.btnOnlineConsult.text = getString(R.string.online_expert_consult)
                sheetBinding.btnOnlineConsult.setOnClickListener {
                    dialog.dismiss()
                    startDoctorConsultation(actions.recommendedDoctorIds)
                }
            }
            "OFFLINE_APPOINTMENT" -> {
                sheetBinding.btnOfflineAppointment.visibility = View.VISIBLE
                sheetBinding.btnOfflineAppointment.text = getString(R.string.book_in_person)
                sheetBinding.btnOfflineAppointment.setOnClickListener {
                    dialog.dismiss()
                    startAppointment()
                }
            }
            "MEDICATION" -> {
                actions.prescription?.let { medicines ->
                    sheetBinding.layoutPrescription.visibility = View.VISIBLE
                    sheetBinding.tvPrescriptionLabel.text = getString(R.string.recommended_medicines)
                    medicines.forEach { med ->
                        val tv = TextView(this).apply {
                            text = "• ${med.name} ${med.dosage} — ${med.frequency} × ${med.days}d"
                            textSize = 13f
                            setPadding(0, 4, 0, 4)
                        }
                        sheetBinding.llMedicines.addView(tv)
                    }
                }
                sheetBinding.btnPharmacy.visibility = View.VISIBLE
                sheetBinding.btnPharmacy.text = getString(R.string.go_to_pharmacy)
                sheetBinding.btnPharmacy.setOnClickListener {
                    dialog.dismiss()
                    startPharmacyWithPrescription(actions.prescription ?: emptyList())
                }
            }
        }
    }

    private fun showLegacySuggestionSheet(sheetBinding: BottomSheetConsultBinding, dialog: BottomSheetDialog, actions: ActionsDto) {
        sheetBinding.tvTitle.visibility = View.VISIBLE
        if (actions.suggestConsultation && actions.consultationType == "AI") {
            sheetBinding.btnAiConsult.visibility = View.VISIBLE
        } else if (actions.suggestConsultation) {
            sheetBinding.btnAiConsult.visibility = View.VISIBLE
            sheetBinding.btnDoctorConsult.visibility = View.VISIBLE
        }
        if (actions.suggestAppointment || actions.suggestConsultation) {
            sheetBinding.btnAppointment.visibility = View.VISIBLE
        }
        sheetBinding.btnAiConsult.setOnClickListener { dialog.dismiss(); startAiConsultation() }
        sheetBinding.btnDoctorConsult.setOnClickListener { dialog.dismiss(); startDoctorConsultation(actions.recommendedDoctorIds) }
        sheetBinding.btnAppointment.setOnClickListener { dialog.dismiss(); startAppointment() }
    }

    private fun startPharmacyWithPrescription(medicines: List<Medicine>) {
        lifecycleScope.launch {
            try {
                val consultation = RetrofitClient.api.createConsultation(
                    ConsultationRequest(userId, null, "AI_CONSULTATION")
                )
                val prescription = RetrofitClient.api.createPrescription(
                    PrescriptionRequest(consultation.id, medicines)
                )
                val intent = Intent(this@ChatbotActivity, PharmacyActivity::class.java)
                intent.putExtra("prescription_id", prescription.id)
                startActivityForResult(intent, REQUEST_PHARMACY)
            } catch (e: Exception) {
                val intent = Intent(this@ChatbotActivity, PharmacyActivity::class.java)
                startActivityForResult(intent, REQUEST_PHARMACY)
            }
        }
    }

    private fun startAiConsultation() {
        lifecycleScope.launch {
            try {
                val consultation = RetrofitClient.api.createConsultation(
                    ConsultationRequest(userId, null, "AI_CONSULTATION")
                )
                currentConsultationId = consultation.id
                val intent = Intent(this@ChatbotActivity, AiConsultationActivity::class.java)
                intent.putExtra("consultation_id", consultation.id)
                startActivityForResult(intent, REQUEST_AI_CONSULT)
            } catch (e: Exception) {
                Toast.makeText(this@ChatbotActivity, "Failed to start consultation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startDoctorConsultation(recommendedIds: List<Long>) {
        lifecycleScope.launch {
            try {
                val consultation = RetrofitClient.api.createConsultation(
                    ConsultationRequest(userId, null, "DOCTOR_CONSULTATION")
                )
                val intent = Intent(this@ChatbotActivity, DoctorListActivity::class.java)
                intent.putExtra("consultation_id", consultation.id)
                startActivityForResult(intent, REQUEST_DOCTOR_CONSULT)
            } catch (e: Exception) {
                Toast.makeText(this@ChatbotActivity, "Failed to start consultation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startAppointment() {
        startActivityForResult(
            Intent(this, AppointmentActivity::class.java),
            REQUEST_APPOINTMENT
        )
    }

    private fun addMessage(role: String, content: String) {
        messages.add(ChatItem(role, content))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun showTyping(show: Boolean) {
        binding.typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val msg = data?.getStringExtra("result_message") ?: return
        addMessage("ASSISTANT", msg)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val REQUEST_AI_CONSULT = 101
        const val REQUEST_DOCTOR_CONSULT = 102
        const val REQUEST_APPOINTMENT = 103
        const val REQUEST_PHARMACY = 104
    }
}
