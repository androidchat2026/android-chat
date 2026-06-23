package com.androidchat.app.ui.support

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androidchat.app.databinding.ActivitySupportBinding

class SupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportBinding

    companion object {
        private const val WHATSAPP_NUMBER = "201150403775"   // E.164 without the +
        private const val SUPPORT_EMAIL   = "androidchat2026@gmail.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Support"
            setDisplayHomeAsUpEnabled(true)
        }

        binding.btnWhatsApp.setOnClickListener { openWhatsApp() }
        binding.btnEmail.setOnClickListener { openEmail() }
    }

    private fun openWhatsApp() {
        val uri = Uri.parse("https://wa.me/$WHATSAPP_NUMBER")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(Intent.createChooser(intent, "Open WhatsApp"))
    }

    private fun openEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "Android Chat Support")
        }
        startActivity(Intent.createChooser(intent, "Send Email"))
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
