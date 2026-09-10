package com.example.campusmarketplace.notifications

import com.google.firebase.firestore.FirebaseFirestore

object NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    fun notifyUser(
        recipientId: String,
        title: String,
        message: String,
        type: String = NotificationType.General,
        relatedId: String = "",
        relatedTitle: String = "",
        createdBy: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (recipientId.isBlank() || title.isBlank() || message.isBlank()) {
            onComplete(false)
            return
        }

        val notificationRef = db.collection("notifications").document()
        val notification = MarketplaceNotification(
            id = notificationRef.id,
            recipientId = recipientId,
            title = title.trim(),
            message = message.trim(),
            type = type,
            relatedId = relatedId,
            relatedTitle = relatedTitle,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis()
        )

        notificationRef.set(notification)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun notifyUsers(
        recipientIds: Iterable<String>,
        title: String,
        message: String,
        type: String = NotificationType.General,
        relatedId: String = "",
        relatedTitle: String = "",
        createdBy: String = ""
    ) {
        recipientIds
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { recipientId ->
                notifyUser(
                    recipientId = recipientId,
                    title = title,
                    message = message,
                    type = type,
                    relatedId = relatedId,
                    relatedTitle = relatedTitle,
                    createdBy = createdBy
                )
            }
    }
}
