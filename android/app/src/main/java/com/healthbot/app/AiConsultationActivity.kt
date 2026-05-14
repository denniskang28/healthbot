package com.healthbot.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbot.app.api.RetrofitClient
import com.healthbot.app.api.models.AiConsultationRequest
import com.healthbot.app.api.models.Medicine
import com.healthbot.app.api.models.PrescriptionRequest
import com.healthbot.app.api.models.PurchaseRequest
import com.healthbot.app.databinding.ActivityAiConsultationBinding
import com.healthbot.app.databinding.ItemChatMessageAssistantBinding
import com.healthbot.app.databinding.ItemChatMessageUserBinding
import com.healthbot.app.utils.LocaleHelper
import kotlinx.coroutines.launch

class AiConsultationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiConsultationBinding
    private val messages = mutableListOf<ChatbotActivity.ChatItem>()
    private lateinit var adapter: ConsultAdapter
    private val userId = 1L
    private var consultationId = -1L

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiConsultationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.ai_consultation_title)

        consultationId = intent.getLongExtra("consultation_id", -1L)

        adapter = ConsultAdapter(messages)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter

        addMessage("ASSISTANT", getString(R.string.ai_doctor_welcome))

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        binding.etMessage.setText("")
        addMessage("USER", text)
        binding.typingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.sendAiConsultationMessage(
                    userId,
                    AiConsultationRequest(consultationId, text)
                )
                binding.typingIndicator.visibility = View.GONE
                addMessage("ASSISTANT", response.message.content)

                if (response.isComplete && response.prescription != null) {
                    showCompletionDialog(response.prescription)
                }
            } catch (e: Exception) {
                binding.typingIndicator.visibility = View.GONE
                addMessage("ASSISTANT", "I'm having trouble processing your response. Please try again.")
            }
        }
    }

    private fun showCompletionDialog(medicines: List<Medicine>) {
        val prescriptionText = medicines.joinToString("\n") {
            "• ${it.name} ${it.dosage} — ${it.frequency} for ${it.days} days"
        }

        AlertDialog.Builder(this)
            .setTitle("AI Consultation Complete")
            .setMessage("Based on your symptoms, I've prepared the following prescription:\n\n$prescriptionText\n\n⚠️ This is an AI-generated prescription. Please consult a real doctor for serious conditions.")
            .setPositiveButton("Go to Pharmacy") { _, _ ->
                savePrescriptionAndNavigate(medicines)
            }
            .setCancelable(false)
            .show()
    }

    private fun savePrescriptionAndNavigate(medicines: List<Medicine>) {
        lifecycleScope.launch {
            try {
                val prescription = RetrofitClient.api.createPrescription(
                    PrescriptionRequest(consultationId, medicines)
                )
                val purchase = RetrofitClient.api.createPurchase(
                    PurchaseRequest(prescription.id, userId)
                )
                val intent = Intent(this@AiConsultationActivity, PharmacyActivity::class.java)
                intent.putExtra("prescription_id", prescription.id)
                intent.putExtra("purchase_id", purchase.id)
                startActivityForResult(intent, 400)
            } catch (e: Exception) {
                val intent = Intent(this@AiConsultationActivity, PharmacyActivity::class.java)
                startActivityForResult(intent, 400)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 400 && data != null) {
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun addMessage(role: String, content: String) {
        messages.add(ChatbotActivity.ChatItem(role, content))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    inner class ConsultAdapter(private val items: List<ChatbotActivity.ChatItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int) = if (items[position].role == "USER") 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                object : RecyclerView.ViewHolder(ItemChatMessageUserBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
            } else {
                object : RecyclerView.ViewHolder(ItemChatMessageAssistantBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
            }
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            if (item.role == "USER") {
                val b = ItemChatMessageUserBinding.bind(holder.itemView)
                b.tvMessage.text = item.content
            } else {
                val b = ItemChatMessageAssistantBinding.bind(holder.itemView)
                b.tvMessage.text = item.content
            }
        }
    }
}
