package com.example.capstone2.model

import com.google.firebase.firestore.GeoPoint

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class PassengerRequest(
    val requestId: String = "",
    val passengerId: String = "",
    val rideId: String = "",
    val pickupLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val pickupAddress: String = "",
    val status: RequestStatus = RequestStatus.PENDING
)
