package com.example.capstone2.model

data class Vehicle(
    val driverId: String = "",               // Corresponds to UserProfile.uid
    val carNumber: String = "",
    val carModel: String = "",
    val carBrand: String = "",
    val color: String = "",
    val conditionRating: Float? = null     // Rating of vehicle condition, to be calculated when enough reviews are available.
)
