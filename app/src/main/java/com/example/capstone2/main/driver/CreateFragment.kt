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
import com.example.capstone2.viewmodels.CreateRideViewModel
import com.google.android.gms.maps.model.LatLng
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
import java.util.Calendar


class CreateFragment : Fragment() {

    private lateinit var viewModel: CreateRideViewModel
    private var _binding: DriverCreateBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private lateinit var universities: List<University>
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
            navigateToList()
        }

        viewModel = ViewModelProvider(this)[CreateRideViewModel::class.java]

        if (!Places.isInitialized()) {
            val apiKey = requireContext().getString(R.string.maps_api_key)
            Places.initialize(requireContext(), apiKey)
        }
        placesClient = Places.createClient(requireContext())

        setupStartLocationAutocomplete()
        fetchUniversities()
        setupDateTimePicker()
        restoreFromViewModel()

        binding.createButton.setOnClickListener {
            createRide()
        }
    }

    private fun navigateToList() {
        findNavController().navigate(R.id.action_createFragment_to_listFragment)
    }

    private fun restoreFromViewModel() {
        // Restore starting point
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

            binding.startLocationSearchInput.setText(displayText)
            binding.startLocationSearch.hint = "Starting point selected"
        }

        // Restore other fields
        viewModel.selectedUniversityName?.let { binding.endDropdownInput.setText(it) }
        viewModel.vehicleId?.let { binding.setVehicle.setText(it) }
        viewModel.maxPassengers?.let { binding.setMaxPassengers.setText(it.toString()) }
        binding.setFemaleOnly.isChecked = viewModel.femaleOnly

        // Restore date/time if selected
        if (viewModel.isDateTimeSelected) {
            updateDateTimeUI()
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
                binding.startLocationSearchInput.setText(displayText)
                binding.startLocationSearch.hint = "Starting point selected"
                binding.startLocationSearch.error = null
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
                setupDestinationDropdown()
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
        val dateFormat = android.text.format.DateFormat.getDateFormat(requireContext())
        val timeFormat = android.text.format.DateFormat.getTimeFormat(requireContext())
        binding.setDate.setText("${dateFormat.format(viewModel.selectedDateTime.time)} ${timeFormat.format(viewModel.selectedDateTime.time)}")
        binding.setDate.error = null
    }

    private fun createRide() {
        binding.createProgressBar.visibility = View.VISIBLE
        binding.createButton.isEnabled = false

        val driverId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showError("User not authenticated")
            return
        }

        val startPlace = viewModel.selectedStartPlace ?: run {
            binding.startLocationSearch.error = "Please select a starting point"
            showError("Starting point required")
            return
        }

        val selectedUniversityName = binding.endDropdownInput.text.toString()
        val selectedUniversity = universities.find { it.name == selectedUniversityName } ?: run {
            binding.endDropdown.error = "Please select a destination"
            showError("Destination required")
            return
        }
        viewModel.selectedUniversityName = selectedUniversityName

        val vehicleId = binding.setVehicle.text.toString()
            .takeIf { it.isNotBlank() }
            ?.uppercase()
            ?: run {
                binding.setVehicle.error = "Please enter vehicle info"
                showError("Vehicle information required")
                return
            }
        viewModel.vehicleId = vehicleId

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

        val ride = Ride(
            driverId = driverId,
            vehicleId = vehicleId,
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
                geoPoint = selectedUniversity.location
            ),
            maxPassengers = maxPassengers,
            departureTime = Timestamp(viewModel.selectedDateTime.time),
            femaleOnly = viewModel.femaleOnly
        )

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
        binding.startLocationSearchInput.text?.clear()
        binding.startLocationSearch.error = null
        binding.startLocationSearch.hint = "Enter starting point"

        binding.endDropdownInput.text?.clear()
        binding.endDropdown.error = null

        binding.setVehicle.text?.clear()
        binding.setVehicle.error = null

        binding.setMaxPassengers.text?.clear()
        binding.setMaxPassengers.error = null

        binding.setDate.text?.clear()
        binding.setFemaleOnly.isChecked = false

        viewModel.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 1001
    }
}