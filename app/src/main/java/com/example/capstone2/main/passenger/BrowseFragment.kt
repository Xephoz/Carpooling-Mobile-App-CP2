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
import com.example.capstone2.model.Ride
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp

class BrowseFragment : Fragment() {
    private var _binding: PassengerBrowseBinding? = null
    private val binding get() = _binding!!
    private lateinit var ridesAdapter: RidesAdapter
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val tag = "BrowseFragment"

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

    private fun loadRides(showLoading: Boolean, isInitialLoad: Boolean) {
        if (showLoading) {
            binding.progressBar.visibility = View.VISIBLE
        }

        val currentUserId = auth.currentUser?.uid ?: return
        val currentTime = Timestamp.now()

        db.collection("rides")
            .whereNotEqualTo("driverId", currentUserId) // Exclude current user's rides
            .whereGreaterThan("departureTime", currentTime) // Only future rides
            .orderBy("departureTime", Query.Direction.ASCENDING) // Order by nearest first
            .get()
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false

                if (task.isSuccessful) {
                    val rides = task.result?.documents?.mapNotNull { doc ->
                        doc.toObject(Ride::class.java)?.let { ride ->
                            doc.id to ride
                        }
                    } ?: emptyList()

                    if (rides.isEmpty()) {
                        Toast.makeText(context, "No upcoming rides found", Toast.LENGTH_SHORT).show()
                    } else {
                        ridesAdapter.submitList(rides)
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

    private fun showRideDetails(documentId: String, ride: Ride) {
        // Implement navigation to ride details or show a dialog
        Toast.makeText(context, "Selected ride to: ${ride.endLocation.displayName}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}