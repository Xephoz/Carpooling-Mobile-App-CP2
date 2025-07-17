package com.example.capstone2.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

enum class RideStatus {
    ACTIVE,
    ONGOING,
    COMPLETED
}

data class Ride(
    var driverId: String = "",
    var vehicleId: String = "",
    var startLocation: LocationInfo = LocationInfo(),
    var endLocation: LocationInfo = LocationInfo(),
    var departureTime: Timestamp = Timestamp.now(),
    var createdAt: Timestamp = Timestamp.now(),
    var maxPassengers: Int = 0,
    var passengers: List<String> = emptyList(),
    var femaleOnly: Boolean = false,
    var status: RideStatus = RideStatus.ACTIVE  // Default value is ACTIVE
) {
    constructor() : this(
        driverId = "",
        vehicleId = "",
        startLocation = LocationInfo(),
        endLocation = LocationInfo(),
        departureTime = Timestamp.now(),
        createdAt = Timestamp.now(),
        maxPassengers = 0,
        passengers = emptyList(),
        femaleOnly = false,
        status = RideStatus.ACTIVE
    )
}

@IgnoreExtraProperties
data class LocationInfo(
    var displayName: String = "",
    var fullAddress: String = "",
    var universityId: String? = null,
    var placeId: String? = null,
    var geoPoint: GeoPoint = GeoPoint(0.0, 0.0)
) {
    constructor() : this("", "", null, null, GeoPoint(0.0, 0.0))
}