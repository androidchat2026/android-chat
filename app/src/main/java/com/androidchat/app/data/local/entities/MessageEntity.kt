package com.androidchat.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType { TEXT, VOICE }
enum class MessageStatus { SENDING, SENT, DELIVERED, READ }

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val type: MessageType,
    val text: String? = null,
    /** Absolute path to the voice note file stored on-device */
    val voiceNotePath: String? = null,
    /** Duration in milliseconds */
    val voiceDurationMs: Long = 0L,
    val status: MessageStatus = MessageStatus.SENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
