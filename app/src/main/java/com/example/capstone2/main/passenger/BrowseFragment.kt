package com.example.capstone2.main.passenger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.adapter.RidesAdapter
import com.example.capstone2.databinding.PassengerBrowseBinding
import com.example.capstone2.extension.distanceTo
import com.example.capstone2.model.Ride
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BrowseFragment : Fragment() {
    private var _binding: PassengerBrowseBinding? = null
    private val binding get() = _binding!!
    private lateinit var ridesAdapter: RidesAdapter
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val tag = "BrowseFragment"
    private var currentLocation: GeoPoint? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PassengerBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchBar()
        setupSortButton()
        // Check permissions before getting location
        if (hasLocationPermissions()) {
            getCurrentLocation(shouldLoadRides = true)
            loadRides(showLoading = true, isInitialLoad = true)
        } else {
            requestLocationPermissions()
        }
    }

    private fun setupRecyclerView() {
        ridesAdapter = RidesAdapter { documentId, ride ->
            showRideDetails(documentId, ride)
        }
        binding.ridesRecyclerView.apply {
            adapter = ridesAdapter
            layoutManager = LinearLayoutManager(requireContext())
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
            .setMessage("Allow location access to see nearby rides first.")
            .setPositiveButton("Allow") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Later") { _, _ ->
                binding.swipeRefreshLayout.isRefreshing = false
                loadRides(showLoading = false, isInitialLoad = false)
            }
            .setOnDismissListener {
                binding.swipeRefreshLayout.isRefreshing = false
                loadRides(showLoading = false, isInitialLoad = false)
            }
            .show()
    }

    private fun setupSearchBar() {
        binding.searchBar.setOnClickListener {
            // Implement search functionality here if needed
        }
    }

    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            // Implement sorting options if needed
            Toast.makeText(context, "Sort options coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestLocationPermissions() {
        requestPermissions(REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                binding.swipeRefreshLayout.isRefreshing = false
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    getCurrentLocation(shouldLoadRides = true)
                } else {
                    loadRides(showLoading = false, isInitialLoad = false)
                }
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
        try {
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = GeoPoint(it.latitude, it.longitude)
                        if (shouldLoadRides) {
                            loadRides(showLoading = false, isInitialLoad = false)
                        }
                    } ?: run {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
                        if (shouldLoadRides) {
                            loadRides(showLoading = false, isInitialLoad = false)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    binding.swipeRefreshLayout.isRefreshing = false
                    Log.e(tag, "Error getting location", e)
                    if (shouldLoadRides) {
                        loadRides(showLoading = false, isInitialLoad = false)
                    }
                }
        } catch (e: SecurityException) {
            binding.swipeRefreshLayout.isRefreshing = false
            Log.e(tag, "Security Exception when getting location", e)
            if (shouldLoadRides) {
                loadRides(showLoading = false, isInitialLoad = false)
            }
        }
    }

    private fun loadRides(showLoading: Boolean, isInitialLoad: Boolean) {
        if (showLoading && !binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.visibility = View.VISIBLE
        }

        val currentUserId = auth.currentUser?.uid ?: run {
            binding.progressBar.visibility = View.GONE
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
                binding.progressBar.visibility = View.GONE
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

                    if (sortedRides.isEmpty()) {
                        ridesAdapter.submitList(emptyList())
                        Toast.makeText(context, "No available rides found", Toast.LENGTH_SHORT).show()
                    } else {
                        ridesAdapter.submitList(sortedRides)
                        if (wasRefreshing) {  // Only show if it was a refresh action
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

    private fun showRideDetails(documentId: String, ride: Ride) {
        Toast.makeText(context, "Selected ride to: ${ride.endLocation.displayName}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
