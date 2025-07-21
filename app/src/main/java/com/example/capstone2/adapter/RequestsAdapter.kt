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

    var onRequestAction: ((RideRequest, RequestStatus, () -> Unit) -> Unit)? = null
    private val viewHolders = mutableListOf<RequestsViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_passenger, parent, false)
        return RequestsViewHolder(view).also {
            viewHolders.add(it)
        }
    }

    override fun onBindViewHolder(holder: RequestsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: RequestsViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder)
    }

    fun disableAllButtons() {
        viewHolders.forEach { holder ->
            holder.acceptButton.isEnabled = false
            holder.rejectButton.isEnabled = false
        }
    }

    fun enableAllButtons() {
        viewHolders.forEach { holder ->
            holder.acceptButton.isEnabled = true
            holder.rejectButton.isEnabled = true
        }
    }

    inner class RequestsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val passengerName: TextView = itemView.findViewById(R.id.passengerName)
        val acceptButton: ImageView = itemView.findViewById(R.id.acceptButton)
        val rejectButton: ImageView = itemView.findViewById(R.id.rejectButton)

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
                disableAllButtons()
                onRequestAction?.invoke(rideRequest, RequestStatus.CONFIRMED) {
                    enableAllButtons()
                }
            }

            rejectButton.setOnClickListener {
                disableAllButtons()
                onRequestAction?.invoke(rideRequest, RequestStatus.REJECTED) {
                    enableAllButtons()
                }
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