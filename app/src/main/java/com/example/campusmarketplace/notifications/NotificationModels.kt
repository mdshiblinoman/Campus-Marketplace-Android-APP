package com.example.campusmarketplace.notifications

data class MarketplaceNotification(
    val id: String = "",
    val recipientId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = NotificationType.General,
    val relatedId: String = "",
    val relatedTitle: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

object NotificationType {
    const val General = "general"
    const val ChatMessage = "chat_message"
    const val ProductSold = "product_sold"
    const val ProductStatus = "product_status"
    const val Admin = "admin"
}
