package com.example.campusmarketplace.admin

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.campusmarketplace.notifications.NotificationRepository
import com.example.campusmarketplace.notifications.NotificationType
import com.example.campusmarketplace.products.Product
import com.example.campusmarketplace.products.ProductApprovalStatus
import com.example.campusmarketplace.products.ProductAvailabilityStatus
import com.example.campusmarketplace.products.ProductCondition
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
            snapshot?.documents
                ?.mapNotNull { document ->
                    document.toObject(Report::class.java)
                        ?.copy(id = document.getString("id")?.takeIf { it.isNotBlank() } ?: document.id)
                }
                ?.sortedWith(compareBy<Report> { reportStatusRank(it.status) }.thenByDescending { it.timestamp })
                ?.let { reports.addAll(it) }
        }

        listingsListener = db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null) {
                message.value = error.message
                return@addSnapshotListener
            }
            listings.clear()
            snapshot?.documents
                ?.mapNotNull { document ->
                    val storedIsSold = document.getBoolean("isSold") ?: document.getBoolean("sold") ?: false
                    val availabilityStatus = normalizeAvailabilityStatus(
                        document.getString("availabilityStatus"),
                        storedIsSold
                    )
                    document.toObject(Product::class.java)
                        ?.copy(
                            id = document.getString("id")?.takeIf { it.isNotBlank() } ?: document.id,
                            condition = document.getString("condition")?.takeIf { it.isNotBlank() } ?: ProductCondition.Used,
                            approvalStatus = document.getString("approvalStatus") ?: ProductApprovalStatus.Approved,
                            availabilityStatus = availabilityStatus,
                            isSold = availabilityStatus == ProductAvailabilityStatus.Sold
                        )
                }
                ?.sortedWith(
                    compareBy<Product> { productApprovalRank(it.approvalStatus) }
                        .thenByDescending { it.createdAt }
                )
                ?.let { listings.addAll(it) }
        }
    }

    fun approveProduct(productId: String) {
        if (!isAdmin.value || productId.isBlank()) return
        val productRef = db.collection("products").document(productId)
        productRef.get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId").orEmpty()
                val productName = document.getString("name") ?: "your listing"
                val now = System.currentTimeMillis()
                productRef.update(
                    mapOf(
                        "approvalStatus" to ProductApprovalStatus.Approved,
                        "reviewedAt" to now,
                        "reviewedBy" to auth.currentUser?.uid.orEmpty(),
                        "publishedAt" to now,
                        "rejectionReason" to "",
                        "updatedAt" to now
                    )
                )
                    .addOnSuccessListener {
                        if (ownerId.isNotBlank()) {
                            NotificationRepository.notifyUser(
                                recipientId = ownerId,
                                title = "Product Approved",
                                message = "\"$productName\" was approved and published.",
                                type = NotificationType.ProductStatus,
                                relatedId = productId,
                                relatedTitle = productName,
                                createdBy = auth.currentUser?.uid.orEmpty()
                            )
                        }
                        message.value = "Product approved and published"
                    }
                    .addOnFailureListener { message.value = it.message }
            }
            .addOnFailureListener { message.value = it.message }
    }

    fun rejectProduct(productId: String, reason: String = "") {
        if (!isAdmin.value || productId.isBlank()) return
        val productRef = db.collection("products").document(productId)
        productRef.get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId").orEmpty()
                val productName = document.getString("name") ?: "your listing"
                val now = System.currentTimeMillis()
                productRef.update(
                    mapOf(
                        "approvalStatus" to ProductApprovalStatus.Rejected,
                        "reviewedAt" to now,
                        "reviewedBy" to auth.currentUser?.uid.orEmpty(),
                        "publishedAt" to 0L,
                        "rejectionReason" to reason.trim(),
                        "updatedAt" to now
                    )
                )
                    .addOnSuccessListener {
                        if (ownerId.isNotBlank()) {
                            val detail = reason.trim().takeIf { it.isNotBlank() }?.let { " Reason: $it" }.orEmpty()
                            NotificationRepository.notifyUser(
                                recipientId = ownerId,
                                title = "Product Needs Changes",
                                message = "\"$productName\" was not approved.$detail",
                                type = NotificationType.ProductStatus,
                                relatedId = productId,
                                relatedTitle = productName,
                                createdBy = auth.currentUser?.uid.orEmpty()
                            )
                        }
                        message.value = "Product rejected"
                    }
                    .addOnFailureListener { message.value = it.message }
            }
            .addOnFailureListener { message.value = it.message }
    }

    fun removeListing(productId: String, onComplete: (Boolean) -> Unit = {}) {
        if (!isAdmin.value || productId.isBlank()) {
            onComplete(false)
            return
        }
        val productRef = db.collection("products").document(productId)
        productRef.get()
            .addOnSuccessListener { document ->
                val productName = document.getString("name") ?: "your listing"
                val ownerId = document.getString("ownerId").orEmpty()
                val now = System.currentTimeMillis()
                productRef.update(
                    mapOf(
                        "availabilityStatus" to ProductAvailabilityStatus.Removed,
                        "isSold" to false,
                        "updatedAt" to now
                    )
                )
                    .addOnSuccessListener {
                        if (ownerId.isNotBlank()) {
                            NotificationRepository.notifyUser(
                                recipientId = ownerId,
                                title = "Important Admin Notification",
                                message = "An admin removed \"$productName\" from the marketplace.",
                                type = NotificationType.Admin,
                                relatedId = productId,
                                relatedTitle = productName,
                                createdBy = auth.currentUser?.uid.orEmpty()
                            )
                        }
                        message.value = "Listing removed"
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        message.value = it.message
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                message.value = it.message
                onComplete(false)
            }
    }

    fun dismissReport(reportId: String) {
        if (!isAdmin.value || reportId.isBlank()) return
        val report = reports.find { it.id == reportId }
        db.collection("reports").document(reportId)
            .update("status", "dismissed")
            .addOnSuccessListener {
                report?.let {
                    NotificationRepository.notifyUser(
                        recipientId = it.reporterId,
                        title = "Important Admin Notification",
                        message = "Your report was dismissed after review.",
                        type = NotificationType.Admin,
                        relatedId = it.productId,
                        createdBy = auth.currentUser?.uid.orEmpty()
                    )
                }
                saveReportReviewMetadata(reportId, "dismissed", "Report dismissed")
            }
            .addOnFailureListener { message.value = it.message }
    }

    fun markReportReviewed(reportId: String) {
        if (!isAdmin.value || reportId.isBlank()) return
        val report = reports.find { it.id == reportId }
        if (report != null && report.status != "pending") {
            message.value = "Only pending reports can be reviewed"
            return
        }
        db.collection("reports").document(reportId)
            .update("status", "reviewed")
            .addOnSuccessListener {
                report?.let {
                    NotificationRepository.notifyUser(
                        recipientId = it.reporterId,
                        title = "Important Admin Notification",
                        message = "Your report was reviewed by an admin.",
                        type = NotificationType.Admin,
                        relatedId = it.productId,
                        createdBy = auth.currentUser?.uid.orEmpty()
                    )
                }
                saveReportReviewMetadata(reportId, "reviewed", "Report reviewed")
            }
            .addOnFailureListener { message.value = it.message }
    }

    fun markReportResolved(reportId: String) {
        if (!isAdmin.value || reportId.isBlank()) return
        val report = reports.find { it.id == reportId }
        if (report != null && report.status != "reviewed") {
            message.value = "Only reviewed reports can be resolved"
            return
        }
        db.collection("reports").document(reportId)
            .update("status", "resolved")
            .addOnSuccessListener {
                report?.let {
                    NotificationRepository.notifyUser(
                        recipientId = it.reporterId,
                        title = "Important Admin Notification",
                        message = "Your report has been resolved by an admin.",
                        type = NotificationType.Admin,
                        relatedId = it.productId,
                        relatedTitle = it.productTitle,
                        createdBy = auth.currentUser?.uid.orEmpty()
                    )
                }
                saveReportReviewMetadata(reportId, "resolved", "Report resolved")
            }
            .addOnFailureListener { message.value = it.message }
    }

    fun removeReportedListing(reportId: String) {
        if (!isAdmin.value || reportId.isBlank()) return
        val report = reports.find { it.id == reportId }
        if (report == null) {
            message.value = "Report not found"
            return
        }

        removeListing(report.productId) { removed ->
            if (removed) {
                db.collection("reports").document(reportId)
                    .update(
                        mapOf(
                            "status" to "resolved",
                            "adminAction" to "listing_removed",
                            "reviewedAt" to System.currentTimeMillis(),
                            "reviewedBy" to auth.currentUser?.uid.orEmpty()
                        )
                    )
                    .addOnSuccessListener {
                        NotificationRepository.notifyUser(
                            recipientId = report.reporterId,
                            title = "Important Admin Notification",
                            message = "An admin removed the listing you reported.",
                            type = NotificationType.Admin,
                            relatedId = report.productId,
                            relatedTitle = report.productTitle,
                            createdBy = auth.currentUser?.uid.orEmpty()
                        )
                        message.value = "Reported listing removed"
                    }
                    .addOnFailureListener { message.value = it.message }
            }
        }
    }

    fun blockReportedSeller(reportId: String) {
        if (!isAdmin.value || reportId.isBlank()) return
        val report = reports.find { it.id == reportId }
        if (report == null) {
            message.value = "Report not found"
            return
        }

        val sellerId = report.sellerId.ifBlank {
            listings.find { it.id == report.productId }?.ownerId.orEmpty()
        }
        if (sellerId.isBlank()) {
            message.value = "Reported seller was not found"
            return
        }

        setUserDisabled(sellerId, true) { blocked ->
            if (blocked) {
                db.collection("reports").document(reportId)
                    .update(
                        mapOf(
                            "status" to "resolved",
                            "adminAction" to "seller_blocked",
                            "reviewedAt" to System.currentTimeMillis(),
                            "reviewedBy" to auth.currentUser?.uid.orEmpty()
                        )
                    )
                    .addOnSuccessListener {
                        NotificationRepository.notifyUser(
                            recipientId = report.reporterId,
                            title = "Important Admin Notification",
                            message = "An admin took action on the seller you reported.",
                            type = NotificationType.Admin,
                            relatedId = report.productId,
                            relatedTitle = report.productTitle,
                            createdBy = auth.currentUser?.uid.orEmpty()
                        )
                        message.value = "Reported seller blocked"
                    }
                    .addOnFailureListener { message.value = it.message }
            }
        }
    }

    fun setUserDisabled(
        userId: String,
        disabled: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (!isAdmin.value || userId.isBlank() || userId == auth.currentUser?.uid) {
            onComplete(false)
            return
        }
        db.collection("users").document(userId).update("disabled", disabled)
            .addOnSuccessListener {
                realtimeDb.child("users").child(userId).child("disabled").setValue(disabled)
                NotificationRepository.notifyUser(
                    recipientId = userId,
                    title = "Important Admin Notification",
                    message = if (disabled) {
                        "Your marketplace account has been disabled by an admin."
                    } else {
                        "Your marketplace account has been enabled by an admin."
                    },
                    type = NotificationType.Admin,
                    createdBy = auth.currentUser?.uid.orEmpty()
                )
                message.value = if (disabled) "User disabled" else "User enabled"
                onComplete(true)
            }
            .addOnFailureListener {
                message.value = it.message
                onComplete(false)
            }
    }

    fun sendAdminNotificationToAll(title: String, notificationMessage: String) {
        if (!isAdmin.value) return
        if (title.isBlank() || notificationMessage.isBlank()) {
            message.value = "Notification title and message are required"
            return
        }

        NotificationRepository.notifyUsers(
            recipientIds = users.map { it.uid },
            title = title,
            message = notificationMessage,
            type = NotificationType.Admin,
            createdBy = auth.currentUser?.uid.orEmpty()
        )
        message.value = "Admin notification sent"
    }

    private fun saveReportReviewMetadata(reportId: String, action: String, successMessage: String) {
        db.collection("reports").document(reportId)
            .update(
                mapOf(
                    "adminAction" to action,
                    "reviewedAt" to System.currentTimeMillis(),
                    "reviewedBy" to auth.currentUser?.uid.orEmpty()
                )
            )
            .addOnSuccessListener { message.value = successMessage }
            .addOnFailureListener { message.value = it.message }
    }

    private fun reportStatusRank(status: String): Int {
        return when (status.lowercase()) {
            "pending" -> 0
            "reviewed" -> 1
            "resolved" -> 2
            "dismissed" -> 3
            else -> 4
        }
    }

    private fun productApprovalRank(status: String): Int {
        return when (status.lowercase()) {
            ProductApprovalStatus.Pending -> 0
            ProductApprovalStatus.Rejected -> 1
            ProductApprovalStatus.Approved -> 2
            else -> 3
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

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
        reportsListener?.remove()
        listingsListener?.remove()
    }
}
