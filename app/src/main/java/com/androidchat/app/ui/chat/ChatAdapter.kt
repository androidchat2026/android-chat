package com.androidchat.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.androidchat.app.data.local.entities.MessageEntity
import com.androidchat.app.data.local.entities.MessageType
import com.androidchat.app.databinding.ItemMessageReceivedBinding
import com.androidchat.app.databinding.ItemMessageSentBinding
import com.androidchat.app.databinding.ItemVoiceReceivedBinding
import com.androidchat.app.databinding.ItemVoiceSentBinding
import com.androidchat.app.utils.toFormattedTime
import com.androidchat.app.utils.toVoiceDuration
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter(
    private val onPlayVoice: (path: String) -> Unit
) : ListAdapter<MessageEntity, RecyclerView.ViewHolder>(DIFF) {

    private val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    companion object {
        private const val VIEW_TEXT_SENT = 0
        private const val VIEW_TEXT_RECEIVED = 1
        private const val VIEW_VOICE_SENT = 2
        private const val VIEW_VOICE_RECEIVED = 3

        private val DIFF = object : DiffUtil.ItemCallback<MessageEntity>() {
            override fun areItemsTheSame(a: MessageEntity, b: MessageEntity) = a.id == b.id
            override fun areContentsTheSame(a: MessageEntity, b: MessageEntity) = a == b
        }
    }

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position)
        val isMine = msg.senderId == myUid
        return when {
            msg.type == MessageType.TEXT && isMine -> VIEW_TEXT_SENT
            msg.type == MessageType.TEXT -> VIEW_TEXT_RECEIVED
            msg.type == MessageType.VOICE && isMine -> VIEW_VOICE_SENT
            else -> VIEW_VOICE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TEXT_SENT -> TextSentVH(ItemMessageSentBinding.inflate(inflater, parent, false))
            VIEW_TEXT_RECEIVED -> TextReceivedVH(ItemMessageReceivedBinding.inflate(inflater, parent, false))
            VIEW_VOICE_SENT -> VoiceSentVH(ItemVoiceSentBinding.inflate(inflater, parent, false))
            else -> VoiceReceivedVH(ItemVoiceReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is TextSentVH -> holder.bind(msg)
            is TextReceivedVH -> holder.bind(msg)
            is VoiceSentVH -> holder.bind(msg)
            is VoiceReceivedVH -> holder.bind(msg)
        }
    }

    inner class TextSentVH(private val b: ItemMessageSentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MessageEntity) {
            b.tvMessage.text = msg.text
            b.tvTime.text = msg.timestamp.toFormattedTime()
        }
    }

    inner class TextReceivedVH(private val b: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MessageEntity) {
            b.tvMessage.text = msg.text
            b.tvTime.text = msg.timestamp.toFormattedTime()
        }
    }

    inner class VoiceSentVH(private val b: ItemVoiceSentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MessageEntity) {
            b.tvDuration.text = msg.voiceDurationMs.toVoiceDuration()
            b.tvTime.text = msg.timestamp.toFormattedTime()
            b.btnPlay.setOnClickListener { msg.voiceNotePath?.let { onPlayVoice(it) } }
        }
    }

    inner class VoiceReceivedVH(private val b: ItemVoiceReceivedBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MessageEntity) {
            b.tvDuration.text = msg.voiceDurationMs.toVoiceDuration()
            b.tvTime.text = msg.timestamp.toFormattedTime()
            b.btnPlay.setOnClickListener { msg.voiceNotePath?.let { onPlayVoice(it) } }
        }
    }
}
