package com.androidchat.app.data.remote

/** Lightweight routing document stored in Firestore — deleted after recipient downloads it. */
data class FirebaseMessage(
    val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    /** "text" or "voice" */
    val type: String = "text",
    val text: String? = null,
    /** Firebase Storage path — only set for voice notes; deleted after local download */
    val voiceStoragePath: String? = null,
    val voiceDurationMs: Long = 0L,
    val timestamp: Long = 0L,
    /** true once the recipient has pulled it locally */
    val consumed: Boolean = false
)
