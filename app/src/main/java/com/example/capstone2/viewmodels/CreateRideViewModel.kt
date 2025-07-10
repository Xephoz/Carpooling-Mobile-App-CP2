package com.example.capstone2.viewmodels

import androidx.lifecycle.ViewModel
import com.google.android.libraries.places.api.model.Place
import java.util.*

class CreateRideViewModel : ViewModel() {
    // Location fields
    var selectedStartPlace: Place? = null
    var selectedEndPlace: Place? = null
    var selectedUniversityName: String? = null
    var isSwapped = false

    // Ride details
    var selectedDateTime: Calendar = Calendar.getInstance()
    var isDateTimeSelected: Boolean = false
    var vehicleId: String? = null
    var maxPassengers: Int? = null
    var femaleOnly: Boolean = false

    fun clear() {
        selectedStartPlace = null
        selectedEndPlace = null
        selectedUniversityName = null
        isSwapped = false
        selectedDateTime = Calendar.getInstance()
        isDateTimeSelected = false
        vehicleId = null
        maxPassengers = null
        femaleOnly = false
    }

    fun getActualStartPlace(): Place? = if (isSwapped) null else selectedStartPlace
    fun getActualEndPlace(): Place? = if (isSwapped) selectedEndPlace else null
    fun getActualUniversityName(): String? = if (isSwapped) selectedUniversityName else null
}