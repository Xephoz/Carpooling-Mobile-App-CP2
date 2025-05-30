package com.example.capstone2.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Ride(
    val rideId: String = "",
    val driverId: String = "",
    val vehicleId: String = "",             // Reference to a Vehicle.vehicleId
    val startLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val startAddress: String = "",
    val destinationInstitution: String = "",
    val destinationLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val departureTime: Timestamp = Timestamp.now(),
    val maxPassengers: Int = 3,
    val currentPassengerIds: List<String> = emptyList(),
    val isFemaleOnly: Boolean = false
)

