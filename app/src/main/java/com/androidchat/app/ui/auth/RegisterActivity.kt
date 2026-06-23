package com.androidchat.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidchat.app.databinding.ActivityRegisterBinding
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
            var createdUser: com.google.firebase.auth.FirebaseUser? = null
            try {
                // 1. Create auth account
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                createdUser = result.user ?: return@launch

                // 2. Check username uniqueness
                val taken = db.collection("users")
                    .whereEqualTo("username", username)
                    .limit(1).get().await()
                if (!taken.isEmpty) {
                    createdUser.delete().await()
                    toast("Username @$username is already taken")
                    setLoading(false)
                    return@launch
                }

                // 3. Save display name to Firebase Auth profile
                createdUser.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                ).await()

                // 4. Save profile to Firestore
                val fcmToken = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
                db.collection("users").document(createdUser.uid).set(
                    mapOf(
                        "uid"              to createdUser.uid,
                        "displayName"      to name,
                        "username"         to username,
                        "email"            to email,
                        "fcmToken"         to fcmToken,
                        "isOnline"         to false,
                        "emailVerified"    to false,
                        "createdAt"        to System.currentTimeMillis()
                    )
                ).await()

                // 5. Send verification email
                createdUser.sendEmailVerification().await()

                // 6. Sign out — user must verify email before logging in
                auth.signOut()

                // 7. Show confirmation dialog then go back to Login
                setLoading(false)
                showVerificationDialog(email)

            } catch (e: Exception) {
                runCatching { createdUser?.delete()?.await() }
                toast(e.message ?: "Registration failed", long = true)
                setLoading(false)
            }
        }
    }

    private fun showVerificationDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Verify Your Email")
            .setMessage("A verification link has been sent to:\n\n$email\n\nPlease check your inbox (and spam folder) and click the link to activate your account before logging in.")
            .setPositiveButton("Go to Login") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }
}
