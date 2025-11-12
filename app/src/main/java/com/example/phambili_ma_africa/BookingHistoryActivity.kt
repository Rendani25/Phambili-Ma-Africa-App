package com.example.phambili_ma_africa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Remove repository imports as we'll use Firestore directly
import com.example.phambili_ma_africa.util.FCMHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookingHistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookingAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var backButton: ImageView
    private lateinit var progressBar: ProgressBar

    // Use Firestore directly instead of repositories
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var bookingStatusListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        initializeViews()
        setupRecyclerView()
        loadBookingsFromFirebase()
        setupBookingStatusListener()

        // Register FCM token to ensure we can receive notifications
        FCMHelper.registerFCMToken(this)
        FCMHelper.subscribeToBookingUpdates()
        
        // Handle intent if activity was launched from notification
        if (intent != null) {
            handleNotificationIntent(intent)
        }
    }
    
    private fun handleNotificationIntent(intent: Intent) {
        val bookingId = intent.getStringExtra("BOOKING_ID")
        val action = intent.getStringExtra("ACTION")
        
        if (bookingId != null) {
            // Find the booking directly from Firestore
            lifecycleScope.launch {
                try {
                    val docSnapshot = db.collection("bookings")
                        .document(bookingId)
                        .get()
                        .await()
                    
                    if (docSnapshot.exists()) {
                        // Try to get service name with both naming conventions
                        val serviceName = docSnapshot.getString("Service_Name")
                            ?: docSnapshot.getString("serviceName")
                            ?: "Unknown Service"

                        // Try to get property type with both naming conventions
                        val propertyType = docSnapshot.getString("Property_Type")
                            ?: docSnapshot.getString("propertyType")
                            ?: "Unknown Property"

                        // Try to get date with both naming conventions
                        val date = docSnapshot.getString("Date")
                            ?: docSnapshot.getString("date")
                            ?: "No Date"

                        // Try to get time with both naming conventions
                        val time = docSnapshot.getString("Time")
                            ?: docSnapshot.getString("time")
                            ?: "No Time"

                        // Try to get status with both naming conventions
                        val status = docSnapshot.getString("Status")
                            ?: docSnapshot.getString("status")
                            ?: "pending"

                        // Try to get address with both naming conventions
                        val address = docSnapshot.getString("Address")
                            ?: docSnapshot.getString("address")
                            ?: "No Address"

                        // Try to get instructions with both naming conventions
                        val instructions = docSnapshot.getString("Special_Instructions")
                            ?: docSnapshot.getString("specialInstructions")
                            ?: ""

                        // Create a booking object
                        val booking = Booking(
                            bookingId, 
                            serviceName, 
                            propertyType, 
                            date, 
                            time, 
                            status, 
                            address, 
                            instructions
                        )
                        
                        runOnUiThread {
                            if (action == "VIEW_DETAILS") {
                                showBookingDetails(booking)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BookingHistory", "Error getting booking details: ${e.message}")
                }
            }
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.bookings_recycler_view)
        emptyState = findViewById(R.id.empty_state)
        backButton = findViewById(R.id.back_button)

        // Add progress bar to layout
        val contentLayout = findViewById<LinearLayout>(R.id.content_layout)
        progressBar = ProgressBar(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                topMargin = 100
            }
            visibility = View.VISIBLE
        }
        contentLayout.addView(progressBar, 0)

        backButton.setOnClickListener {
            finish()
        }

        // Set the bookings count text
        val bookingsCount = findViewById<TextView>(R.id.bookings_count)
        bookingsCount.text = "Your booking history"
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter(emptyList()) { booking ->
            showBookingDetails(booking)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupBookingStatusListener() {
        bookingStatusListener = FCMHelper.listenForBookingStatusChanges(this) { bookingId, newStatus ->
            // Show a toast notification when in the app
            runOnUiThread {
                Toast.makeText(
                    this@BookingHistoryActivity,
                    "Booking status updated to: ${newStatus.replaceFirstChar { it.uppercase() }}",
                    Toast.LENGTH_LONG
                ).show()

                // Refresh the bookings list
                loadBookingsFromFirebase()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the stored intent
        
        // Handle notification clicks using our helper method
        handleNotificationIntent(intent)
    }

    private fun loadBookingsFromFirebase() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyState.visibility = View.GONE

        // Check if user is logged in
        if (auth.currentUser == null) {
            Log.e("BookingHistory", "User not logged in")
            Toast.makeText(this, "Please log in to view your bookings", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        Log.d("BookingHistory", "Starting to load bookings for user: ${auth.currentUser?.uid}")

        lifecycleScope.launch {
            try {
                // Try direct Firestore query
                Log.d("BookingHistory", "Trying direct Firestore query")
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val bookingsSnapshot = db.collection("bookings")
                        .whereEqualTo("Customer_ID", userId)
                        .get()
                        .await()

                    val bookingsList = mutableListOf<Booking>()

                    for (doc in bookingsSnapshot.documents) {
                        val id = doc.id

                        // Try to get service name with both naming conventions
                        val serviceName = doc.getString("Service_Name")
                            ?: doc.getString("serviceName")
                            ?: "Unknown Service"

                        // Try to get property type with both naming conventions
                        val propertyType = doc.getString("Property_Type")
                            ?: doc.getString("propertyType")
                            ?: "Unknown Property"

                        // Try to get date with both naming conventions
                        val date = doc.getString("Date")
                            ?: doc.getString("date")
                            ?: "No Date"

                        // Try to get time with both naming conventions
                        val time = doc.getString("Time")
                            ?: doc.getString("time")
                            ?: "No Time"

                        // Try to get status with both naming conventions
                        val status = doc.getString("Status")
                            ?: doc.getString("status")
                            ?: "pending"

                        // Try to get address with both naming conventions
                        val address = doc.getString("Address")
                            ?: doc.getString("address")
                            ?: "No Address"

                        // Try to get instructions with both naming conventions
                        val instructions = doc.getString("Special_Instructions")
                            ?: doc.getString("specialInstructions")
                            ?: ""

                        // Create a booking object and add it to the list
                        bookingsList.add(Booking(id, serviceName, propertyType, date, time, status, address, instructions))
                    }

                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        updateBookingsUI(bookingsList)
                    }
                }
            } catch (e: Exception) {
                Log.e("BookingHistory", "Error loading bookings: ${e.message}")

                // Final fallback to local storage
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    val localBookings = getExistingBookings()
                    updateBookingsUI(localBookings)

                    Toast.makeText(this@BookingHistoryActivity,
                        "Error loading bookings from server. Showing local data.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBookingsUI(bookings: List<Booking>) {
        val bookingsCount = findViewById<TextView>(R.id.bookings_count)

        if (bookings.isEmpty()) {
            bookingsCount.text = "No bookings found"
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            bookingsCount.text = "${bookings.size} booking(s) found"
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            adapter.updateBookings(bookings)
        }
    }

    private fun getExistingBookings(): List<Booking> {
        val sharedPreferences = getSharedPreferences("booking_history", MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        val bookings = mutableListOf<Booking>()

        for ((key, value) in allEntries) {
            if (value is String) {
                val lines = value.split("\n")
                var service = ""
                var date = ""
                var time = ""
                var status = ""
                var property = ""
                var address = ""
                var instructions = ""

                for (line in lines) {
                    when {
                        line.startsWith("Service:") -> service = line.substringAfter("Service:").trim()
                        line.startsWith("Date:") -> date = line.substringAfter("Date:").trim()
                        line.startsWith("Time:") -> time = line.substringAfter("Time:").trim()
                        line.startsWith("Status:") -> status = line.substringAfter("Status:").trim()
                        line.startsWith("Property:") -> property = line.substringAfter("Property:").trim()
                        line.startsWith("Address:") -> address = line.substringAfter("Address:").trim()
                        line.startsWith("Instructions:") -> instructions = line.substringAfter("Instructions:").trim()
                    }
                }

                if (service.isNotEmpty()) {
                    bookings.add(Booking(key, service, property, date, time, status, address, instructions))
                }
            }
        }

        return bookings
    }

    private fun showBookingDetails(booking: Booking) {
        val statusText = when (booking.status.lowercase()) {
            "requested" -> "Requested (Waiting for confirmation)"
            "confirmed" -> "Confirmed (Service scheduled)"
            "in_progress", "in progress" -> "In Progress (Service ongoing)"
            "completed" -> "Completed (Service finished)"
            "cancelled" -> "Cancelled"
            else -> booking.status.replaceFirstChar { it.uppercase() }
        }

        val details = """
            Service: ${booking.service}
            Property: ${booking.property}
            Date: ${booking.date}
            Time: ${booking.time}
            Address: ${booking.address}
            Status: $statusText
            Instructions: ${booking.instructions}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the booking status listener when the activity is destroyed
        bookingStatusListener?.remove()
    }

    data class Booking(
        val id: String,
        val service: String,
        val property: String,
        val date: String,
        val time: String,
        val status: String,
        val address: String,
        val instructions: String
    )

    inner class BookingAdapter(
        private var bookings: List<Booking>,
        private val onItemClick: (Booking) -> Unit
    ) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val serviceName: TextView = view.findViewById(R.id.booking_service_name)
            val date: TextView = view.findViewById(R.id.booking_date)
            val time: TextView = view.findViewById(R.id.booking_time)
            val status: TextView = view.findViewById(R.id.booking_status)
            val container: View = view.findViewById(R.id.booking_item_container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_booking, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val booking = bookings[position]

            holder.serviceName.text = booking.service
            holder.date.text = booking.date
            holder.time.text = booking.time

            // Set status text and color
            val statusText = when (booking.status.lowercase()) {
                "requested" -> "Requested"
                "approved" -> "Approved"
                "confirmed" -> "Confirmed"
                "in_progress", "in progress" -> "In Progress"
                "completed" -> "Completed"
                "declined", "cancelled" -> "Declined"
                else -> booking.status.replaceFirstChar { it.uppercase() }
            }

            holder.status.text = statusText

            // Set status color
            val statusColor = when (booking.status.lowercase()) {
                "requested" -> R.color.status_requested
                "approved" -> R.color.status_approved
                "confirmed" -> R.color.status_confirmed
                "in_progress", "in progress" -> R.color.status_in_progress
                "completed" -> R.color.status_completed
                "declined", "cancelled" -> R.color.status_declined
                else -> R.color.status_requested
            }

            holder.status.setTextColor(holder.itemView.context.getColor(statusColor))

            // Set click listener
            holder.container.setOnClickListener {
                onItemClick(booking)
            }
        }

        override fun getItemCount() = bookings.size

        fun updateBookings(newBookings: List<Booking>) {
            bookings = newBookings
            notifyDataSetChanged()
        }
    }
}