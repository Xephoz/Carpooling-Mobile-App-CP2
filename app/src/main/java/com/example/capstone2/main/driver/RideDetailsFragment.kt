package com.example.capstone2.main.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.capstone2.adapter.PassengersAdapter
import com.example.capstone2.databinding.DriverRideDetailsBinding
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
    private var _binding: DriverRideDetailsBinding? = null
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}