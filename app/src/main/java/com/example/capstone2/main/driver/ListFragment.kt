package com.example.capstone2.main.driver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.R
import com.example.capstone2.adapter.RidesAdapter
import com.example.capstone2.databinding.DriverListBinding
import com.example.capstone2.model.Ride
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ListFragment : Fragment() {
    private var _binding: DriverListBinding? = null
    private val binding get() = _binding!!
    private lateinit var ridesAdapter: RidesAdapter
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val tag = "ListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DriverListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.clearFocus()
        setupRecyclerView()
        setupSwipeRefresh()
        setupcreateRideButton()
        loadUserRides(showLoading = true, isInitialLoad = true)
    }

    private fun setupRecyclerView() {
        ridesAdapter = RidesAdapter { documentId, ride ->
            showRideDetails(documentId)
        }
        binding.ridesRecyclerView.apply {
            adapter = ridesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserRides(showLoading = false, isInitialLoad = false)
        }
    }

    private fun setupcreateRideButton() {
        binding.createRideButton.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_createFragment)
        }
    }

    private fun loadUserRides(showLoading: Boolean, isInitialLoad: Boolean) {
        if (showLoading) {
            binding.progressBar.visibility = View.VISIBLE
        }

        // Clear existing data immediately when refreshing
        if (!isInitialLoad) {
            ridesAdapter.submitList(emptyList())
        }

        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("rides")
            .whereEqualTo("driverId", currentUserId)
            .orderBy("departureTime", Query.Direction.ASCENDING)
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
                        ridesAdapter.submitList(emptyList()) // Ensure empty state
                        Toast.makeText(context, "No rides found", Toast.LENGTH_SHORT).show()
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
    
    private fun showRideDetails(documentId: String) {
        Toast.makeText(context, "Selected: $documentId", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}