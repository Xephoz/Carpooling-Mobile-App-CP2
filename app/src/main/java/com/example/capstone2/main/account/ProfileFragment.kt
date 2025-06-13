package com.example.capstone2.main.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.capstone2.databinding.AccountProfileBinding
import com.example.capstone2.model.Gender
import com.example.capstone2.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: AccountProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserProfile()
        setupGenderDropdown()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "User not signed in.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userProfile = document.toObject(UserProfile::class.java)
                    if (userProfile != null) {
                        binding.firstNameBox.setText(userProfile.firstName)
                        binding.lastNameBox.setText(userProfile.lastName)
                        binding.emailBox.setText(userProfile.email)
                        binding.phoneBox.setText(userProfile.phoneNumber ?: "")

                        val genderText = when (userProfile.gender) {
                            Gender.MALE -> "Male"
                            Gender.FEMALE -> "Female"
                            Gender.OTHER -> "Other"
                            else -> ""
                        }

                        binding.genderBox.setText(genderText, false)
                    }

                } else {
                    Toast.makeText(requireContext(), "User profile not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load profile: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupGenderDropdown() {
        val genderOptions = listOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        binding.genderBox.setAdapter(adapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
