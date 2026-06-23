package com.androidchat.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.androidchat.app.data.repository.ChatRepository
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository(app)

    val isSending = MutableLiveData(false)
    val error = MutableLiveData<String?>()

    fun getMessages(conversationId: String) = repo.getMessages(conversationId)

    fun sendText(conversationId: String, recipientId: String, text: String) {
        viewModelScope.launch {
            isSending.value = true
            runCatching { repo.sendTextMessage(conversationId, recipientId, text) }
                .onFailure { error.value = it.message }
            isSending.value = false
        }
    }

    fun sendVoice(conversationId: String, recipientId: String, file: File, durationMs: Long) {
        viewModelScope.launch {
            isSending.value = true
            runCatching { repo.sendVoiceNote(conversationId, recipientId, file, durationMs) }
                .onFailure { error.value = it.message }
            isSending.value = false
        }
    }

    fun markRead(conversationId: String) {
        viewModelScope.launch { repo.markConversationRead(conversationId) }
    }
}
