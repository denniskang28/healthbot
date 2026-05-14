package com.healthbot.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbot.app.api.RetrofitClient
import com.healthbot.app.api.models.Medicine
import com.healthbot.app.api.models.PrescriptionRequest
import com.healthbot.app.api.models.PurchaseRequest
import com.healthbot.app.databinding.ActivityVideoConsultationBinding
import com.healthbot.app.utils.LocaleHelper
import kotlinx.coroutines.launch

class VideoConsultationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoConsultationBinding
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var doctorName = "Dr. Smith"
    private var consultationId = -1L
    private var isMuted = false
    private var isCameraOff = false

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoConsultationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        doctorName = intent.getStringExtra("doctor_name") ?: "Dr. Smith"
        consultationId = intent.getLongExtra("consultation_id", -1L)

        binding.tvDoctorNameVideo.text = doctorName
        binding.tvConnectionStatus.text = "Connected • 00:00"

        startTimer()

        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            binding.btnMute.text = if (isMuted) "🔇 Unmute" else "🎤 Mute"
        }
        binding.btnCamera.setOnClickListener {
            isCameraOff = !isCameraOff
            binding.btnCamera.text = if (isCameraOff) "📷 Camera Off" else "📷 Camera"
        }
        binding.btnEnd.setOnClickListener { showEndDialog() }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                seconds++
                val min = seconds / 60
                val sec = seconds % 60
                binding.tvConnectionStatus.text = "Connected • %02d:%02d".format(min, sec)
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun showEndDialog() {
        handler.removeCallbacks(timerRunnable)

        val mockMedicines = listOf(
            Medicine("Amoxicillin", "500mg", "3 times daily", 7),
            Medicine("Ibuprofen", "400mg", "twice daily", 5)
        )

        val prescriptionText = mockMedicines.joinToString("\n") {
            "• ${it.name} ${it.dosage} — ${it.frequency} for ${it.days} days"
        }

        AlertDialog.Builder(this)
            .setTitle("Consultation Complete")
            .setMessage("Dr. $doctorName has prescribed:\n\n$prescriptionText")
            .setPositiveButton("Go to Pharmacy") { _, _ ->
                savePrescriptionAndGoToPharmacy(mockMedicines)
            }
            .setNegativeButton("Cancel") { _, _ -> handler.postDelayed(timerRunnable, 1000) }
            .setCancelable(false)
            .show()
    }

    private fun savePrescriptionAndGoToPharmacy(medicines: List<Medicine>) {
        lifecycleScope.launch {
            try {
                val prescription = if (consultationId > 0) {
                    RetrofitClient.api.createPrescription(PrescriptionRequest(consultationId, medicines))
                } else {
                    null
                }

                if (consultationId > 0) {
                    RetrofitClient.api.completeConsultation(consultationId, mapOf("notes" to "Consultation completed"))
                }

                val intent = Intent(this@VideoConsultationActivity, PharmacyActivity::class.java)
                if (prescription != null) {
                    intent.putExtra("prescription_id", prescription.id)
                }
                intent.putExtra("consultation_id", consultationId)
                startActivityForResult(intent, 300)
            } catch (e: Exception) {
                // Still go to pharmacy with mock data
                val intent = Intent(this@VideoConsultationActivity, PharmacyActivity::class.java)
                startActivityForResult(intent, 300)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 300 && data != null) {
            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
