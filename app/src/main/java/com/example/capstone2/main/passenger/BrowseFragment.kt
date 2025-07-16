package com.example.capstone2.main.passenger

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.adapter.RidesAdapter
import com.example.capstone2.databinding.PassengerBrowseBinding
import com.example.capstone2.extension.distanceTo
import com.example.capstone2.model.Ride
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
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
        getCurrentLocation()
        loadRides(showLoading = true, isInitialLoad = true)
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
            loadRides(showLoading = false, isInitialLoad = false)
        }
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

    private fun getCurrentLocation() {
        val locationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        try {
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = GeoPoint(it.latitude, it.longitude)
                        loadRides(showLoading = false, isInitialLoad = true)
                    } ?: run {
                        // Handle case where location is null
                        Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
                        loadRides(showLoading = false, isInitialLoad = true)
                    }
                }
        } catch (e: SecurityException) {
            // Handle permission exception
        }
    }

    private fun loadRides(showLoading: Boolean, isInitialLoad: Boolean) {
        if (showLoading) {
            binding.progressBar.visibility = View.VISIBLE
        }

        val currentUserId = auth.currentUser?.uid ?: return
        val currentLocation = currentLocation ?: GeoPoint(0.0, 0.0)

        if (!isInitialLoad) {
            ridesAdapter.submitList(emptyList())
        }

        db.collection("rides")
            .whereNotEqualTo("driverId", currentUserId)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false

                if (task.isSuccessful) {
                    val ridesWithDistance = task.result?.documents?.mapNotNull { doc ->
                        doc.toObject(Ride::class.java)?.let { ride ->
                            RideWithDistance(
                                documentId = doc.id,
                                ride = ride,
                                distance = currentLocation.distanceTo(ride.startLocation.geoPoint)
                            )
                        }
                    }

                    val sortedRides = ridesWithDistance
                        ?.sortedBy { it.distance }
                        ?.map { it.documentId to it.ride }
                        ?: emptyList()

                    if (sortedRides.isEmpty()) {
                        ridesAdapter.submitList(emptyList()) // Ensure empty state
                        Toast.makeText(context, "No available rides found", Toast.LENGTH_SHORT).show()
                    } else {
                        ridesAdapter.submitList(sortedRides)
                        if (!isInitialLoad) {
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