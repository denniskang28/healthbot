package com.healthbot.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.healthbot.app.databinding.ActivityMainBinding
import com.healthbot.app.utils.LocaleHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupBotBanner()
        setupLanguageToggle()
        setupFeatureCards()
    }

    private fun setupBotBanner() {
        val pulseAnim = ObjectAnimator.ofFloat(binding.botIconText, "scaleX", 1f, 1.15f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
        }
        val pulseAnimY = ObjectAnimator.ofFloat(binding.botIconText, "scaleY", 1f, 1.15f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
        }
        pulseAnim.start()
        pulseAnimY.start()

        binding.bannerCard.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    private fun setupLanguageToggle() {
        val currentLang = LocaleHelper.getLanguage(this)
        updateLangButton(currentLang)

        binding.btnLanguage.setOnClickListener {
            val newLang = if (LocaleHelper.getLanguage(this) == "en") "zh" else "en"
            LocaleHelper.setLocale(this, newLang)
            recreate()
        }
    }

    private fun updateLangButton(lang: String) {
        binding.btnLanguage.text = if (lang == "en") "中文" else "EN"
    }

    private fun setupFeatureCards() {
        binding.cardDoctors.setOnClickListener {
            startActivity(Intent(this, DoctorListActivity::class.java))
        }
    }
}
