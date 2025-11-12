package com.example.phambili_ma_africa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class BookingAdapter(
    private var bookings: List<BookingHistoryActivity.Booking>,
    private val onItemClick: (BookingHistoryActivity.Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bind(booking)
        holder.itemView.setOnClickListener { onItemClick(booking) }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<BookingHistoryActivity.Booking>) {
        this.bookings = newBookings
        notifyDataSetChanged()
    }

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceText: TextView = itemView.findViewById(R.id.booking_service_name)
        private val dateText: TextView = itemView.findViewById(R.id.booking_date)
        private val timeText: TextView = itemView.findViewById(R.id.booking_time)
        private val statusText: TextView = itemView.findViewById(R.id.booking_status)
        private val propertyText: TextView = itemView.findViewById(R.id.booking_property)

        fun bind(booking: BookingHistoryActivity.Booking) {
            serviceText.text = booking.service
            dateText.text = booking.date
            timeText.text = booking.time
            propertyText.text = booking.property
            statusText.text = booking.status

            // Format status text and set color
            val (displayStatus, color) = when (booking.status.lowercase()) {
                "completed" -> Pair("Completed", R.color.green)
                "cancelled" -> Pair("Cancelled", R.color.red)
                "in_progress", "in progress" -> Pair("In Progress", R.color.orange)
                "confirmed" -> Pair("Confirmed", R.color.primary_blue)
                "requested" -> Pair("Requested", R.color.primary_blue)
                else -> Pair(booking.status.replaceFirstChar { it.uppercase() }, R.color.primary_blue)
            }
            
            statusText.text = displayStatus
            statusText.setTextColor(ContextCompat.getColor(itemView.context, color))
            
            // Set background tint based on status
            val backgroundDrawable = statusText.background.mutate()
            backgroundDrawable.setTint(ContextCompat.getColor(itemView.context, 
                when (booking.status.lowercase()) {
                    "completed" -> R.color.light_green
                    "cancelled" -> R.color.light_red
                    "in_progress", "in progress" -> R.color.light_orange
                    else -> R.color.light_blue
                }
            ))
        }
    }
}