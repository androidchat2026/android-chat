package com.androidchat.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidchat.app.databinding.ActivityRegisterBinding
import com.androidchat.app.ui.conversations.ConversationsActivity
import com.androidchat.app.utils.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name     = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim().lowercase()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm  = binding.etConfirmPassword.text.toString()

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            toast("Please fill in all fields")
            return
        }
        if (!username.matches(Regex("^[a-z0-9_.]{3,30}$"))) {
            toast("Username: 3–30 characters, letters/numbers/._  only")
            return
        }
        if (password != confirm) {
            toast("Passwords do not match")
            return
        }
        if (password.length < 6) {
            toast("Password must be at least 6 characters")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                // Check username uniqueness before creating the account
                val taken = db.collection("users")
                    .whereEqualTo("username", username)
                    .limit(1).get().await()
                if (!taken.isEmpty) {
                    toast("Username @$username is already taken")
                    setLoading(false)
                    return@launch
                }

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: return@launch

                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                ).await()

                val fcmToken = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
                db.collection("users").document(user.uid).set(
                    mapOf(
                        "uid"         to user.uid,
                        "displayName" to name,
                        "username"    to username,
                        "email"       to email,
                        "fcmToken"    to fcmToken,
                        "isOnline"    to true,
                        "createdAt"   to System.currentTimeMillis()
                    )
                ).await()

                startActivity(Intent(this@RegisterActivity, ConversationsActivity::class.java))
                finishAffinity()
            } catch (e: Exception) {
                toast(e.message ?: "Registration failed", long = true)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }
}
