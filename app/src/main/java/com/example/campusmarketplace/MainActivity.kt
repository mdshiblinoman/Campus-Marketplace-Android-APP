package com.example.campusmarketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusmarketplace.auth.AuthScreen
import com.example.campusmarketplace.auth.AuthScreenState
import com.example.campusmarketplace.auth.AuthViewModel
import com.example.campusmarketplace.profile.ProfileScreen
import com.example.campusmarketplace.profile.ProfileViewModel
import com.example.campusmarketplace.ui.theme.CampusMarketplaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            CampusMarketplaceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (authViewModel.currentScreen.value) {
                            AuthScreenState.Auth -> AuthScreen(authViewModel)
                            AuthScreenState.Home -> Greeting(
                                name = "User",
                                onProfileClick = { authViewModel.navigateTo(AuthScreenState.Profile) },
                                modifier = Modifier.fillMaxSize(),
                            )
                            AuthScreenState.Profile -> {
                                val profileViewModel: ProfileViewModel = viewModel()
                                ProfileScreen(
                                    viewModel = profileViewModel,
                                    onBack = { authViewModel.navigateTo(AuthScreenState.Home) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String, onProfileClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.greeting_text, name),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onProfileClick) {
            Text("View Profile")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    CampusMarketplaceTheme {
        Greeting("Hello", onProfileClick = {})
    }
}