package com.androidchat.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val fcmToken: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)
