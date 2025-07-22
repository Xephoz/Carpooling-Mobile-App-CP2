package com.example.capstone2.model

data class Vehicle(
    val id: String = "",
    val driverId: String = "",               // Corresponds to UserProfile.uid
    val carNumber: String = "",
    val carModel: String = "",
    val carBrand: String = "",
    val color: String = "",
    // TODO: Rating of vehicle condition
    val conditionRating: Float? = null,
)


