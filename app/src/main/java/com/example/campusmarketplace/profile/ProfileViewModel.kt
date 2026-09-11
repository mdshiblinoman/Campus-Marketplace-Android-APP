package com.example.campusmarketplace.profile

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.campusmarketplace.products.ProductAvailabilityStatus
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
    var studentId = mutableStateOf("")
    var mobile = mutableStateOf("")
    var department = mutableStateOf("")
    var profileImageUrl = mutableStateOf<String?>(null)
    var emailVerified = mutableStateOf(false)
    var activeListingsCount = mutableStateOf(0)
    var soldProductsCount = mutableStateOf(0)
    
    var isLoading = mutableStateOf(false)
    var isFetchingData = mutableStateOf(false)
    var message = mutableStateOf<String?>(null)
    var isPasswordChangeSuccessful = mutableStateOf(false)

    private var realtimeListener: com.google.firebase.database.ValueEventListener? = null
    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var productStatsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadUserProfile()
    }

    fun resetState() {
        fullName.value = ""
        email.value = ""
        studentId.value = ""
        mobile.value = ""
        department.value = ""
        profileImageUrl.value = null
        emailVerified.value = false
        activeListingsCount.value = 0
        soldProductsCount.value = 0
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
        emailVerified.value = user.isEmailVerified

        isFetchingData.value = true
        
        // Load from Realtime Database with error handling
        try {
            val userRef = realtimeDb.child("users").child(user.uid)
            
            userRef.get().addOnCompleteListener { task ->
                isFetchingData.value = false
                if (task.isSuccessful && task.result.exists()) {
                    val snapshot = task.result
                    fullName.value = snapshot.child("fullName").value?.toString() ?: fullName.value
                    email.value = snapshot.child("email").value?.toString() ?: email.value
                    studentId.value = snapshot.child("studentId").value?.toString() ?: ""
                    mobile.value = snapshot.child("mobile").value?.toString() ?: ""
                    department.value = snapshot.child("department").value?.toString() ?: ""
                    profileImageUrl.value = snapshot.child("profileImageUrl").value?.toString()
                        ?: profileImageUrl.value
                }
            }
            
            // Still keep listener for live updates
            realtimeListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (snapshot.exists()) {
                        fullName.value = snapshot.child("fullName").value?.toString() ?: fullName.value
                        email.value = snapshot.child("email").value?.toString() ?: email.value
                        studentId.value = snapshot.child("studentId").value?.toString() ?: ""
                        mobile.value = snapshot.child("mobile").value?.toString() ?: ""
                        department.value = snapshot.child("department").value?.toString() ?: ""
                        profileImageUrl.value = snapshot.child("profileImageUrl").value?.toString()
                            ?: profileImageUrl.value
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
                if (fullName.value.isEmpty()) fullName.value = document.getString("fullName") ?: ""
                if (email.value.isEmpty()) email.value = document.getString("email") ?: ""
                if (studentId.value.isEmpty()) studentId.value = document.getString("studentId") ?: ""
                if (mobile.value.isEmpty()) mobile.value = document.getString("mobile") ?: ""
                if (department.value.isEmpty()) department.value = document.getString("department") ?: ""
                    if (profileImageUrl.value.isNullOrEmpty()) {
                    profileImageUrl.value = document.getString("profileImageUrl")
                }
            }
        }

        productStatsListener = db.collection("products")
            .whereEqualTo("ownerId", user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val documents = snapshot?.documents.orEmpty()
                activeListingsCount.value = documents.count { document ->
                    val status = normalizeAvailabilityStatus(
                        document.getString("availabilityStatus"),
                        document.getBoolean("isSold") == true || document.getBoolean("sold") == true
                    )
                    status == ProductAvailabilityStatus.Available || status == ProductAvailabilityStatus.Reserved
                }
                soldProductsCount.value = documents.count { document ->
                    normalizeAvailabilityStatus(
                        document.getString("availabilityStatus"),
                        document.getBoolean("isSold") == true || document.getBoolean("sold") == true
                    ) == ProductAvailabilityStatus.Sold
                }
            }
    }

    private fun normalizeAvailabilityStatus(status: String?, isSold: Boolean): String {
        if (isSold) return ProductAvailabilityStatus.Sold
        return when (status?.trim()?.lowercase()) {
            ProductAvailabilityStatus.Reserved -> ProductAvailabilityStatus.Reserved
            ProductAvailabilityStatus.Sold -> ProductAvailabilityStatus.Sold
            ProductAvailabilityStatus.Removed -> ProductAvailabilityStatus.Removed
            else -> ProductAvailabilityStatus.Available
        }
    }

    private fun cleanupListeners() {
        val user = auth.currentUser
        if (user != null && realtimeListener != null) {
            realtimeDb.child("users").child(user.uid).removeEventListener(realtimeListener!!)
        }
        firestoreListener?.remove()
        productStatsListener?.remove()
        realtimeListener = null
        firestoreListener = null
        productStatsListener = null
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

    fun updateProfile(onComplete: (Boolean) -> Unit = {}) {
        val user = auth.currentUser ?: return

        validateEditableProfile()?.let {
            message.value = it
            onComplete(false)
            return
        }

        isLoading.value = true
        val trimmedName = fullName.value.trim()
        val trimmedMobile = mobile.value.trim()
        val trimmedDepartment = department.value.trim()

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(trimmedName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userData = hashMapOf(
                        "fullName" to trimmedName,
                        "mobile" to trimmedMobile,
                        "department" to trimmedDepartment
                    )
                    
                    // Update Realtime Database
                    val realtimeUserData = mapOf(
                        "fullName" to trimmedName,
                        "mobile" to trimmedMobile,
                        "department" to trimmedDepartment
                    )
                    realtimeDb.child("users").child(user.uid).updateChildren(realtimeUserData)

                    db.collection("users").document(user.uid).set(
                        userData,
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                        .addOnSuccessListener {
                            isLoading.value = false
                            message.value = "Profile updated successfully"
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            isLoading.value = false
                            message.value = "Failed to update profile: ${it.message}"
                            onComplete(false)
                        }
                } else {
                    isLoading.value = false
                    message.value = task.exception?.message ?: "Update failed"
                    onComplete(false)
                }
            }
    }

    private fun validateEditableProfile(): String? {
        val trimmedName = fullName.value.trim()
        val trimmedMobile = mobile.value.trim()
        val trimmedDepartment = department.value.trim()

        return when {
            trimmedName.isBlank() -> "Name cannot be empty"
            trimmedName.length < 3 -> "Name must be at least 3 characters"
            trimmedMobile.isBlank() -> "Phone number cannot be empty"
            !trimmedMobile.matches(Regex("^\\+?[0-9 ()-]{7,20}$")) -> "Enter a valid phone number"
            trimmedDepartment.isBlank() -> "Department cannot be empty"
            else -> null
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
                                db.collection("users").document(user.uid).set(
                                    mapOf("profileImageUrl" to downloadUri.toString()),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                realtimeDb.child("users").child(user.uid).child("profileImageUrl")
                                    .setValue(downloadUri.toString())
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
