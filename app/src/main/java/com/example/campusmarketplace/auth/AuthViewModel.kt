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

    // Sign In / Sign Up Mode
    var isSignUpMode = mutableStateOf(false)

    // -------------------------
    // Login Form State
    // -------------------------
    var loginEmail = mutableStateOf("")
    var loginPassword = mutableStateOf("")
    var loginError = mutableStateOf<String?>(null)

    // -------------------------
    // Sign Up Form State
    // -------------------------
    var signUpFullName = mutableStateOf("")
    var signUpMobile = mutableStateOf("")
    var signUpEmail = mutableStateOf("")
    var signUpDepartment = mutableStateOf("")
    var signUpPassword = mutableStateOf("")
    var signUpConfirmPassword = mutableStateOf("")

    var signUpError = mutableStateOf<String?>(null)

    // Registration success message
    var registrationSuccess = mutableStateOf<String?>(null)


    // ============================================================
    // INIT
    // ============================================================

    init {

        // Restore an existing session only after confirming its Firebase profile.
        auth.currentUser?.uid?.let { uid ->
            verifyFirebaseUser(uid) { verified ->
                if (verified) {
                    _currentScreen.value = AuthScreenState.Main
                } else {
                    auth.signOut()
                    loginError.value = "Your account profile was not found in Firebase."
                }
            }
        }

        // Automatically detect sign out / account deletion
        auth.addAuthStateListener { firebaseAuth ->

            if (firebaseAuth.currentUser == null) {

                // User is logged out
                _currentScreen.value = AuthScreenState.Auth
            }
        }
    }


    // ============================================================
    // TOGGLE SIGN IN / SIGN UP
    // ============================================================

    fun toggleAuthMode() {

        isSignUpMode.value = !isSignUpMode.value

        clearErrors()
    }


    // ============================================================
    // NAVIGATION
    // ============================================================

    fun navigateTo(screen: AuthScreenState) {

        _currentScreen.value = screen

        clearErrors()
    }


    // ============================================================
    // CLEAR ERRORS
    // ============================================================

    private fun clearErrors() {

        loginError.value = null
        signUpError.value = null
        registrationSuccess.value = null
    }


    // ============================================================
    // LOGIN
    // ============================================================

    private fun verifyFirebaseUser(uid: String, onResult: (Boolean) -> Unit) {
        realtimeDb
            .child("users")
            .child(uid)
            .get()
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful && task.result.exists())
            }
    }

    fun onLoginClick() {

        if (
            loginEmail.value.isBlank() ||
            loginPassword.value.isBlank()
        ) {

            loginError.value =
                "Email and password cannot be empty"

            return
        }

        loginError.value = null
        registrationSuccess.value = null

        auth.signInWithEmailAndPassword(
            loginEmail.value.trim(),
            loginPassword.value
        )
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val uid = auth.currentUser?.uid

                    if (uid != null) {

                        verifyFirebaseUser(uid) { verified ->
                                if (!verified) {
                                    auth.signOut()
                                    loginError.value = "Your account profile was not found in Firebase."
                                    return@verifyFirebaseUser
                                }

                                realtimeDb
                                    .child("users")
                                    .child(uid)
                                    .child("lastLogin")
                                    .setValue(System.currentTimeMillis())

                                _currentScreen.value = AuthScreenState.Main
                        }
                    } else {
                        loginError.value = "Unable to load your Firebase account."
                    }

                } else {

                    loginError.value =
                        task.exception?.message
                            ?: "Login failed"
                }
            }
    }


    // ============================================================
    // SIGN UP
    // ============================================================

    fun onSignUpClick() {

        // -------------------------
        // Validation
        // -------------------------

        if (signUpFullName.value.isBlank()) {

            signUpError.value =
                "Full name cannot be empty"

            return
        }

        if (signUpEmail.value.isBlank()) {

            signUpError.value =
                "Email cannot be empty"

            return
        }

        if (signUpPassword.value.isBlank()) {

            signUpError.value =
                "Password cannot be empty"

            return
        }

        if (
            signUpPassword.value !=
            signUpConfirmPassword.value
        ) {

            signUpError.value =
                "Passwords do not match"

            return
        }

        clearErrors()
        registrationSuccess.value = null


        // ========================================================
        // CREATE FIREBASE AUTH ACCOUNT
        // ========================================================

        auth.createUserWithEmailAndPassword(
            signUpEmail.value.trim(),
            signUpPassword.value
        )
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {

                    signUpError.value =
                        task.exception?.message
                            ?: "Registration failed"

                    return@addOnCompleteListener
                }


                // Firebase user
                val user = auth.currentUser

                if (user == null) {

                    signUpError.value =
                        "Unable to create user account"

                    return@addOnCompleteListener
                }


                // ====================================================
                // UPDATE FIREBASE AUTH PROFILE
                // ====================================================

                val profileUpdates =
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(
                            signUpFullName.value.trim()
                        )
                        .build()


                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { profileTask ->

                        if (!profileTask.isSuccessful) {

                            signUpError.value =
                                profileTask.exception?.message
                                    ?: "Profile update failed"

                            return@addOnCompleteListener
                        }


                        // =================================================
                        // USER DATA
                        // =================================================

                        val uid = user.uid

                        val firestoreUserData =
                            hashMapOf(

                                "fullName"
                                        to signUpFullName.value.trim(),

                                "mobile"
                                        to signUpMobile.value.trim(),

                                "department"
                                        to signUpDepartment.value.trim(),

                                "email"
                                        to signUpEmail.value.trim()
                            )


                        val realtimeUserData =
                            hashMapOf(

                                "uid" to uid,

                                "fullName"
                                        to signUpFullName.value.trim(),

                                "email"
                                        to signUpEmail.value.trim(),

                                "mobile"
                                        to signUpMobile.value.trim(),

                                "department"
                                        to signUpDepartment.value.trim(),

                                "registrationDate"
                                        to System.currentTimeMillis()
                            )


                        // =================================================
                        // SAVE TO FIRESTORE
                        // =================================================

                        db.collection("users")
                            .document(uid)
                            .set(firestoreUserData)

                            .addOnCompleteListener { firestoreTask ->

                                if (!firestoreTask.isSuccessful) {

                                    signUpError.value =
                                        firestoreTask.exception?.message
                                            ?: "Failed to save user data"

                                    return@addOnCompleteListener
                                }


                                // =================================================
                                // SAVE TO REALTIME DATABASE
                                // =================================================

                                realtimeDb
                                    .child("users")
                                    .child(uid)
                                    .setValue(realtimeUserData)

                                    .addOnCompleteListener { realtimeTask ->

                                        if (!realtimeTask.isSuccessful) {

                                            signUpError.value =
                                                realtimeTask.exception?.message
                                                    ?: "Failed to save user data"

                                            return@addOnCompleteListener
                                        }


                                        realtimeDb
                                            .child("users")
                                            .child(uid)
                                            .get()
                                            .addOnCompleteListener { verificationTask ->
                                                if (!verificationTask.isSuccessful || !verificationTask.result.exists()) {
                                                    signUpError.value = "Registration could not be verified in Firebase."
                                                    return@addOnCompleteListener
                                                }

                                                // Registration never signs the new user into the app.
                                                auth.signOut()
                                                resetSignUpForm()
                                                isSignUpMode.value = false
                                                registrationSuccess.value =
                                                    "Registration successful. Please sign in."
                                                _currentScreen.value = AuthScreenState.Auth
                                            }
                                    }
                            }
                    }
            }
    }


    // ============================================================
    // RESET SIGN UP FORM
    // ============================================================

    private fun resetSignUpForm() {

        signUpFullName.value = ""
        signUpMobile.value = ""
        signUpEmail.value = ""
        signUpDepartment.value = ""
        signUpPassword.value = ""
        signUpConfirmPassword.value = ""
    }


    // ============================================================
    // SIGN OUT
    // ============================================================

    fun signOut() {

        auth.signOut()

        _currentScreen.value =
            AuthScreenState.Auth

        isSignUpMode.value = false

        clearErrors()
    }
}
