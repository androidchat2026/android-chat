package com.androidchat.app.ui.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androidchat.app.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ConversationsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository(app)

    val conversations = repo.getAllConversations()

    init {
        repo.startListeningForMessages(viewModelScope)
    }

    fun startNewConversation(
        participantId: String,
        participantName: String,
        onDone: (conversationId: String, participantId: String, participantName: String) -> Unit
    ) {
        viewModelScope.launch {
            val conv = repo.getOrCreateConversation(participantId, participantName)
            onDone(conv.id, participantId, participantName)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repo.stopListening()
    }
}
