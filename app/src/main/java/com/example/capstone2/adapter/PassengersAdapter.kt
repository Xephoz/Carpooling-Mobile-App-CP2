package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.model.UserProfile
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PassengersAdapter : ListAdapter<String, PassengersAdapter.PassengerViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassengerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_passenger, parent, false)
        return PassengerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PassengerViewHolder, position: Int) {
        val passengerId = getItem(position)
        // You'll need to fetch user details here from Firestore
        holder.bind(passengerId)
    }

    inner class PassengerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(passengerId: String) {
            // Fetch user details from Firestore and bind to views
            Firebase.firestore.collection("users").document(passengerId).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(UserProfile::class.java)
                    itemView.findViewById<TextView>(R.id.passengerName).text =
                        "${user?.firstName} ${user?.lastName}"
                }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}