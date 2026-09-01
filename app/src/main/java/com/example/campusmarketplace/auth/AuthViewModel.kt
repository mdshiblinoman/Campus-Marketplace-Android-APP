package com.example.campusmarketplace.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

enum class AuthScreenState {
    Auth, Main, Chat
}

class AuthViewModel : ViewModel() {
    // Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    // Navigation State
    private val _currentScreen = mutableStateOf(AuthScreenState.Auth)
    val currentScreen: State<AuthScreenState> = _currentScreen

    var currentChatId = mutableStateOf<String?>(null)
    var currentChatPartnerId = mutableStateOf<String?>(null)

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
    var registrationSuccess = mutableStateOf<String?>(null)

    init {
        // Check if user is already logged in
        if (auth.currentUser != null) {
            _currentScreen.value = AuthScreenState.Main
        }

        // Add Auth State Listener to automatically handle sign-outs or account deletions
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                // If user is not found, automatically go back to Login page
                _currentScreen.value = AuthScreenState.Auth
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
        registrationSuccess.value = null
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
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        // Log login information to Realtime Database
                        realtimeDb.child("users").child(uid).child("lastLogin")
                            .setValue(System.currentTimeMillis())
                    }
                    _currentScreen.value = AuthScreenState.Main
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
        
        // Proceed with registration directly
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
                                // Save additional info to Firestore
                                val userData = hashMapOf(
                                    "mobile" to signUpMobile.value,
                                    "department" to signUpDepartment.value,
                                    "email" to signUpEmail.value // Added email for easy reference
                                )
                                // Also save to Realtime Database
                                val realtimeUserData = mapOf(
                                    "fullName" to signUpFullName.value,
                                    "email" to signUpEmail.value,
                                    "mobile" to signUpMobile.value,
                                    "department" to signUpDepartment.value,
                                    "registrationDate" to System.currentTimeMillis()
                                )

                                user.uid.let { uid ->
                                    // Save to Realtime Database
                                    realtimeDb.child("users").child(uid).setValue(realtimeUserData)

                                    db.collection("users").document(uid).set(userData)
                                        .addOnCompleteListener { firestoreTask ->
                                            if (firestoreTask.isSuccessful) {
                                                auth.signOut()
                                                resetSignUpForm()
                                                isSignUpMode.value = false
                                                registrationSuccess.value = "Your registration has been completed."
                                            } else {
                                                signUpError.value = "Failed to save user data: ${firestoreTask.exception?.message}"
                                            }
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

    fun signOut() {
        auth.signOut()
        _currentScreen.value = AuthScreenState.Auth
    }
}
