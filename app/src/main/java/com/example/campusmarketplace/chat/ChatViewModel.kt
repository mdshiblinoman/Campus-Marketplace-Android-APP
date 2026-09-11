package com.example.campusmarketplace.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.campusmarketplace.notifications.NotificationRepository
import com.example.campusmarketplace.notifications.NotificationType
import com.example.campusmarketplace.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    var activeChats = mutableStateListOf<Chat>()
    var messages = mutableStateListOf<Message>()
    var currentChatPartnerId = mutableStateOf<String?>(null)
    
    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)

    var currentOpenChatId = mutableStateOf<String?>(null)
    
    // User name cache to avoid repeated lookups
    private val userNameCache = mutableMapOf<String, String>()
    var userNames = mutableStateMapOf<String, String>()

    private var activeChatsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var messagesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var notificationHelper: NotificationHelper? = null
    private var isFirstChatsLoad = true

    init {
        loadActiveChats()
    }

    fun loadActiveChats() {
        val userId = auth.currentUser?.uid ?: return
        isLoading.value = true
        error.value = null
        activeChatsListener?.remove()
        activeChatsListener = db.collection("chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, e ->
                isLoading.value = false
                if (e != null) {
                    error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val newChats = snapshot.documents.mapNotNull { document ->
                        document.toObject(Chat::class.java)
                            ?.copy(id = document.getString("id")?.takeIf { it.isNotBlank() } ?: document.id)
                    }
                    activeChats.clear()
                    activeChats.addAll(newChats.sortedByDescending { it.lastMessageTimestamp })
                    if (!isFirstChatsLoad) {
                        showIncomingChatNotifications(snapshot.documentChanges, userId)
                    }
                    isFirstChatsLoad = false
                }
            }
    }

    fun initNotificationHelper(context: android.content.Context) {
        notificationHelper = NotificationHelper(context)
    }

    fun startOrGetChat(
        partnerId: String,
        productId: String = "",
        productTitle: String = "",
        onResult: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        if (partnerId.isEmpty()) return
        error.value = null
        
        val participantKey = if (userId < partnerId) "${userId}_$partnerId" else "${partnerId}_$userId"
        val chatId = if (productId.isBlank()) participantKey else "${participantKey}_$productId"

        val chatRef = db.collection("chats").document(chatId)
        chatRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val now = System.currentTimeMillis()
                    val chat = Chat(
                        id = chatId,
                        participantIds = listOf(userId, partnerId),
                        buyerId = userId,
                        sellerId = partnerId,
                        productId = productId,
                        productTitle = productTitle,
                        lastMessage = "No messages yet",
                        lastMessageTimestamp = now,
                        lastSenderId = "",
                        unreadCount = mapOf(userId to 0L, partnerId to 0L),
                        createdAt = now,
                        updatedAt = now
                    )
                    chatRef.set(chat)
                        .addOnSuccessListener { onResult(chatId) }
                        .addOnFailureListener {
                            error.value = "Failed to start chat: ${it.message}"
                        }
                } else if (productId.isNotBlank() || productTitle.isNotBlank()) {
                    chatRef.set(
                        mapOf(
                            "productId" to productId,
                            "productTitle" to productTitle,
                            "buyerId" to (doc.getString("buyerId") ?: userId),
                            "sellerId" to (doc.getString("sellerId") ?: partnerId),
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).addOnCompleteListener { onResult(chatId) }
                } else {
                    onResult(chatId)
                }
            }
            .addOnFailureListener {
                // If get fails (e.g. offline/no DB), we still attempt to create it
                // Firestore will sync it later if it's just a connection issue
                val now = System.currentTimeMillis()
                val chat = Chat(
                    id = chatId,
                    participantIds = listOf(userId, partnerId),
                    buyerId = userId,
                    sellerId = partnerId,
                    productId = productId,
                    productTitle = productTitle,
                    lastMessage = "No messages yet",
                    lastMessageTimestamp = now,
                    lastSenderId = "",
                    unreadCount = mapOf(userId to 0L, partnerId to 0L),
                    createdAt = now,
                    updatedAt = now
                )
                chatRef.set(chat)
                    .addOnSuccessListener { onResult(chatId) }
                    .addOnFailureListener { setError ->
                        error.value = "Failed to start chat: ${setError.message}"
                    }
            }
    }

    fun loadMessages(chatId: String) {
        currentOpenChatId.value = chatId
        isLoading.value = true
        error.value = null
        messages.clear()
        messagesListener?.remove()
        messagesListener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                isLoading.value = false
                if (e != null) {
                    error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    messages.clear()
                    val loadedMessages = snapshot.documents.mapNotNull { document ->
                        document.toObject(Message::class.java)
                            ?.copy(id = document.getString("id")?.takeIf { it.isNotBlank() } ?: document.id)
                    }
                    messages.addAll(loadedMessages)
                    markIncomingMessagesRead(chatId, snapshot.documents)
                }
            }
    }

    fun clearCurrentChat() {
        currentOpenChatId.value = null
        messagesListener?.remove()
        messagesListener = null
    }

    fun fetchUserName(uid: String, onResult: (String) -> Unit) {
        if (uid.isEmpty()) {
            onResult("Unknown User")
            return
        }
        
        // Return from cache if available
        userNameCache[uid]?.let {
            onResult(it)
            return
        }

        // Fetch from Realtime Database
        realtimeDb.child("users").child(uid).child("fullName").get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.value?.toString() ?: "User ${uid.take(5)}"
                userNameCache[uid] = name
                userNames[uid] = name
                onResult(name)
            }
            .addOnFailureListener {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val name = document.getString("fullName")?.takeIf { it.isNotBlank() }
                            ?: "User ${uid.take(5)}"
                        userNameCache[uid] = name
                        userNames[uid] = name
                        onResult(name)
                    }
                    .addOnFailureListener {
                        val name = "User ${uid.take(5)}"
                        userNameCache[uid] = name
                        userNames[uid] = name
                        onResult(name)
                    }
            }
    }

    fun sendMessage(chatId: String, partnerId: String, content: String) {
        val userId = auth.currentUser?.uid ?: return
        if (content.isBlank()) return

        val now = System.currentTimeMillis()
        val chatRef = db.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document()
        val messageId = messageRef.id
        val message = Message(
            id = messageId,
            senderId = userId,
            receiverId = partnerId,
            content = content.trim(),
            timestamp = now,
            isRead = false,
            status = "sent"
        )

        val batch = db.batch()
        batch.set(messageRef, message)
        batch.set(
            chatRef,
            mapOf(
                "id" to chatId,
                "participantIds" to listOf(userId, partnerId),
                "lastMessage" to content.trim(),
                "lastMessageTimestamp" to now,
                "lastSenderId" to userId,
                "updatedAt" to now,
                "unreadCount" to mapOf(
                    userId to 0L,
                    partnerId to FieldValue.increment(1)
                )
            ),
            SetOptions.merge()
        )

        batch.commit()
            .addOnSuccessListener {
                fetchUserName(userId) { senderName ->
                    NotificationRepository.notifyUser(
                        recipientId = partnerId,
                        title = "New Message",
                        message = "$senderName sent you a message: \"${content.trim()}\"",
                        type = NotificationType.ChatMessage,
                        relatedId = chatId,
                        relatedTitle = content.trim(),
                        createdBy = userId
                    )
                }
            }
            .addOnFailureListener {
                error.value = "Failed to send message: ${it.message}"
            }
    }

    private fun markIncomingMessagesRead(
        chatId: String,
        documents: List<com.google.firebase.firestore.DocumentSnapshot>
    ) {
        val userId = auth.currentUser?.uid ?: return
        val unreadDocuments = documents.filter { document ->
            document.getString("receiverId") == userId &&
                document.getBoolean("isRead") != true
        }

        if (unreadDocuments.isEmpty()) return

        val now = System.currentTimeMillis()
        val batch = db.batch()
        unreadDocuments.forEach { document ->
            batch.update(
                document.reference,
                mapOf(
                    "isRead" to true,
                    "status" to "read",
                    "readAt" to now
                )
            )
        }
        batch.set(
            db.collection("chats").document(chatId),
            mapOf("unreadCount" to mapOf(userId to 0L)),
            SetOptions.merge()
        )
        batch.commit().addOnFailureListener {
            error.value = "Failed to update message status: ${it.message}"
        }
    }

    private fun showIncomingChatNotifications(
        changes: List<DocumentChange>,
        currentUserId: String
    ) {
        changes
            .filter { it.type == DocumentChange.Type.ADDED || it.type == DocumentChange.Type.MODIFIED }
            .mapNotNull { change ->
                change.document.toObject(Chat::class.java)?.copy(
                    id = change.document.getString("id")?.takeIf { it.isNotBlank() } ?: change.document.id
                )
            }
            .filter { chat ->
                chat.lastSenderId.isNotBlank() &&
                    chat.lastSenderId != currentUserId &&
                    chat.id != currentOpenChatId.value &&
                    (chat.unreadCount[currentUserId] ?: 0L) > 0
            }
            .forEach { chat ->
                val senderId = chat.lastSenderId
                fetchUserName(senderId) { senderName ->
                    val title = if (chat.productTitle.isBlank()) {
                        "New message from $senderName"
                    } else {
                        "$senderName about ${chat.productTitle}"
                    }
                    notificationHelper?.showNotification(
                        title = title,
                        message = chat.lastMessage.ifBlank { "Sent you a message" },
                        notificationId = chat.id.hashCode()
                    )
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        activeChatsListener?.remove()
        messagesListener?.remove()
    }
}
