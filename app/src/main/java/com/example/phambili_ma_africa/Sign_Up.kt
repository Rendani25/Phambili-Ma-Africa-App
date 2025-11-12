package com.example.phambili_ma_africa

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.phambili_ma_africa.data.model.Customer
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class Sign_Up : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase components
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        setupUI()
    }

    private fun setupUI() {
        val fullNameInput = findViewById<EditText>(R.id.username)
        val emailInput = findViewById<EditText>(R.id.signup_email)
        val passwordInput = findViewById<EditText>(R.id.signup_password)
        val confirmPasswordInput = findViewById<EditText>(R.id.signup_confirm_password)
        val signUpBtn = findViewById<MaterialButton>(R.id.signup_btn)
        val loginText = findViewById<TextView>(R.id.login_text)
        val togglePassword = findViewById<ImageButton>(R.id.signup_toggle_password)
        val toggleConfirmPassword = findViewById<ImageButton>(R.id.signup_confirm_toggle_password)

        // Set up the Sign In text click listener
        loginText.setOnClickListener {
            navigateToLogin()
        }

        // Toggle password visibility
        togglePassword.setOnClickListener {
            togglePasswordVisibility(passwordInput)
        }

        // Toggle confirm password visibility
        toggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(confirmPasswordInput)
        }

        signUpBtn.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (validateInput(fullName, email, password, confirmPassword)) {
                registerUser(fullName, email, password)
            }
        }
    }

    private fun togglePasswordVisibility(editText: EditText) {
        if (editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            // Show password
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        // Move cursor to the end of text
        editText.setSelection(editText.text.length)
    }

    private fun validateInput(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Please fill in all fields")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address")
            return false
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return false
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match")
            return false
        }

        return true
    }

    private fun registerUser(fullName: String, email: String, password: String) {
        val signUpBtn = findViewById<MaterialButton>(R.id.signup_btn)
        signUpBtn.isEnabled = false
        signUpBtn.text = "Creating Account..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                signUpBtn.isEnabled = true
                signUpBtn.text = "Sign Up"

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener
                    
                    // Current timestamp for registration date
                    val registrationDate = java.util.Date().toString()

                    // Create customer object for our model
                    val customer = Customer(
                        ID = userId,
                        Full_Name = fullName,
                        Email = email,
                        Phone = "", // Add default or get from UI if needed
                        Registration_Date = registrationDate
                    )
                    
                    // Create a map with both naming conventions to ensure compatibility
                    // with both the admin dashboard and the app
                    val customerData = hashMapOf<String, Any>(
                        // Original app model fields (uppercase first letter)
                        "ID" to userId,
                        "Full_Name" to fullName,
                        "Email" to email,
                        "Phone" to "",
                        "Registration_Date" to registrationDate,
                        
                        // Admin dashboard fields (lowercase first letter)
                        "id" to userId,
                        "full_name" to fullName,
                        "email" to email,
                        "phone" to "",
                        "registration_date" to registrationDate,
                        
                        // Additional fields that might be used by the admin dashboard
                        "customerName" to fullName,
                        "customerEmail" to email,
                        "customerPhone" to "",
                        "is_active" to true,
                        "created_at" to com.google.firebase.Timestamp.now(),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    )

                    // ✅ Save customer to Firestore with the comprehensive data map
                    db.collection("customers").document(userId)
                        .set(customerData)
                        .addOnSuccessListener {
                            Log.d(TAG, "Customer successfully added to Firestore")
                            showToast("Account created successfully!")
                            
                            // Also update the user profile display name
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build()
                                
                            user.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        Log.d(TAG, "User profile display name updated")
                                    }
                                }

                            // Send email verification
                            user.sendEmailVerification()
                                .addOnCompleteListener { verificationTask ->
                                    if (verificationTask.isSuccessful) {
                                        showToast("Verification email sent to $email")
                                    } else {
                                        Log.w(TAG, "Failed to send verification email", verificationTask.exception)
                                        showToast("Failed to send verification email")
                                    }
                                }

                            navigateToLogin()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error saving customer to Firestore", e)
                            showToast("Error saving data: ${e.localizedMessage}")
                        }

                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    showToast("Account creation failed: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}