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

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val registerProgressBar = findViewById<ProgressBar>(R.id.registerProgressBar)

        binding.registerButton.setOnClickListener {
            binding.registerButton.isEnabled = false // Disable button to prevent spamming
            registerProgressBar.visibility = View.VISIBLE

            val email = binding.registerEmail.text.toString()
            val password = binding.registerPassword.text.toString()
            val confirmPassword = binding.registerConfirm.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
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
                                user?.sendEmailVerification()
                                    ?.addOnCompleteListener { emailTask ->
                                        if (emailTask.isSuccessful) {
                                            Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                                            firebaseAuth.signOut() // Sign out so user must verify email first
                                            val intent = Intent(this, LoginActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show()
                                            binding.registerButton.isEnabled = true // Re-enable button
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
                                binding.registerButton.isEnabled = true // Re-enable button
                                registerProgressBar.visibility = View.GONE

                            }
                        }
                } else {
                    Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true // Re-enable button
                    registerProgressBar.visibility = View.GONE
                }
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true // Re-enable button
                registerProgressBar.visibility = View.GONE
            }
        }

        binding.loginRedirectText.setOnClickListener{
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
}