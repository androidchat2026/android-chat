package com.androidchat.app.data.remote

import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * All Firebase interactions — free Spark tier only, no Storage required.
 *
 * Text messages  → Firestore /messages/{id}, consumed=true after receipt, deleted by cron.
 * Voice notes    → base64-encoded in the same Firestore document (≤ 50 s recording enforced
 *                  in UI → ≤ ~960 KB base64 → safely under Firestore 1 MB document limit).
 *                  Bytes decoded and written to device on receipt; Firestore doc marked consumed.
 * Cleanup        → GitHub Actions cron (firebase-cleanup.yml) deletes consumed/stale docs daily.
 */
class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUserId get() = auth.currentUser?.uid

    // ── Auth ─────────────────────────────────────────────────────────────────

    suspend fun signIn(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun register(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    fun signOut() = auth.signOut()

    // ── User presence ─────────────────────────────────────────────────────────

    suspend fun updateUserProfile(uid: String, name: String, fcmToken: String) {
        db.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "displayName" to name,
                "fcmToken" to fcmToken,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun setOnlineStatus(uid: String, online: Boolean) {
        db.collection("users").document(uid)
            .update("isOnline", online, "lastSeen", System.currentTimeMillis())
            .await()
    }

    suspend fun getUserProfile(uid: String): Map<String, Any>? =
        db.collection("users").document(uid).get().await().data

    suspend fun searchUserByEmail(email: String): Map<String, Any>? =
        db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get().await()
            .documents.firstOrNull()?.data

    suspend fun searchUserByUsername(username: String): Map<String, Any>? =
        db.collection("users")
            .whereEqualTo("username", username.lowercase())
            .limit(1)
            .get().await()
            .documents.firstOrNull()?.data

    /** Search by email or @username — returns the first match found. */
    suspend fun searchUser(query: String): Map<String, Any>? =
        if (query.contains("@") && query.contains("."))
            searchUserByEmail(query)
        else
            searchUserByUsername(query.removePrefix("@"))

    // ── Sending messages ──────────────────────────────────────────────────────

    suspend fun sendTextMessage(message: FirebaseMessage) {
        db.collection("messages").document(message.id).set(message).await()
    }

    /**
     * Encodes the voice file as base64 and stores it inline in a Firestore document.
     * No Firebase Storage needed — works entirely on the free Spark plan.
     *
     * Caller must enforce max 50-second recording to stay under Firestore's 1 MB limit.
     */
    suspend fun sendVoiceMessage(
        messageId: String,
        localFile: File,
        recipientId: String,
        durationMs: Long
    ) {
        val bytes = localFile.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val message = FirebaseMessage(
            id = messageId,
            senderId = currentUserId ?: "",
            recipientId = recipientId,
            type = "voice",
            voiceBase64 = base64,
            voiceDurationMs = durationMs,
            timestamp = System.currentTimeMillis()
        )
        db.collection("messages").document(messageId).set(message).await()
    }

    // ── Receiving messages ────────────────────────────────────────────────────

    fun listenForIncomingMessages(
        userId: String,
        onMessage: (FirebaseMessage) -> Unit
    ): ListenerRegistration {
        return db.collection("messages")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("consumed", false)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    val msg = change.document.toObject(FirebaseMessage::class.java)
                    onMessage(msg)
                }
            }
    }

    /**
     * Decodes a base64 voice note from Firestore and writes it to [destFile].
     * Called immediately on receipt — no network download required beyond Firestore.
     */
    fun decodeVoiceNote(base64: String, destFile: File) {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        destFile.writeBytes(bytes)
    }

    /** Mark document consumed so the cleanup cron can remove it. */
    suspend fun markMessageConsumed(messageId: String) {
        db.collection("messages").document(messageId)
            .update(
                "consumed", true,
                "consumedAt", System.currentTimeMillis(),
                "voiceBase64", null   // clear the payload to free Firestore quota immediately
            )
            .await()
    }

    suspend fun updateMessageStatus(messageId: String, status: String) {
        db.collection("messages").document(messageId)
            .update("status", status)
            .await()
    }
}
