package com.androidchat.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val participantId: String,
    val participantName: String,
    val participantAvatarUrl: String? = null,
    val lastMessageText: String? = null,
    val lastMessageType: MessageType = MessageType.TEXT,
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
