package com.healthbot.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbot.app.api.RetrofitClient
import com.healthbot.app.api.models.Medicine
import com.healthbot.app.api.models.PurchaseRequest
import com.healthbot.app.databinding.ActivityPharmacyBinding
import com.healthbot.app.databinding.ItemMedicineBinding
import com.healthbot.app.utils.LocaleHelper
import kotlinx.coroutines.launch
import kotlin.random.Random

class PharmacyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPharmacyBinding
    private val userId = 1L
    private var prescriptionId = -1L
    private var purchaseId = -1L
    private var medicines = listOf<Medicine>()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPharmacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.pharmacy)

        prescriptionId = intent.getLongExtra("prescription_id", -1L)
        purchaseId = intent.getLongExtra("purchase_id", -1L)

        binding.rvMedicines.layoutManager = LinearLayoutManager(this)

        loadPrescription()

        binding.btnCompletePurchase.text = getString(R.string.complete_purchase)
        binding.btnCompletePurchase.setOnClickListener { completePurchase() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun loadPrescription() {
        if (prescriptionId > 0) {
            lifecycleScope.launch {
                try {
                    val prescription = RetrofitClient.api.getPrescription(prescriptionId)
                    medicines = prescription.medicines
                    binding.tvPrescriptionId.text = "Prescription #${prescription.id}"
                    showMedicines(medicines)

                    if (purchaseId <= 0) {
                        val purchase = RetrofitClient.api.createPurchase(
                            PurchaseRequest(prescriptionId, userId)
                        )
                        purchaseId = purchase.id
                    }
                } catch (e: Exception) {
                    showMockMedicines()
                }
            }
        } else {
            showMockMedicines()
        }
    }

    private fun showMockMedicines() {
        medicines = listOf(
            Medicine("Amoxicillin", "500mg", "3 times daily", 7),
            Medicine("Ibuprofen", "400mg", "twice daily", 5)
        )
        binding.tvPrescriptionId.text = "Prescription #Demo"
        showMedicines(medicines)
    }

    private fun showMedicines(meds: List<Medicine>) {
        val prices = meds.map { Random.nextDouble(15.0, 45.0) }
        val total = prices.sum()

        binding.rvMedicines.adapter = MedicineAdapter(meds, prices)
        binding.tvTotal.text = "Total: $%.2f".format(total)
    }

    private fun completePurchase() {
        lifecycleScope.launch {
            try {
                if (purchaseId > 0) {
                    RetrofitClient.api.completePurchase(purchaseId)
                }
                val resultIntent = Intent()
                resultIntent.putExtra("result_message",
                    getString(R.string.purchase_complete_message))
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                val resultIntent = Intent()
                resultIntent.putExtra("result_message",
                    getString(R.string.purchase_complete_message))
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    inner class MedicineAdapter(
        private val meds: List<Medicine>,
        private val prices: List<Double>
    ) : RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemMedicineBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemMedicineBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = meds.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val m = meds[position]
            with(holder.binding) {
                tvMedicineName.text = m.name
                tvDosageFrequency.text = "${m.dosage} — ${m.frequency}"
                tvDuration.text = "${m.days} days"
                tvPrice.text = "$%.2f".format(prices[position])
            }
        }
    }
}
