package com.example.campusmarketplace.chat

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap()
)
