package com.example.capstone2.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

data class Ride(
    val id: String = "",
    val driverId: String,
    val vehicleId: String,
    val startLocation: LocationInfo,
    val endLocation: LocationInfo,
    val departureTime: Timestamp,
    val createdAt: Timestamp = Timestamp.now(),
    val maxPassengers: Int,
    val passengers: List<String> = emptyList(),
    val femaleOnly: Boolean = false,
)


@IgnoreExtraProperties
data class LocationInfo(
    val displayName: String = "",
    val universityId: String? = null,
    val placeId: String? = null,
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0)
)

//    val createdAt: Timestamp = Timestamp.now(),
//    val isActive: Boolean = true,
//    val isCompleted: Boolean = false,