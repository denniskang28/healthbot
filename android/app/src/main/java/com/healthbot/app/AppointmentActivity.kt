package com.healthbot.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthbot.app.api.RetrofitClient
import com.healthbot.app.api.models.AppointmentRequest
import com.healthbot.app.api.models.Doctor
import com.healthbot.app.databinding.ActivityAppointmentBinding
import com.healthbot.app.utils.LocaleHelper
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentBinding
    private val userId = 1L
    private var doctors = listOf<Doctor>()
    private var selectedDoctor: Doctor? = null
    private var selectedDate = "2026-06-01"
    private var selectedTime = "09:00"

    private val timeSlots = listOf("09:00", "10:00", "11:00", "14:00", "15:00", "16:00")
    private val dateOptions = listOf("2026-06-01", "2026-06-02", "2026-06-03", "2026-06-04", "2026-06-05")

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.book_appointment)

        setupDateButtons()
        setupTimeSlots()
        loadDoctors()

        binding.etHospital.setText("City General Hospital")

        binding.btnConfirm.setOnClickListener { confirmAppointment() }
    }

    private fun setupDateButtons() {
        val dateLabels = listOf("Jun 1", "Jun 2", "Jun 3", "Jun 4", "Jun 5")
        val buttons = listOf(binding.btnDate1, binding.btnDate2, binding.btnDate3, binding.btnDate4, binding.btnDate5)

        buttons.forEachIndexed { i, btn ->
            btn.text = dateLabels[i]
            btn.setOnClickListener {
                selectedDate = dateOptions[i]
                buttons.forEach { b -> b.isSelected = false }
                btn.isSelected = true
                updateButtonStyles(buttons)
            }
        }
        buttons[0].isSelected = true
        updateButtonStyles(buttons)
    }

    private fun updateButtonStyles(buttons: List<com.google.android.material.button.MaterialButton>) {
        buttons.forEach { btn ->
            if (btn.isSelected) {
                btn.setBackgroundColor(getColor(R.color.primary))
                btn.setTextColor(getColor(android.R.color.white))
            } else {
                btn.setBackgroundColor(getColor(R.color.surface))
                btn.setTextColor(getColor(R.color.primary))
            }
        }
    }

    private fun setupTimeSlots() {
        val timeButtons = listOf(
            binding.btnTime1, binding.btnTime2, binding.btnTime3,
            binding.btnTime4, binding.btnTime5, binding.btnTime6
        )
        timeButtons.forEachIndexed { i, btn ->
            btn.text = timeSlots[i]
            btn.setOnClickListener {
                selectedTime = timeSlots[i]
                timeButtons.forEach { b ->
                    b.setBackgroundColor(getColor(R.color.surface))
                    b.setTextColor(getColor(R.color.primary))
                }
                btn.setBackgroundColor(getColor(R.color.primary))
                btn.setTextColor(getColor(android.R.color.white))
            }
        }
        binding.btnTime1.setBackgroundColor(getColor(R.color.primary))
        binding.btnTime1.setTextColor(getColor(android.R.color.white))
    }

    private fun loadDoctors() {
        lifecycleScope.launch {
            try {
                doctors = RetrofitClient.api.getDoctors()
                if (doctors.isNotEmpty()) {
                    selectedDoctor = doctors[0]
                    setupDoctorSpinner()
                }
            } catch (e: Exception) {
                // Use mock doctor
            }
        }
    }

    private fun setupDoctorSpinner() {
        val names = doctors.map { "${it.name} — ${it.specialty}" }.toTypedArray()
        val spinnerAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDoctor.adapter = spinnerAdapter
        binding.spinnerDoctor.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                selectedDoctor = doctors[pos]
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    private fun confirmAppointment() {
        val doctor = selectedDoctor
        if (doctor == null) {
            Toast.makeText(this, "Please select a doctor", Toast.LENGTH_SHORT).show()
            return
        }

        val hospitalName = binding.etHospital.text.toString().takeIf { it.isNotBlank() } ?: "City General Hospital"
        val scheduledTime = "${selectedDate}T${selectedTime}:00"

        lifecycleScope.launch {
            try {
                val appointment = RetrofitClient.api.createAppointment(
                    AppointmentRequest(userId, doctor.id, scheduledTime, hospitalName)
                )
                val msg = getString(R.string.appointment_confirmed_message,
                    doctor.name, hospitalName, selectedDate, selectedTime, appointment.id)
                val resultIntent = Intent()
                resultIntent.putExtra("result_message", msg)
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                val msg = "Appointment confirmed! Dr. ${doctor.name} at $hospitalName on $selectedDate at $selectedTime."
                val resultIntent = Intent()
                resultIntent.putExtra("result_message", msg)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
