package com.androidchat.app.data.remote

/**
 * Routing document stored in Firestore — deleted after recipient saves it locally.
 *
 * Voice notes are base64-encoded directly in this document (no Storage needed).
 * At 96kbps AAC: 60 s ≈ 720 KB raw → ~960 KB base64 — safely under Firestore's 1 MB limit.
 * Max recording is enforced to 50 seconds in ChatActivity to stay within budget.
 */
data class FirebaseMessage(
    val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    /** "text" or "voice" */
    val type: String = "text",
    val text: String? = null,
    /** Base64-encoded voice note bytes — set for type=="voice", null otherwise */
    val voiceBase64: String? = null,
    val voiceDurationMs: Long = 0L,
    val timestamp: Long = 0L,
    /** true once the recipient has saved the message locally */
    val consumed: Boolean = false,
    val consumedAt: Long = 0L
)
