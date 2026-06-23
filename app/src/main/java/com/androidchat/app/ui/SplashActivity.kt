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
        val next = if (FirebaseAuth.getInstance().currentUser != null)
            ConversationsActivity::class.java
        else
            LoginActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }
}
