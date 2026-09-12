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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val isSignUp = viewModel.isSignUpMode.value
    val isLoading = viewModel.isAuthLoading.value
    var showForgotPassword by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    if (showForgotPassword) {
        ForgotPasswordScreen(
            viewModel = viewModel,
            onBack = {
                viewModel.clearForgotPasswordState()
                showForgotPassword = false
            }
        )
        return
    }
    
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
                onValueChange = { input ->
                    // Auto-capitalize words
                    val capitalized = input.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                    viewModel.signUpFullName.value = capitalized
                },
                label = { Text("Full Name") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
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
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                onValueChange = { input ->
                    // Limit to 11 digits
                    if (input.length <= 11 && input.all { it.isDigit() }) {
                        viewModel.signUpMobile.value = input
                    }
                },
                label = { Text("Phone Number") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                onClick = {
                    viewModel.prepareForgotPassword()
                    showForgotPassword = true
                },
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
}

@Composable
private fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val isLoading = viewModel.isPasswordResetLoading.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Forgot Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Enter your email to receive a password reset link.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = viewModel.forgotPasswordEmail.value,
            onValueChange = {
                viewModel.forgotPasswordEmail.value = it
                viewModel.clearForgotPasswordState()
            },
            label = { Text("Email Address") },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        viewModel.forgotPasswordMessage.value?.let {
            Text(
                text = it,
                color = if (viewModel.forgotPasswordSuccess.value) Color(0xFF388E3C) else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.sendPasswordResetEmail() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Sending link..." else "Send Reset Link")
        }

        TextButton(
            onClick = onBack,
            enabled = !isLoading
        ) {
            Text("Back to Sign In")
        }
    }
}
