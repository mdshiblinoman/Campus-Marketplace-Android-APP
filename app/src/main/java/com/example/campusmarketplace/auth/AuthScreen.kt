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
    val isLoading = viewModel.isAuthLoading.value
    var showForgotPassword by remember { mutableStateOf(false) }
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
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = if (isSignUp) viewModel.signUpEmail.value else viewModel.loginEmail.value,
            onValueChange = { 
                if (isSignUp) viewModel.signUpEmail.value = it else viewModel.loginEmail.value = it 
            },
            label = { Text("University Email") },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = viewModel.signUpStudentId.value,
                onValueChange = { viewModel.signUpStudentId.value = it },
                label = { Text("Student ID") },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.signUpDepartment.value,
                onValueChange = { viewModel.signUpDepartment.value = it },
                label = { Text("Department") },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.signUpMobile.value,
                onValueChange = { viewModel.signUpMobile.value = it },
                label = { Text("Phone Number") },
                enabled = !isLoading,
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
            enabled = !isLoading,
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
                enabled = !isLoading,
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

        viewModel.registrationSuccess.value?.let {
            Text(
                text = it,
                color = Color(0xFF388E3C),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (isSignUp) viewModel.onSignUpClick() else viewModel.onLoginClick() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                when {
                    isLoading && isSignUp -> "Creating account..."
                    isLoading -> "Signing in..."
                    isSignUp -> "Sign Up"
                    else -> "Sign In"
                }
            )
        }

        if (!isSignUp) {
            TextButton(
                onClick = { showForgotPassword = true },
                enabled = !isLoading
            ) {
                Text("Forgot password?")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text(if (isSignUp) "Already have an account? " else "Don't have an account? ")
            Text(
                text = if (isSignUp) "Sign In" else "Sign Up",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    if (!isLoading) {
                        viewModel.toggleAuthMode()
                    }
                }
            )
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            viewModel = viewModel,
            onDismiss = { showForgotPassword = false }
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset password") },
        text = {
            Column {
                OutlinedTextField(
                    value = viewModel.forgotPasswordEmail.value,
                    onValueChange = { viewModel.forgotPasswordEmail.value = it },
                    label = { Text("University email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                viewModel.forgotPasswordMessage.value?.let {
                    Text(
                        text = it,
                        color = if (it.startsWith("Password reset")) Color(0xFF388E3C) else Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.sendPasswordResetEmail() }) {
                Text("Send link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
