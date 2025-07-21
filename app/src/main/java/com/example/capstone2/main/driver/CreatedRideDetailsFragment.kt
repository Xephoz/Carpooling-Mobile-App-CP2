package com.example.capstone2.main.driver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.adapter.PassengersAdapter
import com.example.capstone2.adapter.RequestsAdapter
import com.example.capstone2.databinding.DriverRideDetailsBinding
import com.example.capstone2.model.RequestStatus
import com.example.capstone2.model.Ride
import com.example.capstone2.model.RideRequest
import com.example.capstone2.model.UserProfile
import com.example.capstone2.model.Vehicle
import com.google.firebase.auth.FirebaseAuth
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
    private val auth = FirebaseAuth.getInstance()

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

                // Update UI with ride details
                updateRideUI()

                // Fetch driver and vehicle details
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
                onRequestAction = { rideRequest, newStatus ->
                    handleRequestAction(rideRequest, newStatus)
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
        binding.startLocation.text = ride.startLocation.displayName
        binding.endLocation.text = ride.endLocation.displayName

        val date = ride.departureTime.toDate()

        val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }

        binding.dateText.text = dateFormat.format(date)
            .replace(" AM", " AM")
            .replace(" PM", " PM")

        binding.passengerCount.text = "[${ride.passengers.size}/${ride.maxPassengers}]"
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
        passengersAdapter.submitList(ride.passengers)
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

                // Show/hide section based on requests
                binding.requestsContainer.visibility = if (requests.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load requests: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Requests", "Error loading requests", e)
            }
    }

    private fun handleRequestAction(rideRequest: RideRequest, newStatus: RequestStatus) {
        val batch = db.batch()

        // 1. Update the request status
        val requestRef = db.collection("rideRequests").document(rideRequest.requestId)
        batch.update(requestRef, "status", newStatus.name)

        if (newStatus == RequestStatus.CONFIRMED) {
            // 2. Add passenger to ride if accepted
            val rideRef = db.collection("rides").document(rideId)
            batch.update(rideRef, "passengers", FieldValue.arrayUnion(rideRequest.passengerId))

            // 3. Reject all other pending requests from this user for the same ride
            db.collection("rideRequests")
                .whereEqualTo("rideId", rideId)
                .whereEqualTo("passengerId", rideRequest.passengerId)
                .whereEqualTo("status", RequestStatus.PENDING.name)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    querySnapshot.documents.forEach { doc ->
                        batch.update(doc.reference, "status", RequestStatus.REJECTED.name)
                    }
                    commitBatch(batch)
                }
        } else {
            commitBatch(batch)
        }
    }

    private fun commitBatch(batch: WriteBatch) {
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Request updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                refreshData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to update request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun refreshData() {
        // Reload both ride details and requests
        loadRideDetails()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}