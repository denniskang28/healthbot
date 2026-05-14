package com.healthbot.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthbot.app.api.RetrofitClient
import com.healthbot.app.api.models.Doctor
import com.healthbot.app.databinding.ActivityDoctorListBinding
import com.healthbot.app.databinding.ItemDoctorBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.healthbot.app.utils.LocaleHelper
import kotlinx.coroutines.launch

class DoctorListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorListBinding
    private var consultationId: Long = -1L

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.select_doctor)

        consultationId = intent.getLongExtra("consultation_id", -1L)

        binding.rvDoctors.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            try {
                val doctors = RetrofitClient.api.getDoctors()
                binding.rvDoctors.adapter = DoctorAdapter(doctors) { doctor ->
                    val intent = Intent(this@DoctorListActivity, VideoConsultationActivity::class.java)
                    intent.putExtra("doctor_id", doctor.id)
                    intent.putExtra("doctor_name", doctor.name)
                    intent.putExtra("consultation_id", consultationId)
                    startActivityForResult(intent, 200)
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorListActivity, "Failed to load doctors", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && data != null) {
            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    inner class DoctorAdapter(
        private val doctors: List<Doctor>,
        private val onSelect: (Doctor) -> Unit
    ) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemDoctorBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = doctors.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val d = doctors[position]
            with(holder.binding) {
                tvAvatarInitials.text = d.avatarInitials
                tvDoctorName.text = d.name
                tvSpecialty.text = d.specialty
                tvRating.text = "⭐ ${d.rating}"
                tvBio.text = d.bio
                tvAvailable.text = if (d.available) "● Available" else "● Unavailable"
                btnConsult.text = getString(R.string.consult_now)
                btnConsult.setOnClickListener { onSelect(d) }
            }
        }
    }
}
