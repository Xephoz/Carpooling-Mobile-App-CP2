package com.example.capstone2.main.driver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.capstone2.R
import com.example.capstone2.databinding.DriverCreateBinding
import com.example.capstone2.model.LocationInfo
import com.example.capstone2.model.Ride
import com.example.capstone2.model.University
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity.Companion.RESULT_CANCELED
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity.Companion.RESULT_OK
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar


class CreateFragment : Fragment() {

    private var _binding: DriverCreateBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private lateinit var universities: List<University>
    private var selectedStartPlace: Place? = null
    private lateinit var placesClient: PlacesClient
    private lateinit var selectedDateTime: Calendar
    private val TAG = "CreateRideFragment"
    private var isDateTimeSelected = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DriverCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Places API
        if (!Places.isInitialized()) {
            val apiKey = requireContext().getString(R.string.maps_api_key)
            Places.initialize(requireContext(), apiKey)
        }
        placesClient = Places.createClient(requireContext())
        setupStartLocationAutocomplete()
        fetchUniversities()

        selectedDateTime = Calendar.getInstance()
        setupDateTimePicker()

        binding.createButton.setOnClickListener {
            createRide()
        }
    }

    private fun setupStartLocationAutocomplete() {
        val startAutocomplete = binding.setStart

        val autocompleteIntent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
        )
            .setCountries(listOf("my"))
            .build(requireContext())

        // Set click listener for the TextInputEditText
        startAutocomplete.setOnClickListener {
            startAutocomplete.hideKeyboard()
            startActivityForResult(autocompleteIntent, AUTOCOMPLETE_REQUEST_CODE)
        }

        // Optional: Set focus change listener to trigger search when focused
        startAutocomplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                startAutocomplete.hideKeyboard()
                startActivityForResult(autocompleteIntent, AUTOCOMPLETE_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        selectedStartPlace = place
                        binding.setStart.setText(place.name ?: place.address)
                        binding.startBox.hint = "Starting point selected"
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.e(TAG, "Error: ${status.statusMessage}")
                    }
                }
                RESULT_CANCELED -> {
                    // The user canceled the operation
                }
            }
        }
    }

    // Helper extension to hide keyboard
    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun fetchUniversities() {
        db.collection("universities")
            .get()
            .addOnSuccessListener { result ->
                universities = result.map { document ->
                    document.toObject(University::class.java).copy(id = document.id)
                }
                setupDestinationDropdown()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching universities", exception)
                Toast.makeText(context, "Failed to load universities", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDestinationDropdown() {
        val universityNames = universities.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            universityNames
        )
        binding.setEnd.setAdapter(adapter)
        binding.setEnd.setOnClickListener { binding.setEnd.showDropDown() }
    }

    private fun setupDateTimePicker() {
        binding.setDate.setOnClickListener {
            // First show date picker
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDateTime.set(year, month, day)
                // After date is selected, show time picker
                showTimePicker()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to current time
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                // Create a temporary calendar to validate
                val tempCalendar = Calendar.getInstance().apply {
                    set(
                        selectedDateTime.get(Calendar.YEAR),
                        selectedDateTime.get(Calendar.MONTH),
                        selectedDateTime.get(Calendar.DAY_OF_MONTH),
                        hour,
                        minute
                    )
                }

                // Check if selected time is in the future
                if (tempCalendar.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(
                        requireContext(),
                        "Please select a time in the future",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Show time picker again
                    showTimePicker()
                } else {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedDateTime.set(Calendar.MINUTE, minute)
                    updateDateTimeUI()
                }
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        )

        // Set minimum time if it's today's date
        if (isToday(selectedDateTime)) {
            val now = Calendar.getInstance()
            timePicker.updateTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        }

        timePicker.show()
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
    }

    private fun updateDateTimeUI() {
        val dateFormat = android.text.format.DateFormat.getDateFormat(requireContext())
        val timeFormat = android.text.format.DateFormat.getTimeFormat(requireContext())

        val dateStr = dateFormat.format(selectedDateTime.time)
        val timeStr = timeFormat.format(selectedDateTime.time)

        binding.setDate.setText("$dateStr $timeStr")
        isDateTimeSelected = true
        binding.setDate.error = null // Clear any previous error
    }

    private fun createRide() {
        // Show loading state
        binding.createProgressBar.visibility = View.VISIBLE
        binding.createButton.isEnabled = false

        // Validate inputs
        val driverId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showError("User not authenticated")
            return
        }

        if (selectedStartPlace == null) {
            binding.startBox.error = "Please select a starting point"
            showError("Starting point required")
            return
        }

        val selectedUniversityName = binding.setEnd.text.toString()
        val selectedUniversity = universities.find { it.name == selectedUniversityName } ?: run {
            binding.endBox.error = "Please select a destination"
            showError("Destination required")
            return
        }

        val vehicleId = binding.setVehicle.text.toString().takeIf { it.isNotBlank() } ?: run {
            binding.setVehicle.error = "Please enter vehicle info"
            showError("Vehicle information required")
            return
        }

        val maxPassengersText = binding.setMaxPassengers.text.toString()
        val maxPassengers = try {
            val value = maxPassengersText.toInt()
            if (value !in 1..8) {
                binding.setMaxPassengers.error = "Must be between 1-8"
                showError("Passenger count must be between 1-8")
                return
            }
            value
        } catch (_: NumberFormatException) {
            binding.setMaxPassengers.error = "Please enter a number between 1-8"
            showError("Invalid passenger count")
            return
        }

        // Add date validation
        if (!isDateTimeSelected) {
            binding.setDate.error = "Please select departure time"
            showError("Departure time required")
            return
        }

        // Final timestamp validation
        if (selectedDateTime.timeInMillis <= System.currentTimeMillis()) {
            binding.setDate.error = "Please select a future date and time"
            showError("Departure time must be in the future")
            return
        }

        // Create location objects
        val startPlace = selectedStartPlace!!
        val startLocation = LocationInfo(
            displayName = startPlace.address ?: startPlace.name ?: "Unknown location",
            universityId = null,
            placeId = startPlace.id,
            geoPoint = GeoPoint(
                startPlace.latLng?.latitude ?: 0.0,
                startPlace.latLng?.longitude ?: 0.0
            )
        )

        val endLocation = LocationInfo(
            displayName = selectedUniversity.name,
            universityId = selectedUniversity.id,
            placeId = selectedUniversity.placeId,
            geoPoint = selectedUniversity.location
        )


        // Create ride object
        val ride = Ride(
            driverId = driverId,
            vehicleId = vehicleId,
            startLocation = startLocation,
            endLocation = endLocation,
            maxPassengers = maxPassengers,
            departureTime = Timestamp(selectedDateTime.time),
            femaleOnly = binding.setFemaleOnly.isChecked
        )

        // Save to Firestore
        db.collection("rides")
            .add(ride)
            .addOnSuccessListener { documentReference ->
                binding.createProgressBar.visibility = View.GONE
                binding.createButton.isEnabled = true
                Toast.makeText(context, "Ride created successfully!", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating ride", e)
                showError("Failed to create ride: ${e.localizedMessage}")
            }
    }

    private fun showError(message: String) {
        binding.createProgressBar.visibility = View.GONE
        binding.createButton.isEnabled = true
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearForm() {
        binding.setStart.text?.clear()
        binding.startBox.error = null
        binding.startBox.hint = "Enter starting point"
        selectedStartPlace = null

        binding.setEnd.text?.clear()
        binding.endBox.error = null

        binding.setVehicle.text?.clear()
        binding.setVehicle.error = null

        binding.setMaxPassengers.text?.clear()
        binding.setMaxPassengers.error = null

        binding.setDate.text?.clear()
        binding.setFemaleOnly.isChecked = false

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 1001
    }
}

// 20011656@imail.sunway.edu.my