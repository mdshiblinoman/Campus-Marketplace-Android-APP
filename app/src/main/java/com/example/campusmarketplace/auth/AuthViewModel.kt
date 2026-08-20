package com.example.campusmarketplace.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

enum class AuthScreenState {
    Landing, Auth, Home
}

class AuthViewModel : ViewModel() {
    // Firebase Auth
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Navigation State
    private val _currentScreen = mutableStateOf(AuthScreenState.Landing)
    val currentScreen: State<AuthScreenState> = _currentScreen

    // Mode Toggle
    var isSignUpMode = mutableStateOf(false)

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

    init {
        // Check if user is already logged in
        val user = auth.currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                _currentScreen.value = AuthScreenState.Home
            } else {
                auth.signOut()
                _currentScreen.value = AuthScreenState.Landing
            }
        }
    }

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
        if (loginEmail.value.isBlank() || loginPassword.value.isBlank()) {
            loginError.value = "Email and password cannot be empty"
            return
        }

        clearErrors()
        auth.signInWithEmailAndPassword(loginEmail.value, loginPassword.value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        _currentScreen.value = AuthScreenState.Home
                    } else {
                        auth.signOut()
                        loginError.value = "Please verify your email address. A verification link was sent to your inbox."
                    }
                } else {
                    loginError.value = task.exception?.message ?: "Login failed"
                }
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

        clearErrors()
        auth.createUserWithEmailAndPassword(signUpEmail.value, signUpPassword.value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(signUpFullName.value)
                        .build()
                    
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { verificationTask ->
                                        if (verificationTask.isSuccessful) {
                                            signUpError.value = "Verification email sent. Please check your inbox."
                                            isSignUpMode.value = false
                                            resetSignUpForm()
                                        } else {
                                            signUpError.value = verificationTask.exception?.message ?: "Failed to send verification email"
                                        }
                                    }
                            } else {
                                signUpError.value = updateTask.exception?.message ?: "Profile update failed"
                            }
                        }
                } else {
                    signUpError.value = task.exception?.message ?: "Registration failed"
                }
            }
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
