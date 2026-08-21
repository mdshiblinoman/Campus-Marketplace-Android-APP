package com.example.campusmarketplace.products

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val ownerId: String = "",
    val isSold: Boolean = false
)
