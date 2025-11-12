package com.example.phambili_ma_africa

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.phambili_ma_africa.data.repository.ServicesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Booking : BaseActivity() {

    private lateinit var serviceTypeSpinner: Spinner
    private lateinit var propertyTypeSpinner: Spinner
    private lateinit var bookingDateInput: EditText
    private lateinit var bookingTimeInput: Spinner
    private lateinit var bookingAddressInput: EditText
    private lateinit var instructionsInput: EditText
    private lateinit var btnSubmit: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val servicesRepository = ServicesRepository()
    
    private var selectedServiceName = ""
    private var selectedServiceId = ""
    private var selectedServicePrice = ""
    private var selectedServiceDuration = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_booking)
        setupBaseActivity()
        
        initializeViews()
        loadServicesFromFirebase()
        setupDateAndTimePickers()
        setupSubmitButton()
        setupBackButton()
    }

    private fun initializeViews() {
        try {
            // Use service_frequency instead of service_type
            serviceTypeSpinner = findViewById(R.id.service_frequency)
            propertyTypeSpinner = findViewById(R.id.property_type)
            bookingDateInput = findViewById(R.id.booking_date)
            // bookingTimeInput is now a Spinner instead of EditText
            bookingTimeInput = findViewById(R.id.booking_time)
            // Use address_street for the main address field
            bookingAddressInput = findViewById(R.id.address_street)
            // Use special_requests instead of special_instructions
            instructionsInput = findViewById(R.id.special_requests)
            btnSubmit = findViewById(R.id.submit_btn)

            // Get service details passed from previous activity
            selectedServiceName = intent.getStringExtra("serviceName") ?: ""
            selectedServiceId = intent.getStringExtra("serviceId") ?: ""
            selectedServiceDuration = intent.getStringExtra("duration") ?: ""
            selectedServicePrice = if (selectedServiceDuration.isNotEmpty()) {
                "$selectedServiceDuration min"
            } else {
                intent.getStringExtra("price") ?: "R0"
            }

            if (selectedServiceName.isNotEmpty()) {
                // There's no booking_title in the new layout, so we'll skip this
                // findViewById<TextView>(R.id.booking_title)?.text = "Book: $selectedServiceName"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing booking: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBackButton() {
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadServicesFromFirebase() {
        // Show loading indicator
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar?.visibility = View.VISIBLE
        
        // Load services from Firebase
        lifecycleScope.launch {
            try {
                servicesRepository.getAvailableServices().collectLatest { services ->
                    // Create list of service names for the spinner
                    val serviceNames = services.map { it.Name }.toTypedArray()
                    
                    // Create adapter and set it to the spinner
                    val serviceAdapter = ArrayAdapter(this@Booking, 
                        android.R.layout.simple_spinner_dropdown_item, serviceNames)
                    serviceTypeSpinner.adapter = serviceAdapter
                    
                    // Auto-select the service if one was passed from previous activity
                    if (selectedServiceName.isNotEmpty()) {
                        val position = serviceNames.indexOfFirst { 
                            it.equals(selectedServiceName, ignoreCase = true) 
                        }
                        if (position >= 0) {
                            serviceTypeSpinner.setSelection(position)
                        }
                    }
                    
                    // Hide loading indicator
                    progressBar?.visibility = View.GONE
                    
                    // Setup property types spinner after services are loaded
                    setupPropertyTypesSpinner()
                }
            } catch (e: Exception) {
                Log.e("Booking", "Error loading services: ${e.message}")
                Toast.makeText(this@Booking, "Error loading services", Toast.LENGTH_SHORT).show()
                
                // Fallback to hardcoded services if Firebase fails
                val fallbackServices = arrayOf(
                    "Regular Cleaning",
                    "Deep Cleaning",
                    "Office Cleaning",
                    "Window Cleaning",
                    "Carpet Cleaning",
                    "Upholstery Cleaning",
                    "Commercial Cleaning",
                    "Fumigation"
                )
                
                val serviceAdapter = ArrayAdapter(this@Booking, 
                    android.R.layout.simple_spinner_dropdown_item, fallbackServices)
                serviceTypeSpinner.adapter = serviceAdapter
                
                // Hide loading indicator
                progressBar?.visibility = View.GONE
                
                // Setup property types spinner
                setupPropertyTypesSpinner()
            }
        }
    }
    
    private fun setupPropertyTypesSpinner() {
        // Property types
        val propertyTypes = arrayOf("Apartment", "House", "Office", "Commercial Space", "Other")
        val propertyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, propertyTypes)
        propertyTypeSpinner.adapter = propertyAdapter
    }

    private fun setupDateAndTimePickers() {
        val calendar = Calendar.getInstance()

        // Date Picker
        bookingDateInput.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                    bookingDateInput.setText(dateFormat.format(selectedDate.time))

                    // Check for existing bookings when date changes
                    checkExistingBookings()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            // Set minimum date to today
            datePicker.datePicker.minDate = calendar.timeInMillis
            datePicker.show()
        }

        // Setup Time Spinner
        setupTimeSpinner()
    }
    
    private fun setupTimeSpinner() {
        // Create an adapter with time slots from the string array resource
        val timeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.booking_time_slots,
            android.R.layout.simple_spinner_item
        )
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bookingTimeInput.adapter = timeAdapter
    }

    private fun setupSubmitButton() {
        btnSubmit.setOnClickListener {
            if (validateInputs()) {
                submitBooking()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (bookingDateInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            bookingDateInput.requestFocus()
            return false
        }

        // For spinner, check if an item is selected
        if (bookingTimeInput.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            bookingTimeInput.requestFocus()
            return false
        }

        // Validate address fields
        if (bookingAddressInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your street address", Toast.LENGTH_SHORT).show()
            bookingAddressInput.requestFocus()
            return false
        }
        
        // Check city
        val cityInput = findViewById<EditText>(R.id.address_city)
        if (cityInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your city", Toast.LENGTH_SHORT).show()
            cityInput.requestFocus()
            return false
        }
        
        // Check state/province
        val stateInput = findViewById<EditText>(R.id.address_state)
        if (stateInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your state/province", Toast.LENGTH_SHORT).show()
            stateInput.requestFocus()
            return false
        }
        
        // Check postal code
        val postalInput = findViewById<EditText>(R.id.address_postal_code)
        if (postalInput.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your postal code", Toast.LENGTH_SHORT).show()
            postalInput.requestFocus()
            return false
        }

        return true
    }

    // Time validation is now handled by the spinner options

    private fun submitBooking() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login to book a service", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Login::class.java))
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = "Submitting..."
        
        // First, try to get the customer data from Firestore
        lifecycleScope.launch {
            try {
                // Get customer data from Firestore
                val customerDoc = db.collection("customers").document(user.uid).get().await()
                
                // Get the customer name from Firestore or SharedPreferences
                val customerName = if (customerDoc.exists()) {
                    // Try to get the name using both field naming conventions
                    val fullName = customerDoc.getString("full_name")
                    val fullNameAlt = customerDoc.getString("Full_Name")
                    
                    when {
                        !fullName.isNullOrEmpty() -> fullName
                        !fullNameAlt.isNullOrEmpty() -> fullNameAlt
                        !user.displayName.isNullOrEmpty() -> user.displayName
                        else -> prefs.getString("username", "Customer") ?: "Customer"
                    }
                } else {
                    // Fallback to user display name or SharedPreferences
                    user.displayName ?: prefs.getString("username", "Customer") ?: "Customer"
                }
                
                // Get customer email
                val customerEmail = if (customerDoc.exists()) {
                    val email = customerDoc.getString("email")
                    val emailAlt = customerDoc.getString("Email")
                    
                    when {
                        !email.isNullOrEmpty() -> email
                        !emailAlt.isNullOrEmpty() -> emailAlt
                        !user.email.isNullOrEmpty() -> user.email
                        else -> prefs.getString("email", "") ?: ""
                    }
                } else {
                    user.email ?: prefs.getString("email", "") ?: ""
                }
                
                // Get customer phone
                val customerPhone = if (customerDoc.exists()) {
                    val phone = customerDoc.getString("phone")
                    val phoneAlt = customerDoc.getString("Phone")
                    
                    when {
                        !phone.isNullOrEmpty() -> phone
                        !phoneAlt.isNullOrEmpty() -> phoneAlt
                        else -> prefs.getString("phone", "") ?: ""
                    }
                } else {
                    prefs.getString("phone", "") ?: ""
                }
                
                // Get the selected service ID if we don't already have it
                var serviceId = selectedServiceId
                var serviceName = serviceTypeSpinner.selectedItem.toString()
                
                // If we don't have a service ID but have a service name, try to find the ID
                if (serviceId.isEmpty() && serviceName.isNotEmpty()) {
                    try {
                        // Query Firestore to find the service by name
                        val serviceQuery = db.collection("services")
                            .whereEqualTo("Name", serviceName)
                            .limit(1)
                            .get()
                            .await()
                            
                        if (!serviceQuery.isEmpty) {
                            serviceId = serviceQuery.documents[0].id
                        }
                    } catch (e: Exception) {
                        Log.e("Booking", "Error finding service ID: ${e.message}")
                    }
                }
                
                // Get address components
                val cityInput = findViewById<EditText>(R.id.address_city)
                val stateInput = findViewById<EditText>(R.id.address_state)
                val postalInput = findViewById<EditText>(R.id.address_postal_code)
                
                // Combine address components
                val fullAddress = "${bookingAddressInput.text}, ${cityInput.text}, ${stateInput.text}, ${postalInput.text}"
                
                // Create booking data with the retrieved customer and service information
                val bookingData = hashMapOf(
                    // Customer information
                    "Customer_ID" to user.uid,
                    "Customer_Name" to customerName,
                    "Customer_Email" to customerEmail,
                    "Customer_Phone" to customerPhone,
                    
                    // Service information
                    "Service_ID" to serviceId,
                    "Service_Name" to serviceName,
                    "Service_Type" to serviceTypeSpinner.selectedItem.toString(),
                    
                    // Booking details
                    "Property_Type" to propertyTypeSpinner.selectedItem.toString(),
                    "Date" to bookingDateInput.text.toString(),
                    "Time" to bookingTimeInput.selectedItem.toString(),
                    "Address" to fullAddress,
                    "Street_Address" to bookingAddressInput.text.toString(),
                    "City" to cityInput.text.toString(),
                    "State" to stateInput.text.toString(),
                    "Postal_Code" to postalInput.text.toString(),
                    "Special_Instructions" to instructionsInput.text.toString(),
                    "Status" to "requested",
                    "Created_At" to Calendar.getInstance().time,
                    "Updated_At" to Calendar.getInstance().time
                )
                
                // Add booking to Firestore
                db.collection("bookings")
                    .add(bookingData)
                    .addOnSuccessListener { documentReference ->
                        val bookingId = documentReference.id
                        // Save to local storage for offline access
                        saveBookingToLocalStorage(bookingId, serviceName, propertyTypeSpinner.selectedItem.toString(), 
                            bookingDateInput.text.toString(), bookingTimeInput.selectedItem.toString(), 
                            "requested", bookingAddressInput.text.toString(), instructionsInput.text.toString())
                        showSuccessDialog(bookingId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@Booking, "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
                        resetSubmitButton()
                    }
                
            } catch (e: Exception) {
                Log.e("Booking", "Error getting customer data: ${e.message}")
                
                // Get the selected service name
                val serviceName = serviceTypeSpinner.selectedItem.toString()
                var serviceId = selectedServiceId
                
                // If we don't have a service ID but have a service name, try to find the ID
                if (serviceId.isEmpty() && serviceName.isNotEmpty()) {
                    try {
                        // Query Firestore to find the service by name
                        val serviceQuery = db.collection("services")
                            .whereEqualTo("Name", serviceName)
                            .limit(1)
                            .get()
                            .await()
                            
                        if (!serviceQuery.isEmpty) {
                            serviceId = serviceQuery.documents[0].id
                        }
                    } catch (e: Exception) {
                        Log.e("Booking", "Error finding service ID: ${e.message}")
                    }
                }
                
                // Get address components
                val cityInput = findViewById<EditText>(R.id.address_city)
                val stateInput = findViewById<EditText>(R.id.address_state)
                val postalInput = findViewById<EditText>(R.id.address_postal_code)
                
                // Combine address components
                val fullAddress = "${bookingAddressInput.text}, ${cityInput.text}, ${stateInput.text}, ${postalInput.text}"
                
                // Fallback to basic booking data if there's an error
                val fallbackBookingData = hashMapOf(
                    // Customer information
                    "Customer_ID" to user.uid,
                    "Customer_Name" to (user.displayName ?: prefs.getString("username", "Customer") ?: "Customer"),
                    "Customer_Email" to (user.email ?: prefs.getString("email", "") ?: ""),
                    "Customer_Phone" to (prefs.getString("phone", "") ?: ""),
                    
                    // Service information
                    "Service_ID" to serviceId,
                    "Service_Name" to serviceName,
                    "Service_Type" to serviceTypeSpinner.selectedItem.toString(),
                    
                    // Booking details
                    "Property_Type" to propertyTypeSpinner.selectedItem.toString(),
                    "Date" to bookingDateInput.text.toString(),
                    "Time" to bookingTimeInput.selectedItem.toString(),
                    "Address" to fullAddress,
                    "Street_Address" to bookingAddressInput.text.toString(),
                    "City" to cityInput.text.toString(),
                    "State" to stateInput.text.toString(),
                    "Postal_Code" to postalInput.text.toString(),
                    "Special_Instructions" to instructionsInput.text.toString(),
                    "Status" to "requested",
                    "Created_At" to Calendar.getInstance().time,
                    "Updated_At" to Calendar.getInstance().time
                )
                
                db.collection("bookings")
                    .add(fallbackBookingData)
                    .addOnSuccessListener { documentReference ->
                        val bookingId = documentReference.id
                        // Save to local storage for offline access
                        saveBookingToLocalStorage(bookingId, serviceName, propertyTypeSpinner.selectedItem.toString(), 
                            bookingDateInput.text.toString(), bookingTimeInput.selectedItem.toString(), 
                            "requested", bookingAddressInput.text.toString(), instructionsInput.text.toString())
                        showSuccessDialog(bookingId)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@Booking, "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
                        resetSubmitButton()
                    }
            }
        }
    }

    private fun showSuccessDialog(bookingId: String) {
        val serviceName = if (selectedServiceName.isNotEmpty()) selectedServiceName else serviceTypeSpinner.selectedItem.toString()
        
        // Get address components
        val cityInput = findViewById<EditText>(R.id.address_city)
        val stateInput = findViewById<EditText>(R.id.address_state)
        val postalInput = findViewById<EditText>(R.id.address_postal_code)
        
        val bookingDetails = """
            Service: $serviceName
            Property Type: ${propertyTypeSpinner.selectedItem}
            Date: ${bookingDateInput.text}
            Time: ${bookingTimeInput.selectedItem}
            Address: ${bookingAddressInput.text}, ${cityInput.text}, ${stateInput.text}, ${postalInput.text}
            Special Instructions: ${instructionsInput.text}
            Booking ID: $bookingId
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Booking Confirmation")
            .setMessage("Your booking has been submitted!\n\n$bookingDetails")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("booking_success", true)
                }
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkExistingBookings() {
        val user = auth.currentUser
        val selectedDate = bookingDateInput.text.toString()

        if (user != null && selectedDate.isNotEmpty()) {
            db.collection("bookings")
                .whereEqualTo("Customer_ID", user.uid)
                .whereEqualTo("Date", selectedDate)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val services = documents.map { it.getString("Service_Type") ?: "" }
                        val servicesText = services.joinToString(", ")
                        Toast.makeText(this, "Already booked on $selectedDate: $servicesText", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    // Silent fail - don't show error for this check
                }
        }
    }

    private fun resetSubmitButton() {
        btnSubmit.isEnabled = true
        btnSubmit.text = "Submit Booking"
    }
    
    private fun saveBookingToLocalStorage(id: String, service: String, property: String, date: String, time: String, status: String, address: String, instructions: String) {
        val sharedPreferences = getSharedPreferences("booking_history", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        
        // Get full address from components
        val cityInput = findViewById<EditText>(R.id.address_city)
        val stateInput = findViewById<EditText>(R.id.address_state)
        val postalInput = findViewById<EditText>(R.id.address_postal_code)
        val fullAddress = "$address, ${cityInput.text}, ${stateInput.text}, ${postalInput.text}"
        
        val bookingDetails = """
            Service: $service
            Property: $property
            Date: $date
            Time: $time
            Status: $status
            Address: $fullAddress
            Instructions: $instructions
        """.trimIndent()
        
        editor.putString(id, bookingDetails)
        editor.apply()
    }

    // Data class for booking items (optional - for type safety)
    data class BookingItem(
        val id: String,
        val serviceType: String,
        val propertyType: String,
        val date: String,
        val time: String,
        val status: String
    )
}