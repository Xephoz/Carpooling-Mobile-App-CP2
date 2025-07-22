package com.example.capstone2.main.driver

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.capstone2.R
import com.example.capstone2.databinding.DriverCreateBinding
import com.example.capstone2.model.LocationInfo
import com.example.capstone2.model.Ride
import com.example.capstone2.model.University
import com.example.capstone2.model.Vehicle
import com.example.capstone2.viewmodels.CreateRideViewModel
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity.Companion.RESULT_OK
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class CreateFragment : Fragment() {

    private lateinit var viewModel: CreateRideViewModel
    private var _binding: DriverCreateBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private var universities: List<University> = emptyList()
    private var vehicles: List<Vehicle> = emptyList()
    private lateinit var placesClient: PlacesClient
    private val tag = "CreateRideFragment"


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

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel = ViewModelProvider(this)[CreateRideViewModel::class.java]

        binding.swapButton.setOnClickListener {
            swapLocations()
        }

        if (!Places.isInitialized()) {
            val apiKey = requireContext().getString(R.string.maps_api_key)
            Places.initialize(requireContext(), apiKey)
        }
        placesClient = Places.createClient(requireContext())

        setupStartLocationAutocomplete()
        fetchUniversities()
        fetchVehicles()
        setupDateTimePicker()
        restoreFromViewModel()

        binding.createButton.setOnClickListener {
            createRide()
        }
    }

    private fun restoreFromViewModel() {
        // Restore swap state first
        if (viewModel.isSwapped) {
            // Show swapped UI
            binding.startLocationSearch.visibility = View.GONE
            binding.startDropdown.visibility = View.VISIBLE
            binding.endDropdown.visibility = View.GONE
            binding.endLocationSearch.visibility = View.VISIBLE

            // Swap the icons
            binding.startIcon.setImageResource(R.drawable.baseline_place_24)
            binding.endIcon.setImageResource(R.drawable.baseline_trip_origin_24)

            // Setup dropdown if universities are loaded
            if (universities.isNotEmpty()) {
                setupStartDropdown()
            }
        } else {
            // Setup dropdown if universities are loaded
            if (universities.isNotEmpty()) {
                setupDestinationDropdown()
            }
        }

        // Restore starting point (handles both normal and swapped cases)
        viewModel.selectedStartPlace?.let { place ->
            val placeName = place.name ?: ""
            val placeAddress = place.address ?: ""
            val displayText = if (placeName.isNotEmpty() && placeAddress.startsWith(placeName)) {
                placeAddress
            } else {
                listOf(placeName, placeAddress)
                    .filter { it.isNotEmpty() }
                    .joinToString(" - ")
            }

            if (viewModel.isSwapped) {
                binding.endLocationSearchInput.setText(displayText)
                binding.endLocationSearch.hint = "Destination selected"
                binding.endLocationSearch.error = null
            } else {
                binding.startLocationSearchInput.setText(displayText)
                binding.startLocationSearch.hint = "Starting point selected"
                binding.startLocationSearch.error = null
            }
        }

        // Restore university selection (handles both normal and swapped cases)
        viewModel.selectedUniversityName?.let { universityName ->
            if (viewModel.isSwapped) {
                binding.startDropdownInput.setText(universityName)
                binding.startDropdown.error = null
            } else {
                binding.endDropdownInput.setText(universityName)
                binding.endDropdown.error = null
            }
        }

        // Restore other fields
        viewModel.selectedVehicleId?.let { vehicleId ->
            vehicles.find { it.id == vehicleId }?.let { vehicle ->
                binding.vehicleDropdownInput.setText(vehicle.carNumber)
            }
        }

        viewModel.maxPassengers?.let { binding.setMaxPassengers.setText(it.toString()) }
        binding.setFemaleOnly.isChecked = viewModel.femaleOnly

        // Restore date/time if selected
        if (viewModel.isDateTimeSelected) {
            updateDateTimeUI()
        }
    }

    private fun swapLocations() {
        viewModel.isSwapped = !viewModel.isSwapped

        // Clear current values
        binding.startLocationSearchInput.text?.clear()
        binding.startLocationSearch.error = null
        binding.startDropdownInput.text?.clear()
        binding.startDropdown.error = null
        binding.endLocationSearchInput.text?.clear()
        binding.endLocationSearch.error = null
        binding.endDropdownInput.text?.clear()
        binding.endDropdown.error = null
        viewModel.selectedStartPlace = null
        viewModel.selectedUniversityName = null

        // Update UI based on swapped state
        if (viewModel.isSwapped) {
            // Start becomes university dropdown
            binding.startLocationSearch.visibility = View.GONE
            binding.startDropdown.visibility = View.VISIBLE
            binding.startDropdown.hint = "Select starting point"

            // End becomes geoPoint autocomplete
            binding.endDropdown.visibility = View.GONE
            binding.endLocationSearch.visibility = View.VISIBLE
            binding.endLocationSearch.hint = "Enter destination"

            // Setup dropdown for start if universities are loaded
            if (universities.isNotEmpty()) {
                setupStartDropdown()
            }

            // Setup autocomplete for end
            setupEndLocationAutocomplete()
        } else {
            // Restore original state
            binding.startLocationSearch.visibility = View.VISIBLE
            binding.startDropdown.visibility = View.GONE
            binding.startLocationSearch.hint = "Enter starting point"

            binding.endDropdown.visibility = View.VISIBLE
            binding.endLocationSearch.visibility = View.GONE
            binding.endDropdown.hint = "Select destination"

            // Setup dropdown if universities are loaded
            if (universities.isNotEmpty()) {
                setupDestinationDropdown()
            }

            // Restore original autocomplete
            setupStartLocationAutocomplete()
        }
    }

    private fun setupStartLocationAutocomplete() {
        binding.startLocationSearchInput.apply {
            isFocusable = false
            isFocusableInTouchMode = false

            setOnClickListener {
                val autocompleteIntent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY,
                    listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.LAT_LNG,
                        Place.Field.ADDRESS)
                )
                    .setCountries(listOf("my"))
                    .build(requireContext())

                startActivityForResult(autocompleteIntent, AUTOCOMPLETE_REQUEST_CODE)
            }
        }
    }

    private fun setupEndLocationAutocomplete() {
        binding.endLocationSearchInput.apply {
            isFocusable = false
            isFocusableInTouchMode = false

            setOnClickListener {
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

                startActivityForResult(autocompleteIntent, AUTOCOMPLETE_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.let {
                val place = Autocomplete.getPlaceFromIntent(data)
                val placeName = place.name ?: ""
                val placeAddress = place.address ?: ""
                val displayText = if (placeName.isNotEmpty() && placeAddress.startsWith(placeName)) {
                    placeAddress
                } else {
                    listOf(placeName, placeAddress)
                        .filter { it.isNotEmpty() }
                        .joinToString(" - ")
                }

                viewModel.selectedStartPlace = place
                if (viewModel.isSwapped) {
                    binding.endLocationSearchInput.setText(displayText)
                    binding.endLocationSearch.hint = "Destination selected"
                    binding.endLocationSearch.error = null
                } else {
                    binding.startLocationSearchInput.setText(displayText)
                    binding.startLocationSearch.hint = "Starting point selected"
                    binding.startLocationSearch.error = null
                }
            }
        }
    }

    private fun fetchUniversities() {
        db.collection("universities")
            .get()
            .addOnSuccessListener { result ->
                universities = result.map { document ->
                    document.toObject(University::class.java).copy(id = document.id)
                }
                // Only setup dropdown if not swapped
                if (!viewModel.isSwapped) {
                    setupDestinationDropdown()
                }
                // Setup start dropdown if swapped
                if (viewModel.isSwapped) {
                    setupStartDropdown()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Error fetching universities", exception)
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
        binding.endDropdownInput.setAdapter(adapter)
        binding.endDropdownInput.setOnItemClickListener { _, _, _, _ ->
            binding.endDropdown.error = null
            viewModel.selectedUniversityName = binding.endDropdownInput.text.toString()
        }
        binding.endDropdownInput.setOnClickListener { binding.endDropdownInput.showDropDown() }
    }

    private fun setupStartDropdown() {
        val universityNames = universities.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            universityNames
        )
        binding.startDropdownInput.setAdapter(adapter)
        binding.startDropdownInput.setOnItemClickListener { _, _, position, _ ->
            val selectedUni = universities[position]
            viewModel.selectedUniversityName = selectedUni.name
            binding.startDropdown.error = null
        }
        binding.startDropdownInput.setOnClickListener { binding.startDropdownInput.showDropDown() }
    }

    private fun fetchVehicles() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("vehicles")
            .whereEqualTo("driverId", userId)
            .get()
            .addOnSuccessListener { result ->
                vehicles = result.map { document ->
                    document.toObject(Vehicle::class.java).copy(id = document.id)
                }
                setupVehicleDropdown()
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Error fetching vehicles", exception)
                vehicles = emptyList()
                setupVehicleDropdown()
            }
    }

    private fun setupVehicleDropdown() {
        val isEmpty = vehicles.isEmpty()
        val items = if (isEmpty) {
            listOf("Please register your vehicle in Settings")
        } else {
            vehicles.map { it.carNumber }
        }

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            items
        ) {
            override fun isEnabled(position: Int): Boolean {
                // Only allow selection if we have actual vehicles
                return !isEmpty
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (isEmpty) {
                    view.setOnClickListener {
                        Toast.makeText(context, "Please register a vehicle in Settings", Toast.LENGTH_SHORT).show()
                        // Optional: Add navigation to settings here
                        // findNavController().navigate(R.id.action_to_settings)
                    }
                }
                return view
            }
        }

        binding.vehicleDropdownInput.setAdapter(adapter)

        binding.vehicleDropdownInput.setOnItemClickListener { _, _, position, _ ->
            if (!isEmpty) {
                viewModel.selectedVehicleId = vehicles[position].id
                binding.vehicleDropdown.error = null
            }
        }

        binding.vehicleDropdownInput.setOnClickListener {
            binding.vehicleDropdownInput.showDropDown()
        }

        // Clear text if no vehicles (but don't show the message in the input)
        if (isEmpty) {
            binding.vehicleDropdownInput.text?.clear()
        } else {
            // Restore selection if available
            viewModel.selectedVehicleId?.let { selectedId ->
                vehicles.find { it.id == selectedId }?.let { selectedVehicle ->
                    binding.vehicleDropdownInput.setText(selectedVehicle.carNumber, false)
                }
            }
        }
    }

    private fun setupDateTimePicker() {
        binding.setDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                viewModel.selectedDateTime.set(year, month, day)
                showTimePicker()
            },
            viewModel.selectedDateTime.get(Calendar.YEAR),
            viewModel.selectedDateTime.get(Calendar.MONTH),
            viewModel.selectedDateTime.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val tempCalendar = Calendar.getInstance().apply {
                    set(
                        viewModel.selectedDateTime.get(Calendar.YEAR),
                        viewModel.selectedDateTime.get(Calendar.MONTH),
                        viewModel.selectedDateTime.get(Calendar.DAY_OF_MONTH),
                        hour,
                        minute
                    )
                }

                if (tempCalendar.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "Please select a future time", Toast.LENGTH_SHORT).show()
                    showTimePicker()
                } else {
                    viewModel.selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                    viewModel.selectedDateTime.set(Calendar.MINUTE, minute)
                    viewModel.isDateTimeSelected = true
                    updateDateTimeUI()
                }
            },
            viewModel.selectedDateTime.get(Calendar.HOUR_OF_DAY),
            viewModel.selectedDateTime.get(Calendar.MINUTE),
            false
        ).apply {
            if (isToday(viewModel.selectedDateTime)) {
                val now = Calendar.getInstance()
                updateTime(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
            }
            show()
        }
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
    }

    private fun updateDateTimeUI() {
        val formattedDateTime = formatDateTime(viewModel.selectedDateTime)
        binding.setDate.setText(formattedDateTime)
        binding.setDate.error = null
    }

    private fun formatDateTime(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val datePart = dateFormat.format(calendar.time)

        val timeFormat = SimpleDateFormat("h:mma", Locale.getDefault())
        val timePart = timeFormat.format(calendar.time).lowercase(Locale.getDefault())

        return "$datePart at $timePart"
    }

    private fun createRide() {
        binding.createProgressBar.visibility = View.VISIBLE
        binding.createButton.isEnabled = false

        val driverId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showError("User not authenticated")
            return
        }

        val startPlace: Place
        val selectedUniversity: University

        if (!viewModel.isSwapped) {
            // Original state
            // Start is geoPoint autocomplete
            startPlace = viewModel.selectedStartPlace ?: run {
                binding.startLocationSearch.error = "Please select a starting point"
                showError("Starting point required")
                return
            }

            // End is university dropdown
            val selectedUniversityName = binding.endDropdownInput.text.toString()
            selectedUniversity = universities.find { it.name == selectedUniversityName } ?: run {
                binding.endDropdown.error = "Please select a destination"
                showError("Destination required")
                return
            }
        } else {
            // Swapped state
            // Start is university dropdown
            val selectedUniversityName = binding.startDropdownInput.text.toString()
            selectedUniversity = universities.find { it.name == selectedUniversityName } ?: run {
                binding.startDropdown.error = "Please select a starting point"
                showError("Starting point required")
                return
            }

            // End is geoPoint autocomplete
            startPlace = viewModel.selectedStartPlace ?: run {
                binding.endLocationSearch.error = "Please select a destination"
                showError("Destination required")
                return
            }
        }

        if (vehicles.isEmpty()) {
            binding.vehicleDropdown.error = " "
            Toast.makeText(context, "Please register a vehicle first", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedVehicleId = viewModel.selectedVehicleId ?: run {
            binding.vehicleDropdown.error = " "
            showError("Vehicle selection required")
            return
        }

        val maxPassengers = binding.setMaxPassengers.text.toString().toIntOrNull()?.takeIf { it in 1..8 } ?: run {
            binding.setMaxPassengers.error = "Please enter a number (1-8)"
            showError("Invalid passenger count")
            return
        }
        viewModel.maxPassengers = maxPassengers

        if (!viewModel.isDateTimeSelected) {
            binding.setDate.error = "Please select departure time"
            showError("Departure time required")
            return
        }

        if (viewModel.selectedDateTime.timeInMillis <= System.currentTimeMillis()) {
            binding.setDate.error = "Please select a future time"
            showError("Departure time must be in the future")
            return
        }

        viewModel.femaleOnly = binding.setFemaleOnly.isChecked

        // Create the ride with proper locations based on swap state
        val ride = if (!viewModel.isSwapped) {
            // Normal mode: start is geoPoint, end is university
            Ride(
                driverId = driverId,
                vehicleId = selectedVehicleId,
                startLocation = LocationInfo(
                    displayName = startPlace.name ?: "",
                    fullAddress = startPlace.address ?: "",
                    universityId = null,
                    placeId = startPlace.id,
                    geoPoint = GeoPoint(
                        startPlace.latLng?.latitude ?: 0.0,
                        startPlace.latLng?.longitude ?: 0.0
                    )
                ),
                endLocation = LocationInfo(
                    displayName = selectedUniversity.name,
                    universityId = selectedUniversity.id,
                    placeId = selectedUniversity.placeId,
                    geoPoint = selectedUniversity.geoPoint
                ),
                maxPassengers = maxPassengers,
                departureTime = Timestamp(viewModel.selectedDateTime.time),
                femaleOnly = viewModel.femaleOnly
            )
        } else {
            // Swapped mode: start is university, end is geoPoint
            Ride(
                driverId = driverId,
                vehicleId = selectedVehicleId,
                startLocation = LocationInfo(
                    displayName = selectedUniversity.name,
                    universityId = selectedUniversity.id,
                    placeId = selectedUniversity.placeId,
                    geoPoint = selectedUniversity.geoPoint
                ),
                endLocation = LocationInfo(
                    displayName = startPlace.name ?: "",
                    fullAddress = startPlace.address ?: "",
                    universityId = null,
                    placeId = startPlace.id,
                    geoPoint = GeoPoint(
                        startPlace.latLng?.latitude ?: 0.0,
                        startPlace.latLng?.longitude ?: 0.0
                    )
                ),
                maxPassengers = maxPassengers,
                departureTime = Timestamp(viewModel.selectedDateTime.time),
                femaleOnly = viewModel.femaleOnly
            )
        }

        db.collection("rides").add(ride)
            .addOnSuccessListener {
                binding.createProgressBar.visibility = View.GONE
                binding.createButton.isEnabled = true
                Toast.makeText(context, "Ride created!", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error creating ride", e)
                showError("Failed to create ride: ${e.localizedMessage}")
            }
    }

    private fun showError(message: String) {
        binding.createProgressBar.visibility = View.GONE
        binding.createButton.isEnabled = true
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearForm() {
        if (viewModel.isSwapped) {
            binding.startDropdownInput.text?.clear()
            binding.startDropdown.error = null
            binding.startDropdown.hint = "Select starting point"

            binding.endLocationSearchInput.text?.clear()
            binding.endLocationSearch.error = null
            binding.endLocationSearch.hint = "Enter destination"
        } else {
            binding.startLocationSearchInput.text?.clear()
            binding.startLocationSearch.error = null
            binding.startLocationSearch.hint = "Enter starting point"

            binding.endDropdownInput.text?.clear()
            binding.endDropdown.error = null
            binding.endDropdown.hint = "Select destination"
        }

        binding.vehicleDropdownInput.text?.clear()
        binding.vehicleDropdownInput.error = null

        binding.setMaxPassengers.text?.clear()
        binding.setMaxPassengers.error = null

        binding.setDate.text?.clear()
        binding.setFemaleOnly.isChecked = false

        viewModel.clear()

        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 1001
    }
}