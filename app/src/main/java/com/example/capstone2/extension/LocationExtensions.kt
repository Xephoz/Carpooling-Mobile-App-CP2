package com.example.capstone2.extension

import android.location.Location
import com.google.firebase.firestore.GeoPoint

fun GeoPoint.distanceTo(other: GeoPoint): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        this.latitude,
        this.longitude,
        other.latitude,
        other.longitude,
        results
    )
    return results[0]
}