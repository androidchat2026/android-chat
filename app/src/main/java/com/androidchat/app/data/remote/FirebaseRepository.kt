package com.androidchat.app.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Handles all Firebase interactions.
 *
 * Design principle — Firebase is used for routing only, not permanent storage:
 *   - Text messages: stored in Firestore under /messages/{id}, marked consumed=true after receipt.
 *   - Voice notes: uploaded to Storage temporarily; downloaded to device; Storage file deleted.
 *   - Firestore docs are cleaned up by the 24-hour GitHub Actions cron (firebase-cleanup.yml).
 */
class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

    // ── Sending messages ──────────────────────────────────────────────────────

    suspend fun sendTextMessage(message: FirebaseMessage) {
        db.collection("messages").document(message.id).set(message).await()
    }

    /**
     * Uploads voice file to Storage (temporary), then writes a Firestore routing doc.
     * Returns the storage path so it can be included in the Firestore document.
     */
    suspend fun uploadAndSendVoiceNote(
        messageId: String,
        localFile: File,
        recipientId: String,
        durationMs: Long
    ): String {
        val storagePath = "voice_notes/${recipientId}/${messageId}.m4a"
        val ref = storage.reference.child(storagePath)
        ref.putFile(Uri.fromFile(localFile)).await()

        val message = FirebaseMessage(
            id = messageId,
            senderId = currentUserId ?: "",
            recipientId = recipientId,
            type = "voice",
            voiceStoragePath = storagePath,
            voiceDurationMs = durationMs,
            timestamp = System.currentTimeMillis()
        )
        db.collection("messages").document(messageId).set(message).await()
        return storagePath
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
     * Downloads a voice note from Storage to a local file, then deletes the remote blob.
     * This keeps Firebase Storage usage near zero.
     */
    suspend fun downloadVoiceNote(storagePath: String, destFile: File) {
        storage.reference.child(storagePath).getFile(destFile).await()
        // Delete remote file immediately after download
        storage.reference.child(storagePath).delete().await()
    }

    /** Mark a Firestore message document as consumed so the cleanup job can remove it. */
    suspend fun markMessageConsumed(messageId: String) {
        db.collection("messages").document(messageId)
            .update("consumed", true, "consumedAt", System.currentTimeMillis())
            .await()
    }

    // ── Message status updates ────────────────────────────────────────────────

    suspend fun updateMessageStatus(messageId: String, status: String) {
        db.collection("messages").document(messageId)
            .update("status", status)
            .await()
    }
}
