package com.example.phambili_ma_africa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {
    private val TAG = "ProfileActivity"

    private lateinit var editPhoto: ImageView
    private lateinit var editProfileBtn: MaterialButton
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var profilePhone: TextView
    private lateinit var profileAddress: TextView
    private lateinit var profilePhoto: ImageView
    private lateinit var progressBar: ProgressBar
    
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        initializeViews()
        setupClickListeners()
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun initializeViews() {
        editPhoto = findViewById(R.id.edit_photo)
        editProfileBtn = findViewById(R.id.edit_profile_btn)
        profileName = findViewById(R.id.profile_name)
        profileEmail = findViewById(R.id.profile_email)
        profilePhone = findViewById(R.id.profile_phone)
        profileAddress = findViewById(R.id.profile_address)
        profilePhoto = findViewById(R.id.profile_photo)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupClickListeners() {
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener { finish() }

        editPhoto.setOnClickListener { navigateToEditProfile() }
        editProfileBtn.setOnClickListener { navigateToEditProfile() }
    }

    private fun navigateToEditProfile() {
        startActivity(Intent(this, EditProfileActivity::class.java))
    }

    private fun loadUserProfile() {
        progressBar.visibility = View.VISIBLE
        
        // Get current user ID from Firebase Auth
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            // Fallback to SharedPreferences if not logged in with Firebase
            loadProfileFromPrefs()
            progressBar.visibility = View.GONE
            return
        }
        
        // Try to load from Firebase
        lifecycleScope.launch {
            try {
                val customerRepository = com.example.phambili_ma_africa.repository.CustomerRepository()
                val customer = customerRepository.getCustomerById(currentUserId)
                
                if (customer != null) {
                    // Update UI with Firebase data
                    profileName.text = customer.Full_Name
                    profileEmail.text = customer.Email
                    profilePhone.text = formatPhoneNumber(customer.Phone)
                    
                    // Address is now in Firebase model
                    val address = if (customer.Address.isNotEmpty()) customer.Address else "No address provided"
                    profileAddress.text = address
                    
                    // Save all user data to SharedPreferences for other parts of the app
                    prefs.edit().apply {
                        putString("username", customer.Full_Name)
                        putString("email", customer.Email)
                        putString("phone", customer.Phone)
                        putString("address", customer.Address)
                        apply()
                    }
                    
                    // Load profile image if exists
                    loadProfileImage()
                } else {
                    // Fallback to SharedPreferences
                    loadProfileFromPrefs()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading customer data: ${e.message}")
                Toast.makeText(this@ProfileActivity, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                
                // Fallback to SharedPreferences
                loadProfileFromPrefs()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadProfileFromPrefs() {
        val username = prefs.getString("username", "Fatena Tareq") ?: "Fatena Tareq"
        val email = prefs.getString("email", "faten.tarek.abdallah@gmail.com") ?: "faten.tarek.abdallah@gmail.com"
        val phone = prefs.getString("phone", "0111111111") ?: "0111111111"
        val address = prefs.getString("address", "Cairo, Egypt") ?: "Cairo, Egypt"

        profileName.text = username
        profileEmail.text = email
        profilePhone.text = formatPhoneNumber(phone)
        profileAddress.text = address
        
        // Load profile image if exists
        loadProfileImage()
    }
    
    private fun loadProfileImage() {
        val profileImageUri = prefs.getString("profile_image_uri", "")
        if (!profileImageUri.isNullOrEmpty()) {
            try {
                Glide.with(this)
                    .load(Uri.parse(profileImageUri))
                    .circleCrop()
                    .into(profilePhoto)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile image: ${e.message}")
            }
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val digitsOnly = phone.replace("\\D".toRegex(), "")
        return when {
            digitsOnly.length == 10 && digitsOnly.startsWith("0") ->
                "${digitsOnly.substring(0, 3)} ${digitsOnly.substring(3, 6)} ${digitsOnly.substring(6)}"
            digitsOnly.length >= 11 && digitsOnly.startsWith("27") ->
                "+${digitsOnly.substring(0, 2)} ${digitsOnly.substring(2, 4)} ${digitsOnly.substring(4, 7)} ${digitsOnly.substring(7)}"
            else -> phone
        }
    }
}
