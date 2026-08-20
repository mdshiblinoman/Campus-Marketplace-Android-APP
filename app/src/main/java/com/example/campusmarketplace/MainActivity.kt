package com.example.campusmarketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusmarketplace.auth.AuthScreen
import com.example.campusmarketplace.auth.AuthScreenState
import com.example.campusmarketplace.auth.AuthViewModel
import com.example.campusmarketplace.auth.LandingScreen
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
                            AuthScreenState.Landing -> LandingScreen(authViewModel)
                            AuthScreenState.Auth -> AuthScreen(authViewModel)
                            AuthScreenState.Home -> Greeting(
                                name = "User",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.greeting_text, name),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    CampusMarketplaceTheme {
        Greeting("Hello")
    }
}