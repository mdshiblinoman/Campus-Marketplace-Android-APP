package com.example.campusmarketplace.profile

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    var fullName = mutableStateOf("")
    var email = mutableStateOf("")
    var mobile = mutableStateOf("")
    var department = mutableStateOf("")
    var profileImageUrl = mutableStateOf<String?>(null)
    
    var isLoading = mutableStateOf(false)
    var isFetchingData = mutableStateOf(false)
    var message = mutableStateOf<String?>(null)
    var isPasswordChangeSuccessful = mutableStateOf(false)

    private var realtimeListener: com.google.firebase.database.ValueEventListener? = null
    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadUserProfile()
    }

    fun resetState() {
        fullName.value = ""
        email.value = ""
        mobile.value = ""
        department.value = ""
        profileImageUrl.value = null
        message.value = null
    }

    fun loadUserProfile() {
        val user = auth.currentUser ?: return
        
        // Remove existing listeners before starting new ones to prevent overlaps
        cleanupListeners()
        resetState()

        fullName.value = user.displayName ?: ""
        email.value = user.email ?: ""
        profileImageUrl.value = user.photoUrl?.toString()

        isFetchingData.value = true
        
        // Load from Realtime Database with error handling
        try {
            val userRef = realtimeDb.child("users").child(user.uid)
            
            userRef.get().addOnCompleteListener { task ->
                isFetchingData.value = false
                if (task.isSuccessful && task.result.exists()) {
                    val snapshot = task.result
                    mobile.value = snapshot.child("mobile").value?.toString() ?: ""
                    department.value = snapshot.child("department").value?.toString() ?: ""
                }
            }
            
            // Still keep listener for live updates
            realtimeListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (snapshot.exists()) {
                        mobile.value = snapshot.child("mobile").value?.toString() ?: ""
                        department.value = snapshot.child("department").value?.toString() ?: ""
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            userRef.addValueEventListener(realtimeListener!!)
            
        } catch (e: Exception) {
            isFetchingData.value = false
        }

        // Also keep Firestore listener for backwards compatibility
        firestoreListener = db.collection("users").document(user.uid).addSnapshotListener { document, e ->
            if (e != null) return@addSnapshotListener
            if (document != null && document.exists()) {
                if (mobile.value.isEmpty()) mobile.value = document.getString("mobile") ?: ""
                if (department.value.isEmpty()) department.value = document.getString("department") ?: ""
            }
        }
    }

    private fun cleanupListeners() {
        val user = auth.currentUser
        if (user != null && realtimeListener != null) {
            realtimeDb.child("users").child(user.uid).removeEventListener(realtimeListener!!)
        }
        firestoreListener?.remove()
        realtimeListener = null
        firestoreListener = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }

    fun signOut() {
        auth.signOut()
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        isLoading.value = true
        message.value = "Deleting account and cleaning up data..."

        // 1. Delete user's products and their images
        db.collection("products").whereEqualTo("ownerId", uid).get(com.google.firebase.firestore.Source.SERVER)
            .addOnCompleteListener { productTask ->
                if (productTask.isSuccessful) {
                    val batch = db.batch()
                    productTask.result.documents.forEach { doc ->
                        batch.delete(doc.reference)
                        // Delete product image if exists (ignore failure if it doesn't)
                        storage.reference.child("product_images/${doc.id}.jpg").delete()
                    }
                    batch.commit().addOnCompleteListener { proceedToDeleteUser(uid, user, onComplete) }
                } else {
                    proceedToDeleteUser(uid, user, onComplete)
                }
            }
    }

    private fun proceedToDeleteUser(uid: String, user: com.google.firebase.auth.FirebaseUser, onComplete: (Boolean) -> Unit) {
        // 2. Delete from Firestore user collection
        db.collection("users").document(uid).delete().addOnCompleteListener {
            // 3. Delete from Realtime Database
            realtimeDb.child("users").child(uid).removeValue().addOnCompleteListener {
                // 4. Delete Profile Picture (ignore failure)
                storage.reference.child("profile_pictures/$uid.jpg").delete().addOnCompleteListener {
                    // 5. Delete all Chats involving the user
                    db.collection("chats").whereArrayContains("participantIds", uid).get(com.google.firebase.firestore.Source.SERVER)
                        .addOnCompleteListener { chatTask ->
                            if (chatTask.isSuccessful) {
                                val chatBatch = db.batch()
                                chatTask.result.documents.forEach { doc ->
                                    chatBatch.delete(doc.reference)
                                }
                                chatBatch.commit().addOnCompleteListener { finalizeAuthDeletion(user, onComplete) }
                            } else {
                                finalizeAuthDeletion(user, onComplete)
                            }
                        }
                }
            }
        }
    }

    private fun finalizeAuthDeletion(user: com.google.firebase.auth.FirebaseUser, onComplete: (Boolean) -> Unit) {
        // 7. Delete Auth Account (Must be the last step)
        user.delete().addOnCompleteListener { authTask ->
            isLoading.value = false
            if (authTask.isSuccessful) {
                message.value = "Account and all associated data permanently deleted"
                onComplete(true)
            } else {
                val exception = authTask.exception
                if (exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    message.value = "Security error: Please sign out and sign back in to delete your account."
                } else {
                    message.value = "Deletion failed: ${exception?.message}"
                }
                onComplete(false)
            }
        }
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
                    
                    // Update Realtime Database
                    val realtimeUserData = mapOf(
                        "fullName" to fullName.value,
                        "mobile" to mobile.value,
                        "department" to department.value
                    )
                    realtimeDb.child("users").child(user.uid).updateChildren(realtimeUserData)

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
