package com.example.capstone2.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class University(
    val id: String = "",
    val placeId: String? = null,
    val name: String = "",
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0)
)