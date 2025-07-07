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
import com.example.capstone2.model.Ride
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

class RidesAdapter(private val onItemClick: (String, Ride) -> Unit) :
    ListAdapter<Pair<String, Ride>, RidesAdapter.RideViewHolder>(RideDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val (documentId, ride) = getItem(position)
        holder.bind(documentId, ride)
    }

    inner class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val routeText: TextView = itemView.findViewById(R.id.routeText)
        private val departureTimeText: TextView = itemView.findViewById(R.id.departureTimeText)
        private val passengersText: TextView = itemView.findViewById(R.id.passengersText)
        private val femaleOnlyIcon: ImageView = itemView.findViewById(R.id.femaleOnlyIcon)

        fun bind(documentId: String, ride: Ride) {
            routeText.text = "${ride.startLocation.displayName} → ${ride.endLocation.displayName}"

            val dateFormat = SimpleDateFormat("MMM dd, yyyh:mm a", Locale.getDefault())
            departureTimeText.text = dateFormat.format(ride.departureTime.toDate())

            passengersText.text = "Passengers: ${ride.passengers.size}/${ride.maxPassengers}"
            femaleOnlyIcon.visibility = if (ride.femaleOnly) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(documentId, ride) }
        }
    }

    class RideDiffCallback : DiffUtil.ItemCallback<Pair<String, Ride>>() {
        override fun areItemsTheSame(oldItem: Pair<String, Ride>, newItem: Pair<String, Ride>): Boolean {
            // Compare document IDs
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(oldItem: Pair<String, Ride>, newItem: Pair<String, Ride>): Boolean {
            // Compare Ride objects (excluding ID)
            return oldItem.second == newItem.second
        }
    }

    companion object {
        // Helper function to create the adapter from DocumentSnapshot list
        fun fromDocumentList(documents: List<DocumentSnapshot>, onItemClick: (String, Ride) -> Unit): RidesAdapter {
            val items = documents.map { doc ->
                doc.id to doc.toObject(Ride::class.java)!!
            }
            val adapter = RidesAdapter(onItemClick)
            adapter.submitList(items)
            return adapter
        }
    }
}