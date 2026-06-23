package com.androidchat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.androidchat.app.utils.Constants
import com.google.android.gms.ads.MobileAds

class ChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialise AdMob SDK once at app start (required before loading any ad)
        MobileAds.initialize(this)

        // Create notification channel for incoming messages (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID_MESSAGES,
                Constants.CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming chat messages and voice notes"
                enableVibration(true)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
