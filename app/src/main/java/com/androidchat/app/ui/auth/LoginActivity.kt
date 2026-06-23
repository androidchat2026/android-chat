package com.androidchat.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidchat.app.databinding.ActivityLoginBinding
import com.androidchat.app.ui.conversations.ConversationsActivity
import com.androidchat.app.utils.toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            toast("Please fill in all fields")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: return@launch

                if (!user.isEmailVerified) {
                    // Block login — offer to resend the verification email
                    auth.signOut()
                    setLoading(false)
                    showUnverifiedDialog(user.email ?: email)
                    return@launch
                }

                startActivity(Intent(this@LoginActivity, ConversationsActivity::class.java))
                finishAffinity()
            } catch (e: Exception) {
                toast(e.message ?: "Login failed", long = true)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val email = binding.etEmail.text.toString().trim()
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.setText(email)
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email address and we will send you a password reset link.")
            .setView(input)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val resetEmail = input.text.toString().trim()
                if (resetEmail.isEmpty()) { toast("Please enter your email"); return@setPositiveButton }
                sendPasswordReset(resetEmail)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordReset(email: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                toast("Password reset email sent to $email — check your inbox.", long = true)
            } catch (e: Exception) {
                toast(e.message ?: "Failed to send reset email", long = true)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showUnverifiedDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Email Not Verified")
            .setMessage("Please verify your email address before logging in.\n\nCheck your inbox at:\n$email\n\nDidn't receive it?")
            .setPositiveButton("Resend Email") { _, _ -> resendVerificationEmail(email) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resendVerificationEmail(email: String) {
        val password = binding.etPassword.text.toString()
        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.sendEmailVerification()?.await()
                auth.signOut()
                toast("Verification email resent. Please check your inbox.", long = true)
            } catch (e: Exception) {
                toast("Could not resend: ${e.message}", long = true)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}
