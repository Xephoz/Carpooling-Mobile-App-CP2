package com.example.capstone2.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.capstone2.R
import com.example.capstone2.databinding.AuthRegisterBinding
import com.example.capstone2.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private var _binding: AuthRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val registerProgressBar = binding.registerProgressBar

        binding.registerButton.setOnClickListener {
            binding.registerButton.isEnabled = false // Disable button to prevent spamming
            registerProgressBar.visibility = View.VISIBLE

            val firstName = binding.firstName.text.toString().trim()
            val lastName = binding.lastName.text.toString().trim()
            val email = binding.registerEmail.text.toString().trim()
            val password = binding.registerPassword.text.toString()
            val confirmPassword = binding.registerConfirm.text.toString()

            if (firstName.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                // Email domain restriction
                if (!email.endsWith("@imail.sunway.edu.my")) {
                    Toast.makeText(requireContext(), "Only @imail.sunway.edu.my emails are allowed", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                    registerProgressBar.visibility = View.GONE
                    return@setOnClickListener
                }

                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = firebaseAuth.currentUser
                                user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                                    if (emailTask.isSuccessful) {
                                        // Save to Firestore
                                        val uid = user.uid
                                        val userProfile = UserProfile(
                                            firstName = firstName,
                                            lastName = lastName,
                                            email = email,
                                            uid = uid
                                        )
                                        db.collection("users").document(uid).set(userProfile)
                                            .addOnSuccessListener {
                                                Toast.makeText(requireContext(), "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                                                firebaseAuth.signOut()
                                                // Navigate back to LoginFragment
                                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(requireContext(), "Failed to save profile info.", Toast.LENGTH_SHORT).show()
                                                binding.registerButton.isEnabled = true
                                                registerProgressBar.visibility = View.GONE
                                            }
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                        binding.registerButton.isEnabled = true
                                        registerProgressBar.visibility = View.GONE
                                    }
                                }
                            } else {
                                val errorMessage = when (val exception = task.exception) {
                                    is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
                                    is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                                    is FirebaseAuthUserCollisionException -> "This email is already registered."
                                    else -> exception?.localizedMessage ?: "Signup failed. Please try again."
                                }
                                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                                binding.registerButton.isEnabled = true
                                registerProgressBar.visibility = View.GONE
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                    registerProgressBar.visibility = View.GONE
                }
            } else {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
                registerProgressBar.visibility = View.GONE
            }
        }

        binding.loginRedirectText.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
