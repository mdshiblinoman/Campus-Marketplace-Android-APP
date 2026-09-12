package com.example.campusmarketplace.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    chatId: String,
    partnerId: String,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val messages = viewModel.messages
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var partnerName by remember { mutableStateOf("Chat") }
    val listState = rememberLazyListState()
    val chat = viewModel.activeChats.find { it.id == chatId }
    val isLoading = viewModel.isLoading.value
    val error = viewModel.error.value
    val hasBlockedPartner = viewModel.hasBlockedUser(partnerId)
    val isBlockedByPartner = viewModel.isBlockedByUser(partnerId)
    val canSendMessages = !hasBlockedPartner && !isBlockedByPartner
    var showBlockConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }

    DisposableEffect(chatId) {
        onDispose { viewModel.clearCurrentChat() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    
    LaunchedEffect(partnerId) {
        viewModel.fetchUserName(partnerId) { name ->
            partnerName = name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(partnerNameOrFallback(partnerName))
                        if (!chat?.productTitle.isNullOrBlank()) {
                            Text(
                                text = chat?.productTitle.orEmpty(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (hasBlockedPartner) {
                                viewModel.unblockUser(partnerId)
                            } else {
                                showBlockConfirm = true
                            }
                        },
                        enabled = partnerId.isNotBlank()
                    ) {
                        Icon(
                            imageVector = if (hasBlockedPartner) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = if (hasBlockedPartner) "Unblock user" else "Block user",
                            tint = if (hasBlockedPartner) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                if (canSendMessages) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 4
                        )
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(chatId, partnerId, messageText)
                                messageText = ""
                            },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasBlockedPartner) {
                                "You blocked this user."
                            } else {
                                "Messaging is unavailable with this user."
                            },
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        if (hasBlockedPartner) {
                            TextButton(onClick = { viewModel.unblockUser(partnerId) }) {
                                Text("Unblock")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            state = listState,
            reverseLayout = false
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> CircularProgressIndicator()
                            error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                            else -> Text("No messages yet", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(
                    items = messages,
                    key = { message -> message.id.ifBlank { "${message.senderId}_${message.timestamp}" } }
                ) { message ->
                    val isMine = message.senderId == currentUserId
                    ChatBubble(message, isMine)
                }
            }
        }
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block user?") },
            text = { Text("You will no longer be able to send messages to each other.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockUser(partnerId)
                        showBlockConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: Message, isMine: Boolean) {
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (isMine) 
        RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp) else 
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = color,
            contentColor = contentColor,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(text = message.content)
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    if (isMine) {
                        Text(
                            text = " - ${messageStatusLabel(message)}",
                            fontSize = 10.sp,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private fun partnerNameOrFallback(partnerName: String): String {
    return partnerName.ifBlank { "Chat" }
}

private fun messageStatusLabel(message: Message): String {
    return when {
        message.isRead || message.status == "read" -> "Read"
        else -> "Sent"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
