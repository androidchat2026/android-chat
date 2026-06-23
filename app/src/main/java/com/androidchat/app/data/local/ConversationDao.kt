package com.androidchat.app.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.androidchat.app.data.local.entities.ConversationEntity

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): LiveData<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE participantId = :userId")
    suspend fun getConversationWithUser(userId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun clearUnreadCount(id: String)

    @Delete
    suspend fun delete(conversation: ConversationEntity)
}
