package com.example.capstone2.main.passenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.databinding.PassengerSelectedBinding
import com.example.capstone2.adapter.RequestedRidesAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.capstone2.model.Ride
import com.example.capstone2.model.RideRequest
import com.google.firebase.firestore.FieldPath

class SelectedFragment : Fragment() {
    private var _binding: PassengerSelectedBinding? = null
    private val binding get() = _binding!!
    private lateinit var requestedRidesAdapter: RequestedRidesAdapter
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PassengerSelectedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.clearFocus()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupSwipeRefresh()
        loadUserRideRequests(showLoading = true)
    }

    private fun setupRecyclerView() {
        requestedRidesAdapter = RequestedRidesAdapter { rideId ->
            // Handle item click if needed
        }
        binding.ridesRecyclerView.apply {
            adapter = requestedRidesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserRideRequests(showLoading = false)
        }
    }

    private fun loadUserRideRequests(showLoading: Boolean) {
        if (showLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.ridesRecyclerView.visibility = View.GONE
        }

        val currentUserId = auth.currentUser?.uid ?: run {
            hideLoading()
            return
        }

        db.collection("rideRequests")
            .whereEqualTo("passengerId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val rideRequests = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(RideRequest::class.java)?.let { request ->
                        doc.id to request
                    }
                }
                fetchRideDetails(rideRequests)
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(context, "Error loading ride requests", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchRideDetails(rideRequests: List<Pair<String, RideRequest>>) {
        val rideIds = rideRequests.map { it.second.rideId }.distinct()

        if (rideIds.isEmpty()) {
            hideLoading()
            requestedRidesAdapter.submitList(emptyList())
            return
        }

        db.collection("rides")
            .whereIn(FieldPath.documentId(), rideIds)
            .get()
            .addOnSuccessListener { rideSnapshot ->
                val rides = rideSnapshot.documents.associate { doc ->
                    doc.id to doc.toObject(Ride::class.java)
                }

                val items = rideRequests.mapNotNull { (requestId, request) ->
                    rides[request.rideId]?.let { ride ->
                        ride to request.copy(requestId = requestId)
                    }
                }

                requestedRidesAdapter.submitList(items)
                hideLoading()
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(context, "Error loading ride details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.ridesRecyclerView.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}