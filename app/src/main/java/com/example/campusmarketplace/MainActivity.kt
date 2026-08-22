package com.example.campusmarketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusmarketplace.auth.AuthScreen
import com.example.campusmarketplace.auth.AuthScreenState
import com.example.campusmarketplace.auth.AuthViewModel
import com.example.campusmarketplace.ui.main.MainScreen
import com.example.campusmarketplace.ui.theme.CampusMarketplaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            CampusMarketplaceTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (authViewModel.currentScreen.value) {
                        AuthScreenState.Auth -> AuthScreen(authViewModel)
                        AuthScreenState.Main -> MainScreen(authViewModel)
                    }
                }
            }
        }
    }
}
