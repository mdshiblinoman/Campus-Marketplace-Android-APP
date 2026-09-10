package com.example.campusmarketplace.admin

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.campusmarketplace.products.Product
import com.example.campusmarketplace.products.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class AdminViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    var isAdmin = mutableStateOf(false)
    var users = mutableStateListOf<MarketplaceUser>()
    var reports = mutableStateListOf<Report>()
    var listings = mutableStateListOf<Product>()
    var isLoading = mutableStateOf(false)
    var message = mutableStateOf<String?>(null)

    private var usersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var reportsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var listingsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        checkAdminStatus()
    }

    fun checkAdminStatus() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role") ?: "student"
                isAdmin.value = role == "admin"
                if (isAdmin.value) {
                    loadAdminData()
                }
            }
            .addOnFailureListener { message.value = it.message }
    }

    private fun loadAdminData() {
        isLoading.value = true
        usersListener?.remove()
        reportsListener?.remove()
        listingsListener?.remove()

        usersListener = db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) {
                message.value = error.message
                return@addSnapshotListener
            }
            users.clear()
            snapshot?.documents
                ?.map { document ->
                    MarketplaceUser(
                        uid = document.id,
                        fullName = document.getString("fullName") ?: "",
                        email = document.getString("email") ?: "",
                        studentId = document.getString("studentId") ?: "",
                        mobile = document.getString("mobile") ?: "",
                        department = document.getString("department") ?: "",
                        role = document.getString("role") ?: "student",
                        disabled = document.getBoolean("disabled") ?: false,
                        registrationDate = document.getLong("registrationDate") ?: 0L,
                        lastLogin = document.getLong("lastLogin") ?: 0L
                    )
                }
                ?.sortedBy { it.fullName.lowercase() }
                ?.let { users.addAll(it) }
            isLoading.value = false
        }

        reportsListener = db.collection("reports").addSnapshotListener { snapshot, error ->
            if (error != null) {
                message.value = error.message
                return@addSnapshotListener
            }
            reports.clear()
            snapshot?.toObjects(Report::class.java)
                ?.sortedWith(compareBy<Report> { it.status != "pending" }.thenByDescending { it.timestamp })
                ?.let { reports.addAll(it) }
        }

        listingsListener = db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null) {
                message.value = error.message
                return@addSnapshotListener
            }
            listings.clear()
            snapshot?.toObjects(Product::class.java)
                ?.sortedByDescending { it.createdAt }
                ?.let { listings.addAll(it) }
        }
    }

    fun removeListing(productId: String) {
        if (!isAdmin.value || productId.isBlank()) return
        db.collection("products").document(productId).delete()
            .addOnSuccessListener { message.value = "Listing removed" }
            .addOnFailureListener { message.value = it.message }
    }

    fun dismissReport(reportId: String) {
        if (!isAdmin.value || reportId.isBlank()) return
        db.collection("reports").document(reportId).update("status", "dismissed")
            .addOnSuccessListener { message.value = "Report dismissed" }
            .addOnFailureListener { message.value = it.message }
    }

    fun markReportReviewed(reportId: String) {
        if (!isAdmin.value || reportId.isBlank()) return
        db.collection("reports").document(reportId).update("status", "reviewed")
            .addOnSuccessListener { message.value = "Report reviewed" }
            .addOnFailureListener { message.value = it.message }
    }

    fun setUserDisabled(userId: String, disabled: Boolean) {
        if (!isAdmin.value || userId.isBlank() || userId == auth.currentUser?.uid) return
        db.collection("users").document(userId).update("disabled", disabled)
            .addOnSuccessListener {
                realtimeDb.child("users").child(userId).child("disabled").setValue(disabled)
                message.value = if (disabled) "User disabled" else "User enabled"
            }
            .addOnFailureListener { message.value = it.message }
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
        reportsListener?.remove()
        listingsListener?.remove()
    }
}
