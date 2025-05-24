package com.example.capstone2.model

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null
)