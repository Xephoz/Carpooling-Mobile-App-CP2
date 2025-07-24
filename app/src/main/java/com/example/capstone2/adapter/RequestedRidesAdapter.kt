package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.model.RequestStatus
import com.example.capstone2.model.Ride
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.capstone2.model.RideRequest

class RequestedRidesAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<Pair<Ride, RideRequest>, RequestedRidesAdapter.SelectedRideViewHolder>(
    SelectedRideDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedRideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_requested_ride, parent, false)
        return SelectedRideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectedRideViewHolder, position: Int) {
        val (ride, request) = getItem(position)
        holder.bind(ride, request)
    }

    inner class SelectedRideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val startLocationText: TextView = itemView.findViewById(R.id.startLocationText)
        private val endLocationText: TextView = itemView.findViewById(R.id.endLocationText)
        private val departureTimeText: TextView = itemView.findViewById(R.id.departureTimeText)
        private val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)

        fun bind(ride: Ride, request: RideRequest) {
            // Set location texts
            startLocationText.text = when {
                ride.startLocation.fullAddress.startsWith(ride.startLocation.displayName) ->
                    ride.startLocation.fullAddress
                else -> listOf(ride.startLocation.displayName, ride.startLocation.fullAddress)
                    .filter { it.isNotEmpty() }
                    .joinToString(" - ")
            }
            endLocationText.text = when {
                ride.endLocation.fullAddress.startsWith(ride.endLocation.displayName) ->
                    ride.endLocation.fullAddress
                else -> listOf(ride.endLocation.displayName, ride.endLocation.fullAddress)
                    .filter { it.isNotEmpty() }
                    .joinToString(" - ")
            }

            // Set departure time
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            departureTimeText.text = "Departure Time: ${dateFormat.format(ride.departureTime.toDate())}"

            // Set status
            statusText.text = when (request.status) {
                RequestStatus.PENDING -> "Request Pending"
                RequestStatus.CONFIRMED -> "Request Confirmed"
                RequestStatus.REJECTED -> "Request Rejected"
            }

            // Set status icon color
            statusIcon.setColorFilter(
                ContextCompat.getColor(itemView.context, when (request.status) {
                    RequestStatus.CONFIRMED -> R.color.confirmed_request
                    RequestStatus.PENDING -> R.color.pending_request
                    RequestStatus.REJECTED -> R.color.rejected_request
                }))
        }
    }

    class SelectedRideDiffCallback : DiffUtil.ItemCallback<Pair<Ride, RideRequest>>() {
        override fun areItemsTheSame(
            oldItem: Pair<Ride, RideRequest>,
            newItem: Pair<Ride, RideRequest>
        ): Boolean {
            return oldItem.second.requestId == newItem.second.requestId
        }

        override fun areContentsTheSame(
            oldItem: Pair<Ride, RideRequest>,
            newItem: Pair<Ride, RideRequest>
        ): Boolean {
            return oldItem == newItem
        }
    }
}