package com.androidchat.app.data.repository

import android.content.Context
import com.androidchat.app.data.local.AppDatabase
import com.androidchat.app.data.local.entities.*
import com.androidchat.app.data.remote.FirebaseMessage
import com.androidchat.app.data.remote.FirebaseRepository
import com.androidchat.app.utils.Constants
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val firebase = FirebaseRepository()
    private var messageListener: ListenerRegistration? = null

    val currentUserId get() = firebase.currentUserId

    // ── Conversations ─────────────────────────────────────────────────────────

    fun getAllConversations() = db.conversationDao().getAllConversations()

    suspend fun getOrCreateConversation(
        participantId: String,
        participantName: String,
        avatarUrl: String? = null
    ): ConversationEntity {
        val existing = db.conversationDao().getConversationWithUser(participantId)
        if (existing != null) return existing

        val conv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            participantId = participantId,
            participantName = participantName,
            participantAvatarUrl = avatarUrl
        )
        db.conversationDao().insert(conv)
        return conv
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun getMessages(conversationId: String) = db.messageDao().getMessagesForConversation(conversationId)

    suspend fun sendTextMessage(
        conversationId: String,
        recipientId: String,
        text: String
    ) {
        val id = UUID.randomUUID().toString()
        val entity = MessageEntity(
            id = id,
            conversationId = conversationId,
            senderId = currentUserId ?: return,
            recipientId = recipientId,
            type = MessageType.TEXT,
            text = text,
            status = MessageStatus.SENDING
        )
        db.messageDao().insert(entity)

        val firebaseMsg = FirebaseMessage(
            id = id,
            senderId = currentUserId ?: "",
            recipientId = recipientId,
            type = "text",
            text = text,
            timestamp = entity.timestamp
        )
        firebase.sendTextMessage(firebaseMsg)
        db.messageDao().updateStatus(id, MessageStatus.SENT)
        updateConversationLastMessage(conversationId, text, MessageType.TEXT)
    }

    suspend fun sendVoiceNote(
        conversationId: String,
        recipientId: String,
        localFile: File,
        durationMs: Long
    ) {
        val id = UUID.randomUUID().toString()
        val entity = MessageEntity(
            id = id,
            conversationId = conversationId,
            senderId = currentUserId ?: return,
            recipientId = recipientId,
            type = MessageType.VOICE,
            voiceNotePath = localFile.absolutePath,
            voiceDurationMs = durationMs,
            status = MessageStatus.SENDING
        )
        db.messageDao().insert(entity)

        firebase.uploadAndSendVoiceNote(id, localFile, recipientId, durationMs)
        db.messageDao().updateStatus(id, MessageStatus.SENT)
        updateConversationLastMessage(conversationId, "🎤 Voice note", MessageType.VOICE)
    }

    // ── Incoming message listener ─────────────────────────────────────────────

    fun startListeningForMessages(scope: CoroutineScope) {
        val uid = currentUserId ?: return
        messageListener = firebase.listenForIncomingMessages(uid) { msg ->
            scope.launch(Dispatchers.IO) { handleIncomingMessage(msg) }
        }
    }

    private suspend fun handleIncomingMessage(msg: FirebaseMessage) {
        // Resolve or create conversation
        val conv = getOrCreateConversation(msg.senderId, msg.senderId)

        when (msg.type) {
            "voice" -> {
                val destDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
                val destFile = File(destDir, "${msg.id}.m4a")

                if (msg.voiceStoragePath != null) {
                    // Download to device — remote blob is deleted inside downloadVoiceNote()
                    firebase.downloadVoiceNote(msg.voiceStoragePath, destFile)
                }

                val entity = MessageEntity(
                    id = msg.id,
                    conversationId = conv.id,
                    senderId = msg.senderId,
                    recipientId = currentUserId ?: "",
                    type = MessageType.VOICE,
                    voiceNotePath = destFile.absolutePath,
                    voiceDurationMs = msg.voiceDurationMs,
                    status = MessageStatus.DELIVERED,
                    timestamp = msg.timestamp
                )
                db.messageDao().insert(entity)
                updateConversationLastMessage(conv.id, "🎤 Voice note", MessageType.VOICE, unread = true)
            }
            else -> {
                val entity = MessageEntity(
                    id = msg.id,
                    conversationId = conv.id,
                    senderId = msg.senderId,
                    recipientId = currentUserId ?: "",
                    type = MessageType.TEXT,
                    text = msg.text,
                    status = MessageStatus.DELIVERED,
                    timestamp = msg.timestamp
                )
                db.messageDao().insert(entity)
                updateConversationLastMessage(conv.id, msg.text ?: "", MessageType.TEXT, unread = true)
            }
        }

        // Mark consumed so Firebase cleanup job can remove it
        firebase.markMessageConsumed(msg.id)
    }

    suspend fun markConversationRead(conversationId: String) {
        db.messageDao().markAllRead(conversationId)
        db.conversationDao().clearUnreadCount(conversationId)
    }

    fun stopListening() = messageListener?.remove()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun updateConversationLastMessage(
        conversationId: String,
        text: String,
        type: MessageType,
        unread: Boolean = false
    ) {
        val conv = db.conversationDao().getConversationById(conversationId) ?: return
        db.conversationDao().update(
            conv.copy(
                lastMessageText = text,
                lastMessageType = type,
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = if (unread) conv.unreadCount + 1 else conv.unreadCount,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
