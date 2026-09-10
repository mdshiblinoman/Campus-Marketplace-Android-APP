package com.example.campusmarketplace.chat

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val status: String = "sent",
    val readAt: Long = 0L
)

data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val buyerId: String = "",
    val sellerId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Map<String, Long> = emptyMap(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
