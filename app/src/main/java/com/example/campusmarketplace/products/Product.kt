package com.example.campusmarketplace.products

import com.google.firebase.firestore.PropertyName

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val condition: String = ProductCondition.Used,
    val location: String = "",
    val contactPreference: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L,
    val approvalStatus: String = ProductApprovalStatus.Approved,
    val availabilityStatus: String = ProductAvailabilityStatus.Available,
    val reviewedAt: Long = 0L,
    val reviewedBy: String = "",
    val publishedAt: Long = 0L,
    val rejectionReason: String = "",
    @get:PropertyName("isSold")
    @set:PropertyName("isSold")
    var isSold: Boolean = false
)

object ProductApprovalStatus {
    const val Pending = "pending"
    const val Approved = "approved"
    const val Rejected = "rejected"
}

object ProductAvailabilityStatus {
    const val Available = "available"
    const val Reserved = "reserved"
    const val Sold = "sold"
    const val Removed = "removed"
}

object ProductCondition {
    const val New = "New"
    const val LikeNew = "Like New"
    const val Good = "Good"
    const val Fair = "Fair"
    const val Used = "Used"
}
