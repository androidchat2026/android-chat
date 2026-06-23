package com.androidchat.app.ui.profile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidchat.app.databinding.ActivityProfileBinding
import com.androidchat.app.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply { title = "Profile"; setDisplayHomeAsUpEnabled(true) }

        val user = auth.currentUser ?: return
        binding.etName.setText(user.displayName)
        binding.tvEmail.setText(user.email)

        // Load username from Firestore
        lifecycleScope.launch {
            try {
                val doc = db.collection("users").document(user.uid).get().await()
                binding.etUsername.setText(doc.getString("username") ?: "")
            } catch (_: Exception) {}
        }

        binding.btnSave.setOnClickListener { saveProfile() }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) { toast("Name cannot be empty"); return }

        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                auth.currentUser?.apply {
                    updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
                    db.collection("users").document(uid).update("displayName", name).await()
                }
                toast("Profile updated")
            } catch (e: Exception) {
                toast(e.message ?: "Update failed")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
