package com.example.campusmarketplace.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

enum class AuthScreenState {
    Landing, Auth, Home
}

data class User(
    val fullName: String,
    val mobileNumber: String,
    val email: String,
    val department: String,
    val password: String
)

class AuthViewModel : ViewModel() {
    // Navigation State
    private val _currentScreen = mutableStateOf(AuthScreenState.Landing)
    val currentScreen: State<AuthScreenState> = _currentScreen

    // Mode Toggle
    var isSignUpMode = mutableStateOf(false)

    // Mock Database
    private val _registeredUsers = mutableStateListOf<User>()

    // Login Form State
    var loginEmail = mutableStateOf("")
    var loginPassword = mutableStateOf("")
    var loginError = mutableStateOf<String?>(null)

    // Sign Up Form State
    var signUpFullName = mutableStateOf("")
    var signUpMobile = mutableStateOf("")
    var signUpEmail = mutableStateOf("")
    var signUpDepartment = mutableStateOf("")
    var signUpPassword = mutableStateOf("")
    var signUpConfirmPassword = mutableStateOf("")
    var signUpError = mutableStateOf<String?>(null)

    fun toggleAuthMode() {
        isSignUpMode.value = !isSignUpMode.value
        clearErrors()
    }

    fun navigateTo(screen: AuthScreenState) {
        _currentScreen.value = screen
        clearErrors()
    }

    private fun clearErrors() {
        loginError.value = null
        signUpError.value = null
    }

    fun onLoginClick() {
        val user = _registeredUsers.find { it.email == loginEmail.value && it.password == loginPassword.value }
        if (user != null) {
            _currentScreen.value = AuthScreenState.Home
        } else {
            loginError.value = "Invalid email or password"
        }
    }

    fun onSignUpClick() {
        if (signUpPassword.value != signUpConfirmPassword.value) {
            signUpError.value = "Passwords do not match"
            return
        }
        
        if (signUpEmail.value.isBlank() || signUpPassword.value.isBlank() || signUpFullName.value.isBlank()) {
            signUpError.value = "Please fill in all required fields"
            return
        }

        if (_registeredUsers.any { it.email == signUpEmail.value }) {
            signUpError.value = "User already exists with this email"
            return
        }

        val newUser = User(
            fullName = signUpFullName.value,
            mobileNumber = signUpMobile.value,
            email = signUpEmail.value,
            department = signUpDepartment.value,
            password = signUpPassword.value
        )
        
        _registeredUsers.add(newUser)
        // Reset Sign Up form
        resetSignUpForm()
        // Switch to Sign In mode after successful registration
        isSignUpMode.value = false
    }

    private fun resetSignUpForm() {
        signUpFullName.value = ""
        signUpMobile.value = ""
        signUpEmail.value = ""
        signUpDepartment.value = ""
        signUpPassword.value = ""
        signUpConfirmPassword.value = ""
    }
}
