package com.example.campusmarketplace.products

data class Report(
    val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val sellerId: String = "",
    val reporterId: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending", // pending, reviewed, resolved, dismissed
    val adminAction: String = "",
    val reviewedAt: Long = 0L,
    val reviewedBy: String = ""
)
