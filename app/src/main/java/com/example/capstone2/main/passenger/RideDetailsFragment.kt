package com.example.capstone2.main.passenger

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.adapter.PassengersAdapter
import com.example.capstone2.databinding.PassengerRideDetailsBinding
import com.example.capstone2.model.Gender
import com.example.capstone2.model.RequestStatus
import com.example.capstone2.model.Ride
import com.example.capstone2.model.RideRequest
import com.example.capstone2.model.UserProfile
import com.example.capstone2.model.Vehicle
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RideDetailsFragment : Fragment() {
    private var _binding: PassengerRideDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var passengersAdapter: PassengersAdapter
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
        _binding = PassengerRideDetailsBinding.inflate(inflater, container, false)
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
        setupPassengersRecyclerView()
        setupRequestButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadRideDetails() {
        // Show loading state
        binding.requestProgressBar.visibility = View.VISIBLE
        binding.requestButton.isEnabled = false

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
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load ride: ${e.message}", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
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

        binding.passengerText.text = "[${ride.passengers.size}/${ride.maxPassengers}]"
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
                            binding.requestProgressBar.visibility = View.GONE
                            binding.requestButton.isEnabled = true
                            binding.requestButton.text = "Invalid Ride"
                            Toast.makeText(requireContext(), "Vehicle not found", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        vehicle = vehicleSnapshot.toObject(Vehicle::class.java) ?: return@addOnSuccessListener

                        // Update vehicle UI
                        binding.carNumber.text = vehicle.carNumber
                        binding.carModel.text = "${vehicle.carBrand} ${vehicle.carModel} - ${vehicle.color}"

                        // Enable request button
                        binding.requestProgressBar.visibility = View.GONE
                        binding.requestButton.isEnabled = true
                    }
                    .addOnFailureListener { e ->
                        binding.carNumber.text = "Unknown Vehicle Number"
                        binding.carModel.visibility = View.GONE
                        binding.requestProgressBar.visibility = View.GONE
                        binding.requestButton.isEnabled = true
                        binding.requestButton.text = "Invalid Ride"
                        Toast.makeText(requireContext(), "Failed to load vehicle details", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.driverName.text = "Unknown Driver"
                binding.requestProgressBar.visibility = View.GONE
                binding.requestButton.isEnabled = true
                binding.requestButton.text = "Invalid Ride"
                Toast.makeText(requireContext(), "Failed to load driver details", Toast.LENGTH_SHORT).show()
            }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("rideRequests")
                .whereEqualTo("rideId", rideId)
                .whereEqualTo("passengerId", currentUser.uid)
                .whereEqualTo("status", RequestStatus.PENDING)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        binding.statusIcon.visibility = View.VISIBLE
                        binding.statusText.visibility = View.VISIBLE
                        binding.requestButton.apply {
                            text = "Request Pending"
                            isEnabled = false
                        }
                    }
                }
        }

        passengersAdapter.submitList(ride.passengers)
    }

    private fun setupRequestButton() {
        binding.requestButton.setOnClickListener {
            if (binding.requestButton.text == "Invalid Ride") {
                Toast.makeText(requireContext(), "This ride cannot be joined", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser ?: run {
                Toast.makeText(requireContext(), "You need to be logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if ride is full
            if (ride.passengers.size >= ride.maxPassengers) {
                Toast.makeText(requireContext(), "This ride is already full", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if user is already a passenger
            if (ride.passengers.contains(currentUser.uid)) {
                Toast.makeText(requireContext(), "You've already joined this ride", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check female-only restriction
            if (ride.femaleOnly) {
                db.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { snapshot ->
                        val userProfile = snapshot.toObject(UserProfile::class.java)
                        if (userProfile?.gender != Gender.FEMALE) {
                            Toast.makeText(requireContext(), "This ride is for females only", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        checkExistingRequest()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to verify gender", Toast.LENGTH_SHORT).show()
                    }
            } else {
                checkExistingRequest()
            }
        }
    }

    private fun checkExistingRequest() {
        val currentUser = auth.currentUser ?: return

        db.collection("rideRequests")
            .whereEqualTo("rideId", rideId)
            .whereEqualTo("passengerId", currentUser.uid)
            .whereEqualTo("status", RequestStatus.PENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    createRideRequest()
                } else {
                    binding.statusIcon.visibility = View.VISIBLE
                    binding.statusText.visibility = View.VISIBLE
                    binding.requestButton.text = "Request Pending"
                    Toast.makeText(requireContext(), "You already have a pending request for this ride", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to check existing requests", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createRideRequest() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.requestProgressBar.visibility = View.VISIBLE
        binding.requestButton.isEnabled = false

        val requestId = db.collection("rideRequests").document().id
        val rideRequest = RideRequest(
            requestId = requestId,
            rideId = rideId,
            passengerId = currentUser.uid,
            driverId = ride.driverId,
            status = RequestStatus.PENDING,
            requestedAt = Timestamp.now()
        )

        db.collection("rideRequests").document(requestId).set(rideRequest)
            .addOnSuccessListener {
                binding.requestProgressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Request sent successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                binding.requestProgressBar.visibility = View.GONE
                binding.requestButton.isEnabled = true
                Toast.makeText(requireContext(), "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}