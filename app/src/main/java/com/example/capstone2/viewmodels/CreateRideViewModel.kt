package com.example.capstone2.viewmodels

import androidx.lifecycle.ViewModel
import com.google.android.libraries.places.api.model.Place
import java.util.*

class CreateRideViewModel : ViewModel() {
    var selectedStartPlace: Place? = null
    var selectedDateTime: Calendar = Calendar.getInstance()
    var isDateTimeSelected: Boolean = false
    var selectedUniversityName: String? = null
    var selectedVehicleId: String? = null  // Changed from vehicleId to selectedVehicleId for clarity
    var maxPassengers: Int? = null
    var femaleOnly: Boolean = false
    var isSwapped = false

    fun clear() {
        selectedStartPlace = null
        selectedDateTime = Calendar.getInstance()
        isDateTimeSelected = false
        selectedUniversityName = null
        selectedVehicleId = null  // Updated to match new property name
        maxPassengers = null
        femaleOnly = false
        isSwapped = false
    }
}