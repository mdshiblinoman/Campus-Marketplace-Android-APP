package com.example.campusmarketplace.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }
    
    LaunchedEffect(partnerId) {
        viewModel.fetchUserName(partnerId) { name ->
            partnerName = name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(partnerName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
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
                    IconButton(onClick = {
                        viewModel.sendMessage(chatId, partnerId, messageText)
                        messageText = ""
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
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
            reverseLayout = false
        ) {
            items(messages) { message ->
                val isMine = message.senderId == currentUserId
                ChatBubble(message, isMine)
            }
        }
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
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = message.content)
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End),
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
