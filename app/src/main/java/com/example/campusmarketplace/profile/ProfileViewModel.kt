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

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val user = auth.currentUser ?: return
        fullName.value = user.displayName ?: ""
        email.value = user.email ?: ""
        profileImageUrl.value = user.photoUrl?.toString()

        isFetchingData.value = true
        
        // Load from Realtime Database with error handling
        try {
            realtimeDb.child("users").child(user.uid).get().addOnCompleteListener { task ->
                isFetchingData.value = false
                if (task.isSuccessful && task.result.exists()) {
                    val snapshot = task.result
                    mobile.value = snapshot.child("mobile").value?.toString() ?: ""
                    department.value = snapshot.child("department").value?.toString() ?: ""
                }
            }
            
            // Still keep listener for live updates if possible
            realtimeDb.child("users").child(user.uid).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (snapshot.exists()) {
                        mobile.value = snapshot.child("mobile").value?.toString() ?: ""
                        department.value = snapshot.child("department").value?.toString() ?: ""
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    // Fail silently or log, but don't block UI
                }
            })
        } catch (e: Exception) {
            isFetchingData.value = false
        }

        // Also keep Firestore listener for backwards compatibility
        db.collection("users").document(user.uid).addSnapshotListener { document, e ->
            if (e != null) return@addSnapshotListener
            if (document != null && document.exists()) {
                if (mobile.value.isEmpty()) mobile.value = document.getString("mobile") ?: ""
                if (department.value.isEmpty()) department.value = document.getString("department") ?: ""
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val email = user.email ?: ""
        isLoading.value = true

        // 1. Add email to Blacklist FIRST
        val blacklistData = hashMapOf(
            "email" to email,
            "deletionDate" to System.currentTimeMillis()
        )
        db.collection("blacklisted_users").document(email).set(blacklistData)
            .addOnCompleteListener { blacklistTask ->
                // Proceed with deletion regardless of blacklist success (though it should succeed)
                
                // 2. Delete user's products and their images
                db.collection("products").whereEqualTo("ownerId", uid).get()
                    .addOnSuccessListener { snapshot ->
                        val batch = db.batch()
                        snapshot.documents.forEach { doc ->
                            batch.delete(doc.reference)
                            // Delete product image if exists
                            storage.reference.child("product_images/${doc.id}.jpg").delete()
                        }
                        batch.commit().addOnCompleteListener {
                            // 3. Delete from Firestore user collection
                            db.collection("users").document(uid).delete()
                                .addOnCompleteListener { firestoreTask ->
                                    // 4. Delete from Realtime Database
                                    realtimeDb.child("users").child(uid).removeValue()
                                        .addOnCompleteListener { realtimeTask ->
                                            // 5. Delete Profile Picture from Storage
                                            storage.reference.child("profile_pictures/$uid.jpg").delete()
                                                .addOnCompleteListener { storageTask ->
                                                    // 6. Delete Auth Account (Must be the last step)
                                                    user.delete()
                                                        .addOnCompleteListener { authTask ->
                                                            isLoading.value = false
                                                            if (authTask.isSuccessful) {
                                                                message.value = "Account permanently deleted and blocked"
                                                                onComplete(true)
                                                            } else {
                                                                message.value = authTask.exception?.message ?: "Account deletion failed"
                                                                onComplete(false)
                                                            }
                                                        }
                                                }
                                        }
                                }
                        }
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
