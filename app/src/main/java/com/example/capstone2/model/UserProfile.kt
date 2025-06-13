package com.example.capstone2.model

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val driverRating: Float? = null,        // Rating as a driver.
    val passengerRating: Float? = null,     // Rating as a passenger
    val gender: Gender? = null,
    val university: String = ""
)