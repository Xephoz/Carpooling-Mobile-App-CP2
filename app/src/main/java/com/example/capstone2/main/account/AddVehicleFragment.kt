package com.example.capstone2.main.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.capstone2.databinding.AccountAddVehicleBinding
import com.example.capstone2.model.Vehicle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddVehicleFragment : Fragment() {

    private var _binding: AccountAddVehicleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountAddVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.registerButton.setOnClickListener {
            binding.registerButton.isEnabled = false // Disable button to prevent spamming
            binding.registerProgressBar.visibility = View.VISIBLE
            registerVehicle()
        }
    }

    private fun registerVehicle() {
        // Get input values
        val vehicleNumber = binding.vehicleNumber.text.toString().trim()
        val vehicleBrand = binding.vehicleBrand.text.toString().trim()
        val vehicleModel = binding.vehicleModel.text.toString().trim()
        val vehicleColor = binding.vehicleColor.text.toString().trim()

        // Validate inputs
        if (vehicleNumber.isEmpty() || vehicleBrand.isEmpty() || vehicleModel.isEmpty() || vehicleColor.isEmpty()) {
            showToast("All fields are required")
            enableButton()
            return
        }

        // Ensure values are alphanumeric
        if (vehicleNumber.contains(" ")) {
            binding.vehicleNumber.error = "Number plate cannot contain spaces"
            enableButton()
            return
        }

        if (!vehicleNumber.matches("^[a-zA-Z0-9]+\$".toRegex())) {
            binding.vehicleNumber.error = "Only letters and numbers allowed"
            enableButton()
            return
        }

        if (!vehicleBrand.matches("^[a-zA-Z0-9 ]+\$".toRegex())) {
            binding.vehicleBrand.error = "Only letters, numbers and spaces allowed"
            enableButton()
            return
        }

        if (!vehicleColor.matches("^[a-zA-Z]+\$".toRegex())) {
            binding.vehicleColor.error = "Only letters allowed."
            enableButton()
            return
        }

        // Convert number plate to uppercase
        val formattedNumberPlate = vehicleNumber.uppercase()

        // Capitalize first letter of each word
        val formattedBrand = vehicleBrand.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }}
        val formattedModel = vehicleModel.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }}
        val formattedColor = vehicleColor.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }}

        // Store data in Firestore
        FirebaseFirestore.getInstance().collection("vehicles")
            .whereEqualTo("carNumber", formattedNumberPlate)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Vehicle doesn't exist, proceed with registration
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.let { user ->
                        val vehicle = Vehicle(
                            driverId = user.uid,
                            carNumber = formattedNumberPlate,
                            carModel = formattedModel,
                            carBrand = formattedBrand,
                            color = formattedColor,
                        )

                        // Add vehicle to Firestore
                        FirebaseFirestore.getInstance().collection("vehicles")
                            .add(vehicle)
                            .addOnSuccessListener {
                                enableButton()
                                showToast("Vehicle added successfully")
                                // Exit fragment
                                findNavController().navigateUp()
                            }
                            .addOnFailureListener { e ->
                                enableButton()
                                showToast("Failed to register vehicle: ${e.message}")
                            }
                    } ?: run {
                        enableButton()
                        showToast("User not authenticated")
                    }
                } else {
                    enableButton()
                    binding.vehicleNumber.error = "This number plate is already registered"
                }
            }
            .addOnFailureListener { e ->
                enableButton()
                showToast("Failed to check vehicle: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun enableButton() {
        binding.registerButton.isEnabled = true
        binding.registerProgressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


