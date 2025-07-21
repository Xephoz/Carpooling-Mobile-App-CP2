package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.model.RequestStatus
import com.example.capstone2.model.UserProfile
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.capstone2.model.RideRequest

class RequestsAdapter : ListAdapter<RideRequest, RequestsAdapter.RequestsViewHolder>(DiffCallback()) {

    var onRequestAction: ((RideRequest, RequestStatus) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_passenger, parent, false)
        return RequestsViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequestsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val passengerName: TextView = itemView.findViewById(R.id.passengerName)
        private val acceptButton: ImageView = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: ImageView = itemView.findViewById(R.id.rejectButton)

        fun bind(rideRequest: RideRequest) {
            acceptButton.visibility = View.VISIBLE
            rejectButton.visibility = View.VISIBLE

            // Load passenger details
            Firebase.firestore.collection("users").document(rideRequest.passengerId).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(UserProfile::class.java)
                    passengerName.text = "${user?.firstName} ${user?.lastName}"
                }

            acceptButton.setOnClickListener {
                onRequestAction?.invoke(rideRequest, RequestStatus.CONFIRMED)
            }

            rejectButton.setOnClickListener {
                onRequestAction?.invoke(rideRequest, RequestStatus.REJECTED)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RideRequest>() {
        override fun areItemsTheSame(oldItem: RideRequest, newItem: RideRequest) =
            oldItem.requestId == newItem.requestId

        override fun areContentsTheSame(oldItem: RideRequest, newItem: RideRequest) =
            oldItem == newItem
    }
}