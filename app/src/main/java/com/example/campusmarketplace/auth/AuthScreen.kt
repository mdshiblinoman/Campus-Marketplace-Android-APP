package com.example.campusmarketplace.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val isSignUp = viewModel.isSignUpMode.value
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Sign Up" else "Sign In",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (isSignUp) {
            OutlinedTextField(
                value = viewModel.signUpFullName.value,
                onValueChange = { viewModel.signUpFullName.value = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.signUpMobile.value,
                onValueChange = { viewModel.signUpMobile.value = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = if (isSignUp) viewModel.signUpEmail.value else viewModel.loginEmail.value,
            onValueChange = { 
                if (isSignUp) viewModel.signUpEmail.value = it else viewModel.loginEmail.value = it 
            },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = viewModel.signUpDepartment.value,
                onValueChange = { viewModel.signUpDepartment.value = it },
                label = { Text("Department") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = if (isSignUp) viewModel.signUpPassword.value else viewModel.loginPassword.value,
            onValueChange = { 
                if (isSignUp) viewModel.signUpPassword.value = it else viewModel.loginPassword.value = it 
            },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Text(
                    text = if (passwordVisible) "Hide" else "Show",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { passwordVisible = !passwordVisible }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = viewModel.signUpConfirmPassword.value,
                onValueChange = { viewModel.signUpConfirmPassword.value = it },
                label = { Text("Confirm Password") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Text(
                        text = if (confirmPasswordVisible) "Hide" else "Show",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { confirmPasswordVisible = !confirmPasswordVisible }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        val error = if (isSignUp) viewModel.signUpError.value else viewModel.loginError.value
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (isSignUp) viewModel.onSignUpClick() else viewModel.onLoginClick() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) "Sign Up" else "Sign In")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text(if (isSignUp) "Already have an account? " else "Don't have an account? ")
            Text(
                text = if (isSignUp) "Sign In" else "Sign Up",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    viewModel.toggleAuthMode()
                }
            )
        }
    }
}
