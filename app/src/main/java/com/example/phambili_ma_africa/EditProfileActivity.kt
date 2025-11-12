package com.example.phambili_ma_africa

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.phambili_ma_africa.data.model.Customer
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class EditProfileActivity : BaseActivity() {
    private val TAG = "EditProfileActivity"
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var editPhoto: ImageView
    private lateinit var profilePhoto: ImageView
    private lateinit var saveBtn: MaterialButton
    private lateinit var cancelBtn: MaterialButton
    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var editAddress: EditText
    
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null
    private var progressDialog: ProgressDialog? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.edit_profile)

        initializeViews()
        loadCurrentProfileData()
        setupClickListeners()
    }

    private fun initializeViews() {
        editPhoto = findViewById(R.id.edit_photo)
        profilePhoto = findViewById(R.id.profile_photo)
        saveBtn = findViewById(R.id.save_btn)
        cancelBtn = findViewById(R.id.cancel_btn)
        editName = findViewById(R.id.edit_name)
        editEmail = findViewById(R.id.edit_email)
        editPhone = findViewById(R.id.edit_phone)
        editAddress = findViewById(R.id.edit_address)

        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentProfileData() {
        currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            // Fallback to SharedPreferences if not logged in with Firebase
            editName.setText(prefs.getString("username", ""))
            editEmail.setText(prefs.getString("email", ""))
            editPhone.setText(prefs.getString("phone", ""))
            editAddress.setText(prefs.getString("address", ""))
            return
        }
        
        // Show loading dialog
        showProgressDialog("Loading profile...")
        
        // Load data from Firebase
        lifecycleScope.launch {
            try {
                val customerRepository = com.example.phambili_ma_africa.repository.CustomerRepository()
                val customer = customerRepository.getCustomerById(currentUserId!!)
                
                if (customer != null) {
                    editName.setText(customer.Full_Name)
                    editEmail.setText(customer.Email)
                    editPhone.setText(customer.Phone)
                    editAddress.setText(customer.Address) // Address now in Firebase model
                    
                    // Also save to SharedPreferences for other parts of the app
                    prefs.edit().apply {
                        putString("username", customer.Full_Name)
                        putString("email", customer.Email)
                        putString("phone", customer.Phone)
                        putString("address", customer.Address)
                        apply()
                    }
                    
                    // Load profile image if exists
                    val profileImageUri = prefs.getString("profile_image_uri", "")
                    if (!profileImageUri.isNullOrEmpty()) {
                        try {
                            Glide.with(this@EditProfileActivity)
                                .load(Uri.parse(profileImageUri))
                                .circleCrop()
                                .into(profilePhoto)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading profile image: ${e.message}")
                        }
                    }
                } else {
                    // Fallback to SharedPreferences
                    editName.setText(prefs.getString("username", ""))
                    editEmail.setText(prefs.getString("email", ""))
                    editPhone.setText(prefs.getString("phone", ""))
                    editAddress.setText(prefs.getString("address", ""))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading customer data: ${e.message}")
                Toast.makeText(this@EditProfileActivity, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                
                // Fallback to SharedPreferences
                editName.setText(prefs.getString("username", ""))
                editEmail.setText(prefs.getString("email", ""))
                editPhone.setText(prefs.getString("phone", ""))
                editAddress.setText(prefs.getString("address", ""))
            } finally {
                hideProgressDialog()
            }
        }
    }

    private fun setupClickListeners() {
        editPhoto.setOnClickListener {
            openImagePicker()
        }

        saveBtn.setOnClickListener {
            saveProfileChanges()
        }

        cancelBtn.setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(this, "No file picker available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileChanges() {
        val username = editName.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val phone = editPhone.text.toString().trim()
        val address = editAddress.text.toString().trim()

        if (!validateInputs(username, email, phone, address)) {
            return
        }
        
        // Show loading dialog
        showProgressDialog("Saving profile...")
        
        // Save to SharedPreferences first (as a backup)
        prefs.edit().apply {
            putString("username", username)
            putString("email", email)
            putString("phone", phone)
            putString("address", address)
            apply()
        }
        
        // Save to Firebase if user is logged in
        if (currentUserId != null) {
            lifecycleScope.launch {
                try {
                    val customerRepository = com.example.phambili_ma_africa.repository.CustomerRepository()
                    
                    // First upload image if selected
                    if (selectedImageUri != null) {
                        try {
                            val storageRef = storage.reference.child("profile_images/${currentUserId}")
                            storageRef.putFile(selectedImageUri!!).await()
                            val downloadUrl = storageRef.downloadUrl.await()
                            
                            // Save image URL to SharedPreferences
                            prefs.edit().putString("profile_image_uri", downloadUrl.toString()).apply()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error uploading profile image: ${e.message}")
                        }
                    }
                    
                    // Update customer profile
                    val success = customerRepository.updateCustomerProfile(
                        customerId = currentUserId!!, 
                        fullName = username, 
                        phone = phone,
                        address = address
                    )
                    
                    if (success) {
                        runOnUiThread {
                            Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@EditProfileActivity, "Failed to update profile in database", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK) // Still OK since we saved to SharedPreferences
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving profile: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@EditProfileActivity, "Profile saved locally only", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK) // Still OK since we saved to SharedPreferences
                        finish()
                    }
                } finally {
                    hideProgressDialog()
                }
            }
        } else {
            // No Firebase user, just finish
            hideProgressDialog()
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun validateInputs(username: String, email: String, phone: String, address: String): Boolean {
        if (username.isEmpty()) {
            editName.error = "Name cannot be empty"
            editName.requestFocus()
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.error = "Enter a valid email address"
            editEmail.requestFocus()
            return false
        }
        if (phone.isEmpty()) {
            editPhone.error = "Phone number cannot be empty"
            editPhone.requestFocus()
            return false
        }
        if (address.isEmpty()) {
            editAddress.error = "Address cannot be empty"
            editAddress.requestFocus()
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            selectedImageUri = uri
            
            // Use Glide to load and display the image with circle crop
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(profilePhoto)
                
            // We'll save the URI to SharedPreferences and Firebase Storage when the user clicks Save
            Toast.makeText(this, "Profile picture selected", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected image: ${e.message}")
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showProgressDialog(message: String) {
        hideProgressDialog() // Hide any existing dialog first
        progressDialog = ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }
    
    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}