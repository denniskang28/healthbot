package com.healthbot.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.healthbot.app.databinding.ItemChatMessageAssistantBinding
import com.healthbot.app.databinding.ItemChatMessageUserBinding

class ChatAdapter(private val messages: List<ChatbotActivity.ChatItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_USER = 0
        private const val VIEW_ASSISTANT = 1
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].role == "USER") VIEW_USER else VIEW_ASSISTANT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_USER) {
            UserViewHolder(ItemChatMessageUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            AssistantViewHolder(ItemChatMessageAssistantBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(item.content)
            is AssistantViewHolder -> holder.bind(item.content)
        }
    }

    override fun getItemCount() = messages.size

    inner class UserViewHolder(private val binding: ItemChatMessageUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(content: String) { binding.tvMessage.text = content }
    }

    inner class AssistantViewHolder(private val binding: ItemChatMessageAssistantBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(content: String) { binding.tvMessage.text = content }
    }
}
