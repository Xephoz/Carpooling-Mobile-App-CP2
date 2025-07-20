package com.example.capstone2.main.passenger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.adapter.RidesAdapter
import com.example.capstone2.databinding.PassengerBrowseBinding
import com.example.capstone2.extension.distanceTo
import com.example.capstone2.model.Ride
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import com.example.capstone2.R
import com.example.capstone2.viewmodels.BrowseViewModel

class BrowseFragment : Fragment() {
    private var _binding: PassengerBrowseBinding? = null
    private val binding get() = _binding!!
    private lateinit var ridesAdapter: RidesAdapter
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val tag = "BrowseFragment"
    private var currentLocation: GeoPoint? = null
    private val viewModel: BrowseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PassengerBrowseBinding.inflate(inflater, container, false)

        ridesAdapter = RidesAdapter { documentId, _ ->
            showRideDetails(documentId)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchBar()
        setupSortButton()

        // Observe ViewModel state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rides.collect { rides ->
                ridesAdapter.submitList(rides)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentLocation.collect { location ->
                currentLocation = location
            }
        }

        // Only load data if we don't have it already
        if (viewModel.rides.value.isEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                if (hasLocationPermissions()) {
                    getCurrentLocation(shouldLoadRides = true)
                } else {
                    requestLocationPermissions()
                }
                loadRides(showLoading = true, isInitialLoad = true)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    // TODO: Add pagination
    private fun setupRecyclerView() {
        binding.ridesRecyclerView.apply {
            adapter = ridesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            layoutManager?.let {
                if (it is LinearLayoutManager) {
                    it.recycleChildrenOnDetach = true
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (hasLocationPermissions()) {
                getCurrentLocation(shouldLoadRides = true)
            } else {
                showLocationPermissionExplanation()
            }
        }
    }

    private fun showLocationPermissionExplanation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enable Location")
            .setMessage("Allow location access to sort rides by your current location. You may need to enable it through settings if this dialog reappears after selecting 'Allow'")
            .setPositiveButton("Allow") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Later") { _, _ ->
                binding.swipeRefreshLayout.isRefreshing = false
                loadRides(showLoading = false, isInitialLoad = false)
                Toast.makeText(context, "Refreshed list", Toast.LENGTH_SHORT).show()
            }
            .setOnDismissListener {
                binding.swipeRefreshLayout.isRefreshing = false
                loadRides(showLoading = false, isInitialLoad = false)
            }
            .show()
    }

    private fun setupSearchBar() {
        binding.searchBar.setOnClickListener {
            // TODO: Implement search functionality
        }
    }

    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            Toast.makeText(context, "Sort options coming soon", Toast.LENGTH_SHORT).show()
            // TODO: Implement list filter
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestLocationPermissions() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationPermissionExplanation()
        } else {
            locationPermissionRequest.launch(REQUIRED_PERMISSIONS)
        }
    }

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        binding.swipeRefreshLayout.isRefreshing = false
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation(shouldLoadRides = true)
            }
            else -> {
                loadRides(showLoading = false, isInitialLoad = false)
            }
        }
    }

    private fun getCurrentLocation(shouldLoadRides: Boolean = true) {
        if (!hasLocationPermissions()) {
            binding.swipeRefreshLayout.isRefreshing = false
            if (shouldLoadRides) {
                loadRides(showLoading = false, isInitialLoad = false)
            }
            return
        }

        val locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val locationCallback = object : LocationCallback() {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onLocationResult(result: LocationResult) {
                locationClient.removeLocationUpdates(this)
                result.lastLocation?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                    if (shouldLoadRides) {
                        loadRides(showLoading = false, isInitialLoad = false)
                    }
                    return
                }
                getLastKnownLocation(locationClient, shouldLoadRides)
            }
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000
            ).apply {
                setMinUpdateIntervalMillis(5000)
                setMaxUpdates(1)
                setWaitForAccurateLocation(true)
            }.build()

            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            Handler(Looper.getMainLooper()).postDelayed({
                locationClient.removeLocationUpdates(locationCallback)
                getLastKnownLocation(locationClient, shouldLoadRides)
            }, 5000)

        } catch (e: SecurityException) {
            Log.e(tag, "Security Exception when getting geoPoint", e)
            binding.swipeRefreshLayout.isRefreshing = false
            getLastKnownLocation(locationClient, shouldLoadRides)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getLastKnownLocation(client: FusedLocationProviderClient, shouldLoadRides: Boolean) {
        client.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = GeoPoint(it.latitude, it.longitude)
                if (shouldLoadRides) {
                    loadRides(showLoading = false, isInitialLoad = false)
                }
            } ?: run {
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(context, "Unable to get geoPoint", Toast.LENGTH_SHORT).show()
                if (shouldLoadRides) {
                    loadRides(showLoading = false, isInitialLoad = false)
                }
            }
        }.addOnFailureListener { e ->
            binding.swipeRefreshLayout.isRefreshing = false
            Log.e(tag, "Error getting last known geoPoint", e)
            if (shouldLoadRides) {
                loadRides(showLoading = false, isInitialLoad = false)
            }
        }
    }

    private fun loadRides(showLoading: Boolean, isInitialLoad: Boolean, showRefreshToast: Boolean = false) {
        if (!isAdded || isDetached) return

        if (showLoading) {
            viewModel.setLoading(true)
        }

        val currentUserId = auth.currentUser?.uid ?: run {
            viewModel.setLoading(false)
            binding.swipeRefreshLayout.isRefreshing = false
            return
        }

        if (!isInitialLoad) {
            ridesAdapter.submitList(emptyList())
        }

        val localLocation = currentLocation

        db.collection("rides")
            .whereNotEqualTo("driverId", currentUserId)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnCompleteListener { task ->
                viewModel.setLoading(false)
                val wasRefreshing = binding.swipeRefreshLayout.isRefreshing
                binding.swipeRefreshLayout.isRefreshing = false

                if (task.isSuccessful) {
                    val rides = task.result?.documents?.mapNotNull { doc ->
                        doc.toObject(Ride::class.java)?.let { ride ->
                            doc.id to ride
                        }
                    } ?: emptyList()

                    val sortedRides = if (localLocation != null) {
                        rides.map { (id, ride) ->
                            RideWithDistance(
                                documentId = id,
                                ride = ride,
                                distance = localLocation.distanceTo(ride.startLocation.geoPoint)
                            )
                        }.sortedWith(compareBy<RideWithDistance> { it.distance }
                            .thenBy { it.ride.departureTime })
                            .map { it.documentId to it.ride }
                    } else {
                        rides.sortedBy { (_, ride) -> ride.departureTime }
                    }

                    // Save to ViewModel
                    viewModel.setRides(sortedRides)
                    viewModel.setCurrentLocation(localLocation)

                    if (sortedRides.isEmpty()) {
                        ridesAdapter.submitList(emptyList())
                        Toast.makeText(context, "No available rides found", Toast.LENGTH_SHORT).show()
                    } else {
                        ridesAdapter.submitList(sortedRides)
                        // Show toast if it was a refresh action OR explicitly requested
                        if (wasRefreshing || showRefreshToast) {
                            Toast.makeText(context, "Refreshed list", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w(tag, "Error getting rides", task.exception)
                    Toast.makeText(context, "Error loading rides", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private data class RideWithDistance(
        val documentId: String,
        val ride: Ride,
        val distance: Float
    )

    private fun showRideDetails(rideId: String) {
        val bundle = bundleOf("rideId" to rideId)
        findNavController().navigate(R.id.rideDetailsFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}