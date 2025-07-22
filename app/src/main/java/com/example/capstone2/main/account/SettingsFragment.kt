package com.example.capstone2.main.account

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.capstone2.R
import com.example.capstone2.auth.AuthActivity
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.account_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialTextView>(R.id.logoutButton).setOnClickListener {
            showLogoutDialog()
        }

        view.findViewById<View>(R.id.profileContainer).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_profileFragment)
        }

        view.findViewById<View>(R.id.addVehicleContainer).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_addVehicleFragment)
        }

        // TODO: Navigate to view vehicles
        view.findViewById<View>(R.id.myVehiclesContainer).setOnClickListener {
            Toast.makeText(context, "TODO: Manage vehicles", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.account_logout, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.logoutButton).setOnClickListener {
            signOut()
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun signOut() {
        try {
            auth.signOut()
            val intent = Intent(requireActivity(), AuthActivity::class.java).apply {
                // Clear the back stack and create a new task
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)

            requireActivity().finish()
        } catch (e: Exception) {
            // Show error message to user
            Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


}