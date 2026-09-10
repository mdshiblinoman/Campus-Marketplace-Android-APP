package com.example.campusmarketplace.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

enum class AuthScreenState {
    Auth, Main, Chat
}

private data class FirebaseUserAccess(
    val profileExists: Boolean,
    val disabled: Boolean
)

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
    var forgotPasswordEmail = mutableStateOf("")
    var forgotPasswordMessage = mutableStateOf<String?>(null)

    // -------------------------
    // Sign Up Form State
    // -------------------------
    var signUpFullName = mutableStateOf("")
    var signUpStudentId = mutableStateOf("")
    var signUpMobile = mutableStateOf("")
    var signUpEmail = mutableStateOf("")
    var signUpDepartment = mutableStateOf("")
    var signUpPassword = mutableStateOf("")
    var signUpConfirmPassword = mutableStateOf("")

    var signUpError = mutableStateOf<String?>(null)
    var isAuthLoading = mutableStateOf(false)

    // Registration success message
    var registrationSuccess = mutableStateOf<String?>(null)


    // ============================================================
    // INIT
    // ============================================================

    init {

        // Restore an existing session only after confirming its Firebase profile.
        auth.currentUser?.uid?.let { uid ->
            restoreExistingSession(uid)
        }

        // Automatically detect sign out / account deletion
        auth.addAuthStateListener { firebaseAuth ->

            if (firebaseAuth.currentUser == null) {

                // User is logged out
                _currentScreen.value = AuthScreenState.Auth
                currentChatId.value = null
                currentChatPartnerId.value = null
                isAuthLoading.value = false
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

    private fun restoreExistingSession(uid: String) {
        isAuthLoading.value = true
        auth.currentUser?.reload()?.addOnCompleteListener {
            verifyFirebaseUser(uid) { access ->
                if (access.profileExists && !access.disabled && auth.currentUser?.isEmailVerified == true) {
                    isAuthLoading.value = false
                    _currentScreen.value = AuthScreenState.Main
                } else {
                    auth.signOut()
                    loginError.value = when {
                        !access.profileExists -> "Your account profile was not found in Firebase."
                        access.disabled -> "This account has been disabled by an administrator."
                        else -> "Please verify your university email before signing in."
                    }
                    isAuthLoading.value = false
                }
            }
        }
    }

    private fun verifyFirebaseUser(uid: String, onResult: (FirebaseUserAccess) -> Unit) {
        realtimeDb
            .child("users")
            .child(uid)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(FirebaseUserAccess(profileExists = false, disabled = false))
                    return@addOnCompleteListener
                }

                val snapshot = task.result
                onResult(
                    FirebaseUserAccess(
                        profileExists = snapshot.exists(),
                        disabled = snapshot.child("disabled").getValue(Boolean::class.java) == true
                    )
                )
            }
    }

    fun onLoginClick() {
        if (isAuthLoading.value) return

        validateLoginInputs()?.let { error ->
            loginError.value = error
            return
        }

        loginError.value = null
        registrationSuccess.value = null
        isAuthLoading.value = true

        auth.signInWithEmailAndPassword(
            loginEmail.value.trim(),
            loginPassword.value
        )
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val uid = auth.currentUser?.uid

                    if (uid != null) {

                        verifyFirebaseUser(uid) { access ->
                                if (!access.profileExists) {
                                    auth.signOut()
                                    loginError.value = "Your account profile was not found in Firebase."
                                    isAuthLoading.value = false
                                    return@verifyFirebaseUser
                                }

                                if (access.disabled) {
                                    auth.signOut()
                                    loginError.value = "This account has been disabled by an administrator."
                                    isAuthLoading.value = false
                                    return@verifyFirebaseUser
                                }

                                if (auth.currentUser?.isEmailVerified != true) {
                                    auth.currentUser?.sendEmailVerification()
                                    auth.signOut()
                                    loginError.value = "Please verify your university email. A new link was sent."
                                    isAuthLoading.value = false
                                    return@verifyFirebaseUser
                                }

                                updateLoginMetadata(uid)
                                loginPassword.value = ""
                                isAuthLoading.value = false
                                _currentScreen.value = AuthScreenState.Main
                        }
                    } else {
                        loginError.value = "Unable to load your Firebase account."
                        isAuthLoading.value = false
                    }

                } else {

                    loginError.value = getLoginErrorMessage(task.exception)
                    isAuthLoading.value = false
                }
            }
    }


    // ============================================================
    // SIGN UP
    // ============================================================

    fun onSignUpClick() {
        if (isAuthLoading.value) return

        validateSignUpInputs()?.let { error ->
            signUpError.value = error
            return
        }

        clearErrors()
        registrationSuccess.value = null
        isAuthLoading.value = true


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
                    isAuthLoading.value = false

                    return@addOnCompleteListener
                }


                // Firebase user
                val user = auth.currentUser

                if (user == null) {

                    signUpError.value =
                        "Unable to create user account"
                    isAuthLoading.value = false

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
                            isAuthLoading.value = false

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

                                "studentId"
                                        to signUpStudentId.value.trim(),

                                "department"
                                        to signUpDepartment.value.trim(),

                                "email"
                                        to signUpEmail.value.trim(),

                                "role" to "student",

                                "disabled" to false,

                                "registrationDate" to System.currentTimeMillis()
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

                                "studentId"
                                        to signUpStudentId.value.trim(),

                                "department"
                                        to signUpDepartment.value.trim(),

                                "role" to "student",

                                "disabled" to false,

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
                                    isAuthLoading.value = false

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
                                            isAuthLoading.value = false

                                            return@addOnCompleteListener
                                        }


                                        realtimeDb
                                            .child("users")
                                            .child(uid)
                                            .get()
                                            .addOnCompleteListener { verificationTask ->
                                                if (!verificationTask.isSuccessful || !verificationTask.result.exists()) {
                                                    signUpError.value = "Registration could not be verified in Firebase."
                                                    isAuthLoading.value = false
                                                    return@addOnCompleteListener
                                                }

                                                user.sendEmailVerification()
                                                    .addOnCompleteListener { verificationTask ->
                                                        // Registration never signs the new user into the app.
                                                        auth.signOut()
                                                        resetSignUpForm()
                                                        isSignUpMode.value = false
                                                        isAuthLoading.value = false
                                                        registrationSuccess.value = if (verificationTask.isSuccessful) {
                                                            "Registration successful. Check your university email to verify your account."
                                                        } else {
                                                            "Registration successful. Sign in to request a new verification link."
                                                        }
                                                        _currentScreen.value = AuthScreenState.Auth
                                                    }
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
        signUpStudentId.value = ""
        signUpMobile.value = ""
        signUpEmail.value = ""
        signUpDepartment.value = ""
        signUpPassword.value = ""
        signUpConfirmPassword.value = ""
    }

    private fun validateSignUpInputs(): String? {
        val fullName = signUpFullName.value.trim()
        val email = signUpEmail.value.trim()
        val studentId = signUpStudentId.value.trim()
        val department = signUpDepartment.value.trim()
        val phone = signUpMobile.value.trim()
        val password = signUpPassword.value

        return when {
            fullName.isBlank() -> "Full name cannot be empty"
            fullName.length < 3 -> "Full name must be at least 3 characters"
            email.isBlank() -> "University email cannot be empty"
            !isUniversityEmail(email) -> "Use a valid university email address."
            studentId.isBlank() -> "Student ID cannot be empty"
            !isValidStudentId(studentId) -> "Student ID can contain letters, numbers, hyphens, underscores, or slashes."
            department.isBlank() -> "Department cannot be empty"
            phone.isBlank() -> "Phone number cannot be empty"
            !isValidPhoneNumber(phone) -> "Enter a valid phone number."
            password.isBlank() -> "Password cannot be empty"
            !isValidPassword(password) -> "Password must be at least 8 characters and include a letter and a number."
            password != signUpConfirmPassword.value -> "Passwords do not match"
            else -> null
        }
    }

    private fun validateLoginInputs(): String? {
        val email = loginEmail.value.trim()
        val password = loginPassword.value

        return when {
            email.isBlank() -> "University email cannot be empty"
            !isUniversityEmail(email) -> "Use a valid university email address."
            password.isBlank() -> "Password cannot be empty"
            else -> null
        }
    }

    private fun updateLoginMetadata(uid: String) {
        val now = System.currentTimeMillis()
        realtimeDb.child("users").child(uid).child("lastLogin").setValue(now)
        db.collection("users").document(uid).set(
            mapOf("lastLogin" to now),
            SetOptions.merge()
        )
    }

    private fun getLoginErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "No account was found for this university email."
            is FirebaseAuthInvalidCredentialsException -> {
                if (exception.errorCode == "ERROR_WRONG_PASSWORD") {
                    "Incorrect password. Please try again."
                } else {
                    "Invalid email or password. Please check your details."
                }
            }
            is FirebaseTooManyRequestsException -> "Too many login attempts. Please wait and try again."
            is FirebaseNetworkException -> "Network error. Check your connection and try again."
            else -> exception?.message ?: "Login failed. Please try again."
        }
    }

    fun sendPasswordResetEmail() {
        val email = forgotPasswordEmail.value.trim()
        forgotPasswordMessage.value = null

        if (!isUniversityEmail(email)) {
            forgotPasswordMessage.value = "Use your university email address."
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                forgotPasswordMessage.value = if (task.isSuccessful) {
                    "Password reset instructions were sent to your email."
                } else {
                    task.exception?.message ?: "Unable to send password reset email."
                }
            }
    }

    private fun isUniversityEmail(email: String): Boolean {
        return email.trim().matches(
            Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(edu(?:\\.[A-Za-z]{2,})?|ac\\.[A-Za-z]{2,})$")
        )
    }

    private fun isValidStudentId(studentId: String): Boolean {
        return studentId.matches(Regex("^[A-Za-z0-9_/-]{3,30}$"))
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^\\+?[0-9 ()-]{7,20}$"))
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
    }


    // ============================================================
    // SIGN OUT
    // ============================================================

    fun signOut() {

        auth.signOut()

        _currentScreen.value =
            AuthScreenState.Auth

        isSignUpMode.value = false
        loginPassword.value = ""
        currentChatId.value = null
        currentChatPartnerId.value = null

        clearErrors()
        forgotPasswordMessage.value = null
        isAuthLoading.value = false
    }
}
