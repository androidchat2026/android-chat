package com.androidchat.app.data.local

import androidx.room.*
import com.androidchat.app.data.local.entities.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET fcmToken = :token WHERE uid = :uid")
    suspend fun updateFcmToken(uid: String, token: String)

    @Query("UPDATE users SET isOnline = :online, lastSeen = :lastSeen WHERE uid = :uid")
    suspend fun updateOnlineStatus(uid: String, online: Boolean, lastSeen: Long)

    @Delete
    suspend fun delete(user: UserEntity)
}
