package com.example.campusmarketplace.products

data class SellerReview(
    val id: String = "",
    val sellerId: String = "",
    val reviewerId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
