package com.example.campusmarketplace.products

data class Report(
    val id: String = "",
    val productId: String = "",
    val reporterId: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending" // pending, reviewed, dismissed
)
