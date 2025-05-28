package com.example.capstone2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.capstone2.databinding.ActivityProfileBinding
import com.example.capstone2.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.topBar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.topBar.title = "Account"
        binding.topBar.setNavigationIcon(R.drawable.baseline_arrow_back_30)

        binding.topBar.setNavigationOnClickListener {
            finish()
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show()
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
                    }
                } else {
                    Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load profile: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}

