package com.example.capstone2.main.driver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
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
    private val TAG = "ListFragment"

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
        setupRecyclerView()
        setupSwipeRefresh()
        loadUserRides(showLoading = true, isInitialLoad = true)
    }

    private fun setupRecyclerView() {
        ridesAdapter = RidesAdapter { documentId, ride ->
            showRideDetails(documentId, ride)
        }
        binding.ridesRecyclerView.apply {
            adapter = ridesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserRides(showLoading = false, isInitialLoad = false)
        }
    }

    private fun loadUserRides(showLoading: Boolean, isInitialLoad: Boolean) {
        if (showLoading) {
            binding.progressBar.visibility = View.VISIBLE
        }
        binding.emptyStateText.visibility = View.GONE

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
                        binding.emptyStateText.visibility = View.VISIBLE
                        Toast.makeText(context, "No rides found", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.emptyStateText.visibility = View.GONE
                        ridesAdapter.submitList(rides)
                        if (!isInitialLoad) {
                            Toast.makeText(context, "Refreshed list", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w(TAG, "Error getting rides", task.exception)
                    Toast.makeText(context, "Error loading rides", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showRideDetails(documentId: String, ride: Ride) {
        Toast.makeText(context, "Selected: $documentId", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}