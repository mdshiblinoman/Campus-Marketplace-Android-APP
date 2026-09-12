package com.example.campusmarketplace

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusmarketplace.auth.AuthScreen
import com.example.campusmarketplace.auth.AuthScreenState
import com.example.campusmarketplace.auth.AuthViewModel
import com.example.campusmarketplace.chat.ChatScreen
import com.example.campusmarketplace.chat.ChatViewModel
import com.example.campusmarketplace.notifications.NotificationViewModel
import com.example.campusmarketplace.ui.main.MainScreen
import com.example.campusmarketplace.ui.theme.CampusMarketplaceTheme
import com.example.campusmarketplace.utils.DatabaseUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val currentUserId = authViewModel.currentScreen.value.let { 
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            }
            val chatViewModel: ChatViewModel = viewModel(key = currentUserId)
            val notificationViewModel: NotificationViewModel = viewModel(key = "notifications_$currentUserId")
            val context = androidx.compose.ui.platform.LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}

            // MASTER WIPE TRIGGER: Run once to clear entire database.
            // After the app signs you out, REMOVE this block immediately.
            LaunchedEffect(Unit) {
                DatabaseUtils.wipeAllData { success ->
                    Log.d("MainActivity", "Database wipe finished: $success")
                }
            }

            LaunchedEffect(currentUserId) {
                if (currentUserId != "anonymous") {
                    chatViewModel.initNotificationHelper(context)
                    notificationViewModel.initNotificationHelper(context)
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            
            CampusMarketplaceTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (authViewModel.currentScreen.value) {
                        AuthScreenState.Auth -> AuthScreen(authViewModel)
                        AuthScreenState.Main -> MainScreen(authViewModel, notificationViewModel)
                        AuthScreenState.Chat -> {
                            val chatId = authViewModel.currentChatId.value
                            val partnerId = authViewModel.currentChatPartnerId.value
                            if (chatId != null && partnerId != null) {
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    chatId = chatId,
                                    partnerId = partnerId,
                                    onBack = { authViewModel.navigateTo(AuthScreenState.Main) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
