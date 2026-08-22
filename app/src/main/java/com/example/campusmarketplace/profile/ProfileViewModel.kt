package com.example.campusmarketplace.profile

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    var fullName = mutableStateOf("")
    var email = mutableStateOf("")
    var mobile = mutableStateOf("")
    var department = mutableStateOf("")
    var profileImageUrl = mutableStateOf<String?>(null)
    
    var isLoading = mutableStateOf(false)
    var isFetchingData = mutableStateOf(false)
    var message = mutableStateOf<String?>(null)
    var isPasswordChangeSuccessful = mutableStateOf(false)

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val user = auth.currentUser ?: return
        fullName.value = user.displayName ?: ""
        email.value = user.email ?: ""
        profileImageUrl.value = user.photoUrl?.toString()

        isFetchingData.value = true
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                isFetchingData.value = false
                if (document.exists()) {
                    mobile.value = document.getString("mobile") ?: ""
                    department.value = document.getString("department") ?: ""
                }
            }
            .addOnFailureListener {
                isFetchingData.value = false
                message.value = "Failed to load additional profile info"
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun updateProfile() {
        val user = auth.currentUser ?: return
        isLoading.value = true

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName.value)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userData = hashMapOf(
                        "mobile" to mobile.value,
                        "department" to department.value
                    )
                    db.collection("users").document(user.uid).set(userData)
                        .addOnSuccessListener {
                            isLoading.value = false
                            message.value = "Profile updated successfully"
                        }
                        .addOnFailureListener {
                            isLoading.value = false
                            message.value = "Failed to update additional info"
                        }
                } else {
                    isLoading.value = false
                    message.value = task.exception?.message ?: "Update failed"
                }
            }
    }

    fun uploadProfilePicture(uri: Uri) {
        val user = auth.currentUser ?: return
        isLoading.value = true
        val ref = storage.reference.child("profile_pictures/${user.uid}.jpg")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build()
                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { updateTask ->
                            isLoading.value = false
                            if (updateTask.isSuccessful) {
                                profileImageUrl.value = downloadUri.toString()
                                message.value = "Profile picture updated"
                            } else {
                                message.value = "Failed to update profile URI"
                            }
                        }
                } else {
                    isLoading.value = false
                    message.value = task.exception?.message ?: "Upload failed"
                }
            }
    }

    fun changePassword(newPassword: String) {
        val user = auth.currentUser ?: return
        isLoading.value = true
        
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    message.value = "Password changed successfully"
                    isPasswordChangeSuccessful.value = true
                } else {
                    message.value = task.exception?.message ?: "Password change failed"
                    isPasswordChangeSuccessful.value = false
                }
            }
    }
}
