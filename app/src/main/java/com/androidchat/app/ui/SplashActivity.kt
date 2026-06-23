package com.androidchat.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androidchat.app.ui.auth.LoginActivity
import com.androidchat.app.ui.conversations.ConversationsActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        val next = when {
            user == null -> LoginActivity::class.java
            !user.isEmailVerified -> {
                // Signed in but never verified — sign out and ask them to verify
                FirebaseAuth.getInstance().signOut()
                LoginActivity::class.java
            }
            else -> ConversationsActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
