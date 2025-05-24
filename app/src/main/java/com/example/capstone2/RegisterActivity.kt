package com.example.capstone2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone2.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.example.capstone2.model.UserProfile


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val registerProgressBar = findViewById<ProgressBar>(R.id.registerProgressBar)

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
                    Toast.makeText(this, "Only @imail.sunway.edu.my emails are allowed", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true // Re-enable button
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
                                                Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                                                firebaseAuth.signOut()
                                                startActivity(Intent(this, LoginActivity::class.java))
                                                finish()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "Failed to save profile info.", Toast.LENGTH_SHORT).show()
                                                binding.registerButton.isEnabled = true
                                                registerProgressBar.visibility = View.GONE
                                            }
                                    } else {
                                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                                binding.registerButton.isEnabled = true
                                registerProgressBar.visibility = View.GONE
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                    registerProgressBar.visibility = View.GONE
                }
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
                registerProgressBar.visibility = View.GONE
            }
        }

        binding.loginRedirectText.setOnClickListener{
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
}