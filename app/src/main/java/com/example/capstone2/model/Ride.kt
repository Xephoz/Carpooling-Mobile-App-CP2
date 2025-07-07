package com.example.capstone2.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

data class Ride(
    var driverId: String = "",
    var vehicleId: String = "",
    var startLocation: LocationInfo = LocationInfo(),
    var endLocation: LocationInfo = LocationInfo(),
    var departureTime: Timestamp = Timestamp.now(),
    var createdAt: Timestamp = Timestamp.now(),
    var maxPassengers: Int = 0,
    var passengers: List<String> = emptyList(),
    var femaleOnly: Boolean = false
) {
    constructor() : this("", "", LocationInfo(), LocationInfo(), Timestamp.now(), Timestamp.now(), 1, emptyList(), false)
}


@IgnoreExtraProperties
data class LocationInfo(
    var displayName: String = "",
    var fullAddress: String = "",
    var universityId: String? = null,
    var placeId: String? = null,
    var geoPoint: GeoPoint = GeoPoint(0.0, 0.0)
) {
    // No-arg constructor for Firebase
    constructor() : this("", "", null, null, GeoPoint(0.0, 0.0))
}

//    val isActive: Boolean = true,
//    val isCompleted: Boolean = false,