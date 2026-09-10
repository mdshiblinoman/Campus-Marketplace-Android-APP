package com.example.campusmarketplace.admin

data class MarketplaceUser(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val studentId: String = "",
    val mobile: String = "",
    val department: String = "",
    val role: String = "student",
    val disabled: Boolean = false,
    val registrationDate: Long = 0L,
    val lastLogin: Long = 0L
)
