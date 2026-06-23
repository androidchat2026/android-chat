package com.androidchat.app.ui.conversations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.androidchat.app.data.local.entities.ConversationEntity
import com.androidchat.app.databinding.ItemConversationBinding
import com.androidchat.app.utils.toFormattedTime
import com.bumptech.glide.Glide

class ConversationsAdapter(
    private val onClick: (ConversationEntity) -> Unit
) : ListAdapter<ConversationEntity, ConversationsAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemConversationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ConversationEntity) {
            b.tvName.text = item.participantName
            b.tvLastMessage.text = item.lastMessageText ?: ""
            b.tvTime.text = if (item.lastMessageTimestamp > 0) item.lastMessageTimestamp.toFormattedTime() else ""
            b.tvUnread.text = item.unreadCount.toString()
            b.tvUnread.visibility = if (item.unreadCount > 0) android.view.View.VISIBLE else android.view.View.GONE

            if (item.participantAvatarUrl != null) {
                Glide.with(b.root).load(item.participantAvatarUrl).circleCrop().into(b.ivAvatar)
            } else {
                b.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
            b.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ConversationEntity>() {
            override fun areItemsTheSame(a: ConversationEntity, b: ConversationEntity) = a.id == b.id
            override fun areContentsTheSame(a: ConversationEntity, b: ConversationEntity) = a == b
        }
    }
}
