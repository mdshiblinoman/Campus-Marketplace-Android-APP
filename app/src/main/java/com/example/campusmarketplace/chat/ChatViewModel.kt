package com.example.campusmarketplace.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
    private var notificationHelper: com.example.campusmarketplace.utils.NotificationHelper? = null
    private var lastChatUpdateTimes = mutableMapOf<String, Long>()
    private var isFirstLoad = true
    
    // User name cache to avoid repeated lookups
    private val userNameCache = mutableMapOf<String, String>()
    var userNames = mutableMapOf<String, String>() // Observable map for UI if needed, but a simple cache + callback is usually enough

    init {
        loadActiveChats()
    }

    fun initNotificationHelper(context: android.content.Context) {
        notificationHelper = com.example.campusmarketplace.utils.NotificationHelper(context)
    }

    fun loadActiveChats() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val newChats = snapshot.toObjects(Chat::class.java)
                    
                    if (!isFirstLoad) {
                        newChats.forEach { chat ->
                            val lastTime = lastChatUpdateTimes[chat.id] ?: 0L
                            if (chat.lastMessageTimestamp > lastTime && 
                                chat.lastSenderId != userId && 
                                chat.id != currentOpenChatId.value) {
                                
                                notificationHelper?.showNotification(
                                    "New Message",
                                    chat.lastMessage
                                )
                            }
                            lastChatUpdateTimes[chat.id] = chat.lastMessageTimestamp
                        }
                    } else {
                        newChats.forEach { lastChatUpdateTimes[it.id] = it.lastMessageTimestamp }
                        isFirstLoad = false
                    }

                    activeChats.clear()
                    activeChats.addAll(newChats.sortedByDescending { it.lastMessageTimestamp })
                }
            }
    }

    fun startOrGetChat(partnerId: String, onResult: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        if (partnerId.isEmpty()) return
        
        val chatId = if (userId < partnerId) "${userId}_$partnerId" else "${partnerId}_$userId"
        
        // Immediate navigation callback
        onResult(chatId)
        
        // Background creation check
        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val chat = Chat(
                        id = chatId,
                        participantIds = listOf(userId, partnerId),
                        lastMessage = "No messages yet",
                        lastMessageTimestamp = System.currentTimeMillis(),
                        lastSenderId = ""
                    )
                    db.collection("chats").document(chatId).set(chat)
                }
            }
            .addOnFailureListener {
                // If get fails (e.g. offline/no DB), we still attempt to create it
                // Firestore will sync it later if it's just a connection issue
                val chat = Chat(
                    id = chatId,
                    participantIds = listOf(userId, partnerId),
                    lastMessage = "No messages yet",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    lastSenderId = ""
                )
                db.collection("chats").document(chatId).set(chat)
            }
    }

    fun loadMessages(chatId: String) {
        currentOpenChatId.value = chatId
        messages.clear()
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    messages.clear()
                    messages.addAll(snapshot.toObjects(Message::class.java))
                }
            }
    }

    fun clearCurrentChat() {
        currentOpenChatId.value = null
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
                onResult(name)
            }
            .addOnFailureListener {
                onResult("User ${uid.take(5)}")
            }
    }

    fun sendMessage(chatId: String, partnerId: String, content: String) {
        val userId = auth.currentUser?.uid ?: return
        if (content.isBlank()) return

        val messageId = db.collection("chats").document(chatId).collection("messages").document().id
        val message = Message(
            id = messageId,
            senderId = userId,
            receiverId = partnerId,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        // Add message
        db.collection("chats").document(chatId).collection("messages").document(messageId).set(message)
            .addOnSuccessListener {
                // Update chat metadata
                db.collection("chats").document(chatId).update(
                    "lastMessage", content,
                    "lastMessageTimestamp", System.currentTimeMillis(),
                    "lastSenderId", userId
                )
            }
    }
}
