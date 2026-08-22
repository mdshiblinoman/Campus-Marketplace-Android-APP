package com.example.campusmarketplace.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var activeChats = mutableStateListOf<Chat>()
    var messages = mutableStateListOf<Message>()
    var currentChatPartnerId = mutableStateOf<String?>(null)
    
    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)

    init {
        loadActiveChats()
    }

    fun loadActiveChats() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("chats")
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    activeChats.clear()
                    activeChats.addAll(snapshot.toObjects(Chat::class.java))
                }
            }
    }

    fun startOrGetChat(partnerId: String, onResult: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val chatId = if (userId < partnerId) "${userId}_$partnerId" else "${partnerId}_$userId"
        
        db.collection("chats").document(chatId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val chat = Chat(
                        id = chatId,
                        participantIds = listOf(userId, partnerId),
                        lastMessage = "No messages yet",
                        lastMessageTimestamp = System.currentTimeMillis()
                    )
                    db.collection("chats").document(chatId).set(chat)
                }
                onResult(chatId)
            }
    }

    fun loadMessages(chatId: String) {
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
                    "lastMessageTimestamp", System.currentTimeMillis()
                )
            }
    }
}
