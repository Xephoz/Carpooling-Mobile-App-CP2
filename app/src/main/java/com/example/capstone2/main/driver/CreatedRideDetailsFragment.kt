package com.example.capstone2.main.driver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.R
import com.example.capstone2.adapter.PassengersAdapter
import com.example.capstone2.adapter.RequestsAdapter
import com.example.capstone2.databinding.DriverRideDetailsBinding
import com.example.capstone2.model.RequestStatus
import com.example.capstone2.model.Ride
import com.example.capstone2.model.RideRequest
import com.example.capstone2.model.UserProfile
import com.example.capstone2.model.Vehicle
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CreatedRideDetailsFragment : Fragment() {
    private var _binding: DriverRideDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var passengersAdapter: PassengersAdapter
    private lateinit var requestsAdapter: RequestsAdapter
    private val db = Firebase.firestore

    private lateinit var rideId: String
    private lateinit var ride: Ride
    private lateinit var driverProfile: UserProfile
    private lateinit var vehicle: Vehicle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DriverRideDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get ride ID from navigation arguments
        rideId = arguments?.getString("rideId") ?: run {
            Toast.makeText(requireContext(), "Invalid ride", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        loadRideDetails()
        setupRequestsRecyclerView()
        setupPassengersRecyclerView()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadRideDetails() {
        // Fetch ride details
        db.collection("rides").document(rideId).get()
            .addOnSuccessListener { rideSnapshot ->
                if (!rideSnapshot.exists()) {
                    Toast.makeText(requireContext(), "Ride not found", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@addOnSuccessListener
                }

                ride = rideSnapshot.toObject(Ride::class.java) ?: run {
                    Toast.makeText(requireContext(), "Invalid ride data", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@addOnSuccessListener
                }

                updateRideUI()
                fetchDriverAndVehicleDetails()
                loadRequests()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load ride: ${e.message}", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
    }

    private fun setupRequestsRecyclerView() {
        binding.requestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            requestsAdapter = RequestsAdapter().apply {
                onRequestAction = { rideRequest, newStatus, onComplete ->
                    // Disable all buttons immediately
                    disableAllButtons()
                    handleRequestAction(rideRequest, newStatus, onComplete)
                }
            }
            adapter = requestsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupPassengersRecyclerView() {
        binding.passengersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            passengersAdapter = PassengersAdapter()
            adapter = passengersAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun updateRideUI() {
        binding.startLocation.text = when {
            ride.startLocation.fullAddress.startsWith(ride.startLocation.displayName) ->
                ride.startLocation.fullAddress
            else -> listOf(ride.startLocation.displayName, ride.startLocation.fullAddress)
                .filter { it.isNotEmpty() }
                .joinToString(" - ")
        }
        binding.endLocation.text = when {
            ride.endLocation.fullAddress.startsWith(ride.endLocation.displayName) ->
                ride.endLocation.fullAddress
            else -> listOf(ride.endLocation.displayName, ride.endLocation.fullAddress)
                .filter { it.isNotEmpty() }
                .joinToString(" - ")
        }
        val date = ride.departureTime.toDate()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        binding.dateText.text = dateFormat.format(date)
            .replace(" AM", " AM")
            .replace(" PM", " PM")

        binding.passengerCount.text = "[${ride.passengers.size}/${ride.maxPassengers}]"

        // Update passengers RecyclerView visibility
        if (ride.passengers.isNotEmpty()) {
            binding.passengersRecyclerView.visibility = View.VISIBLE
            binding.passengerEmpty.visibility = View.GONE
        } else {
            binding.passengersRecyclerView.visibility = View.GONE
            binding.passengerEmpty.visibility = View.VISIBLE
        }

        passengersAdapter.submitList(ride.passengers)
    }

    private fun fetchDriverAndVehicleDetails() {
        // Fetch driver profile
        db.collection("users").document(ride.driverId).get()
            .addOnSuccessListener { userSnapshot ->
                driverProfile = userSnapshot.toObject(UserProfile::class.java) ?: return@addOnSuccessListener

                // Update driver UI
                binding.driverName.text = "${driverProfile.firstName} ${driverProfile.lastName}"

                // Fetch vehicle details
                db.collection("vehicles").document(ride.vehicleId).get()
                    .addOnSuccessListener { vehicleSnapshot ->
                        if (!vehicleSnapshot.exists()) {
                            binding.carNumber.text = "Unknown Vehicle Number"
                            binding.carModel.text = "Unknown Vehicle Model"
                            Toast.makeText(requireContext(), "Vehicle not found", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        vehicle = vehicleSnapshot.toObject(Vehicle::class.java) ?: return@addOnSuccessListener

                        // Update vehicle UI
                        binding.carNumber.text = vehicle.carNumber
                        binding.carModel.text = "${vehicle.carBrand} ${vehicle.carModel} - ${vehicle.color}"
                    }
                    .addOnFailureListener { e ->
                        binding.carNumber.text = "Unknown Vehicle Number"
                        binding.carModel.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to load vehicle details", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.driverName.text = "Unknown Driver"
                Toast.makeText(requireContext(), "Failed to load driver details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRequests() {
        db.collection("rideRequests")
            .whereEqualTo("rideId", rideId)
            .whereEqualTo("status", RequestStatus.PENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val requests = querySnapshot.toObjects(RideRequest::class.java)
                Log.d("Requests", "Loaded ${requests.size} requests")
                requestsAdapter.submitList(requests)

                // Check ride full status
                if (ride.passengers.size >= ride.maxPassengers) {
                    binding.requestsFull.visibility = View.VISIBLE
                    binding.requestsEmpty.visibility = View.GONE
                    binding.requestsRecyclerView.visibility = View.GONE
                } else {
                    if (requests.isEmpty()) {
                        binding.requestsFull.visibility = View.GONE
                        binding.requestsEmpty.visibility = View.VISIBLE
                        binding.requestsRecyclerView.visibility = View.GONE
                    } else {
                        binding.requestsFull.visibility = View.GONE
                        binding.requestsEmpty.visibility = View.GONE
                        binding.requestsRecyclerView.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load requests: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Requests", "Error loading requests", e)
                // On error, still show empty state
                binding.requestsFull.visibility = View.GONE
                binding.requestsRecyclerView.visibility = View.GONE
                binding.requestsEmpty.visibility = View.VISIBLE
            }
    }

    private fun handleRequestAction(
        rideRequest: RideRequest,
        newStatus: RequestStatus,
        onComplete: () -> Unit
    ) {
        val batch = db.batch()
        val requestRef = db.collection("rideRequests").document(rideRequest.requestId)
        batch.update(requestRef, "status", newStatus.name)

        if (newStatus == RequestStatus.CONFIRMED) {
            val rideRef = db.collection("rides").document(rideId)
            batch.update(rideRef, "passengers", FieldValue.arrayUnion(rideRequest.passengerId))

            val updatedPassengerCount = ride.passengers.size + 1
            if (updatedPassengerCount >= ride.maxPassengers) {
                db.collection("rideRequests")
                    .whereEqualTo("rideId", rideId)
                    .whereEqualTo("status", RequestStatus.PENDING.name)
                    .whereNotEqualTo("__name__", rideRequest.requestId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { doc ->
                            batch.update(doc.reference, "status", RequestStatus.REJECTED.name)
                        }
                        commitBatch(batch, onComplete)
                    }
                    .addOnFailureListener { e ->
                        commitBatch(batch, onComplete)
                    }
            } else {
                db.collection("rideRequests")
                    .whereEqualTo("rideId", rideId)
                    .whereEqualTo("passengerId", rideRequest.passengerId)
                    .whereEqualTo("status", RequestStatus.PENDING.name)
                    .whereNotEqualTo("__name__", rideRequest.requestId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { doc ->
                            batch.update(doc.reference, "status", RequestStatus.REJECTED.name)
                        }
                        commitBatch(batch, onComplete)
                    }
            }
        } else {
            commitBatch(batch, onComplete)
        }
    }

    private fun commitBatch(batch: WriteBatch, onComplete: () -> Unit) {
        batch.commit()
            .addOnSuccessListener {
                val updatedPassengerCount = ride.passengers.size + 1
                val isLastPassenger = updatedPassengerCount >= ride.maxPassengers

                // Combine messages if it's the last passenger
                val message = if (isLastPassenger) {
                    "Request accepted successfully.\nRemaining requests have been rejected automatically."
                } else {
                    "Request accepted successfully."
                }

                showSuccessSnackbar(message)
                refreshData()
                onComplete()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to update request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete()
            }
    }

    private fun showSuccessSnackbar(message: String) {
        // Set duration to 5 seconds (5000ms)
        val durationMs = 5000
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE).apply {
            duration = durationMs

            // Add bottom margin
            view.apply {
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.snackbar_margin_bottom)
                }
                setBackgroundColor(ContextCompat.getColor(context, R.color.confirmed_request))

                // Enable multi-line text
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
                    maxLines = 3 // Allow up to 3 lines of text
                }
            }

            // Set action with close icon
            setAction(android.R.string.ok) { dismiss() }
            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        }

        snackbar.show()
    }

    private fun refreshData() {
        loadRideDetails()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}