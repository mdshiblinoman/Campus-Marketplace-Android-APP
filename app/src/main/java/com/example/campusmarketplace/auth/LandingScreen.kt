package com.example.campusmarketplace.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LandingScreen(viewModel: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Campus Marketplace",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Your one-stop shop for campus essentials",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = { 
                viewModel.isSignUpMode.value = false
                viewModel.navigateTo(AuthScreenState.Auth) 
            },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Get Started")
        }
    }
}
