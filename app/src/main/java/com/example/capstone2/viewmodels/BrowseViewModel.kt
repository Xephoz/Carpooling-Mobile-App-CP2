package com.example.capstone2.viewmodels

import androidx.lifecycle.ViewModel
import com.example.capstone2.model.Ride
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowseViewModel : ViewModel() {
    private val _rides = MutableStateFlow<List<Pair<String, Ride>>>(emptyList())
    val rides: StateFlow<List<Pair<String, Ride>>> = _rides.asStateFlow()

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setRides(rides: List<Pair<String, Ride>>) {
        _rides.value = rides
    }

    fun setCurrentLocation(location: GeoPoint?) {
        _currentLocation.value = location
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}