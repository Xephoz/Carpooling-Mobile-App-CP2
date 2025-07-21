package com.example.capstone2.model

import com.google.firebase.Timestamp

enum class RequestStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}

data class RideRequest(
    var requestId: String = "",
    var rideId: String = "",
    var passengerId: String = "",     // Reference to the passenger's UserID
    var driverId: String = "",         // Reference to the driver's UserID
    var status: RequestStatus = RequestStatus.PENDING,
    var requestedAt: Timestamp = Timestamp.now(),
) {
    constructor() : this(
        requestId = "",
        rideId = "",
        passengerId = "",
        driverId = "",
        status = RequestStatus.PENDING,
        requestedAt = Timestamp.now(),
    )
}