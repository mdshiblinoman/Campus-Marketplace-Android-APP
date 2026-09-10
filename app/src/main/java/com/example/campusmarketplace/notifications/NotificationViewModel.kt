package com.example.campusmarketplace.notifications

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.campusmarketplace.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var notifications = mutableStateListOf<MarketplaceNotification>()
    var unreadCount = mutableStateOf(0)
    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)

    private var notificationHelper: NotificationHelper? = null
    private var notificationsListener: ListenerRegistration? = null
    private var isFirstLoad = true

    init {
        loadNotifications()
    }

    fun initNotificationHelper(context: android.content.Context) {
        notificationHelper = NotificationHelper(context)
    }

    fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        isLoading.value = true
        error.value = null
        notificationsListener?.remove()
        notificationsListener = db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snapshot, exception ->
                isLoading.value = false
                if (exception != null) {
                    error.value = exception.message
                    return@addSnapshotListener
                }

                val loadedNotifications = snapshot
                    ?.toObjects(MarketplaceNotification::class.java)
                    .orEmpty()
                    .sortedByDescending { it.createdAt }

                notifications.clear()
                notifications.addAll(loadedNotifications)
                unreadCount.value = loadedNotifications.count { !it.isRead }

                if (snapshot != null && !isFirstLoad) {
                    snapshot.documentChanges
                        .filter { it.type == DocumentChange.Type.ADDED }
                        .mapNotNull { it.document.toObject(MarketplaceNotification::class.java) }
                        .filter { !it.isRead }
                        .forEach { notification ->
                            notificationHelper?.showNotification(
                                title = notification.title,
                                message = notification.message,
                                notificationId = notification.id.hashCode()
                            )
                        }
                }
                isFirstLoad = false
            }
    }

    fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        db.collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnFailureListener { error.value = it.message }
    }

    fun markAllAsRead() {
        val unreadNotifications = notifications.filter { !it.isRead }
        if (unreadNotifications.isEmpty()) return

        val batch = db.batch()
        unreadNotifications.forEach { notification ->
            batch.update(
                db.collection("notifications").document(notification.id),
                "isRead",
                true
            )
        }
        batch.commit().addOnFailureListener { error.value = it.message }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}
