package com.example.phambili_ma_africa

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.phambili_ma_africa.util.FCMHelper

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var prefs: SharedPreferences
    private val TAG = "LoginActivity"
    private val db = Firebase.firestore

    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            // Initialize Firebase Auth
            auth = Firebase.auth
            prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

            // Check if user is already logged in
            if (auth.currentUser != null) {
                navigateToMain()
                return
            }

            setupGoogleSignIn()
            setupUI()

        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed", e)
            showToast("App initialization failed")
        }
    }

    private fun setupUI() {
        val btnLogin = findViewById<MaterialButton>(R.id.login_btn)
        val emailEditText = findViewById<EditText>(R.id.login_email)
        val passwordEditText = findViewById<EditText>(R.id.login_password)
        val togglePassword = findViewById<ImageButton>(R.id.login_toggle_password)
        val rememberCheckBox = findViewById<CheckBox>(R.id.login_remember)
        val signupText = findViewById<TextView>(R.id.login_signup)
        val forgotPasswordText = findViewById<TextView>(R.id.login_forgot)
        val googleBtn = findViewById<SignInButton>(R.id.btn_google_signin)

        // Load saved email
        emailEditText.setText(prefs.getString("saved_email", ""))

        // Email/Password Login
        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (validateLogin(email, password)) {
                loginWithEmailPassword(email, password, rememberCheckBox.isChecked)
            }
        }

        // Toggle password visibility
        togglePassword.setOnClickListener {
            togglePasswordVisibility(passwordEditText)
        }

        // Navigate to sign up
        signupText.setOnClickListener {
            startActivity(Intent(this, Sign_Up::class.java))
        }

        // Forgot password
        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Google Sign-In
        googleBtn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun loginWithEmailPassword(email: String, password: String, rememberMe: Boolean) {
        val btnLogin = findViewById<MaterialButton>(R.id.login_btn)
        btnLogin.isEnabled = false
        btnLogin.text = "Signing in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    user?.let {
                        // Save customer data to Firestore
                        saveCustomerToFirestore(it, email, rememberMe)
                        showToast("Welcome back!")
                        navigateToMain()
                    }
                } else {
                    // If sign in fails, check if user needs to be created
                    if (task.exception?.message?.contains("no user record") == true) {
                        createUserWithEmailPassword(email, password, rememberMe)
                    } else {
                        // Sign in failed
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        showToast("Authentication failed: ${task.exception?.message}")
                        resetLoginButton()
                    }
                }
            }
    }

    private fun createUserWithEmailPassword(email: String, password: String, rememberMe: Boolean) {
        val btnLogin = findViewById<MaterialButton>(R.id.login_btn)
        btnLogin.text = "Creating account..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.let {
                        // Create customer profile in Firestore
                        createCustomerProfile(it, email, rememberMe)
                        showToast("Account created successfully!")
                        navigateToMain()
                    }
                } else {
                    // Sign up failed
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    showToast("Account creation failed: ${task.exception?.message}")
                    resetLoginButton()
                }
            }
    }

    private fun saveCustomerToFirestore(user: com.google.firebase.auth.FirebaseUser, email: String, rememberMe: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if customer already exists
                val customerDoc = db.collection("customers").document(user.uid).get().await()
                
                val displayName = user.displayName ?: email.substringBefore("@")

                if (!customerDoc.exists()) {
                    // Create new customer document with both naming conventions
                    val customerData = hashMapOf<String, Any>(
                        // Original app model fields (uppercase first letter)
                        "ID" to user.uid,
                        "Full_Name" to displayName,
                        "Email" to email,
                        "Phone" to "",
                        "Registration_Date" to java.util.Date().toString(),
                        
                        // Admin dashboard fields (lowercase first letter)
                        "id" to user.uid,
                        "full_name" to displayName,
                        "email" to email,
                        "phone" to "",
                        "registration_date" to java.util.Date().toString(),
                        
                        // Additional fields that might be used by the admin dashboard
                        "customerName" to displayName,
                        "customerEmail" to email,
                        "customerPhone" to "",
                        "address" to "",
                        "is_active" to true,
                        "email_verified" to user.isEmailVerified,
                        "login_attempts" to 0,
                        "created_at" to com.google.firebase.Timestamp.now(),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("customers").document(user.uid).set(customerData).await()
                    Log.d(TAG, "Customer data saved to Firestore")
                } else {
                    // Update existing customer with both naming conventions
                    val updateData = hashMapOf<String, Any>(
                        // Update both naming conventions
                        "Full_Name" to displayName,
                        "full_name" to displayName,
                        "customerName" to displayName,
                        "email_verified" to user.isEmailVerified,
                        "updated_at" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("customers").document(user.uid).update(updateData).await()
                    Log.d(TAG, "Customer data updated in Firestore")
                }

                // Save session data
                saveUserSession(user, rememberMe)

            } catch (e: Exception) {
                Log.e(TAG, "Error saving customer to Firestore", e)
                // Still save session even if Firestore fails
                runOnUiThread {
                    saveUserSession(user, rememberMe)
                }
            }
        }
    }

    private fun createCustomerProfile(user: com.google.firebase.auth.FirebaseUser, email: String, rememberMe: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val displayName = user.displayName ?: email.substringBefore("@")
                
                // Create customer document with both naming conventions
                val customerData = hashMapOf<String, Any>(
                    // Original app model fields (uppercase first letter)
                    "ID" to user.uid,
                    "Full_Name" to displayName,
                    "Email" to email,
                    "Phone" to "",
                    "Registration_Date" to java.util.Date().toString(),
                    
                    // Admin dashboard fields (lowercase first letter)
                    "id" to user.uid,
                    "full_name" to displayName,
                    "email" to email,
                    "phone" to "",
                    "registration_date" to java.util.Date().toString(),
                    
                    // Additional fields that might be used by the admin dashboard
                    "customerName" to displayName,
                    "customerEmail" to email,
                    "customerPhone" to "",
                    "address" to "",
                    "is_active" to true,
                    "email_verified" to user.isEmailVerified,
                    "login_attempts" to 0,
                    "created_at" to com.google.firebase.Timestamp.now(),
                    "updated_at" to com.google.firebase.Timestamp.now()
                )

                db.collection("customers").document(user.uid).set(customerData).await()
                Log.d(TAG, "New customer profile created in Firestore")
                
                // Also update the user profile display name
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                    
                user.updateProfile(profileUpdates).await()
                Log.d(TAG, "User profile display name updated")

                // Save session data
                saveUserSession(user, rememberMe)

            } catch (e: Exception) {
                Log.e(TAG, "Error creating customer profile in Firestore", e)
                // Still save session even if Firestore fails
                runOnUiThread {
                    saveUserSession(user, rememberMe)
                }
            }
        }
    }

    private fun saveUserSession(user: com.google.firebase.auth.FirebaseUser, rememberMe: Boolean) {
        runOnUiThread {
            prefs.edit {
                putBoolean("is_logged_in", true)
                putString("user_email", user.email ?: "")
                putString("user_name", user.displayName ?: user.email?.substringBefore("@") ?: "User")
                putString("user_id", user.uid)
                if (rememberMe) {
                    putString("saved_email", user.email ?: "")
                } else {
                    remove("saved_email")
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed
                Log.w(TAG, "Google sign in failed", e)
                showToast("Google sign in failed")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.let {
                        // Save Google user to Firestore
                        saveGoogleUserToFirestore(it, true)
                        showToast("Google sign in successful!")
                        navigateToMain()
                    }
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    showToast("Authentication failed")
                }
            }
    }

    private fun saveGoogleUserToFirestore(user: com.google.firebase.auth.FirebaseUser, rememberMe: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val displayName = user.displayName ?: "Google User"
                val email = user.email ?: ""
                
                // Create customer document with both naming conventions
                val customerData = hashMapOf<String, Any>(
                    // Original app model fields (uppercase first letter)
                    "ID" to user.uid,
                    "Full_Name" to displayName,
                    "Email" to email,
                    "Phone" to "",
                    "Registration_Date" to java.util.Date().toString(),
                    
                    // Admin dashboard fields (lowercase first letter)
                    "id" to user.uid,
                    "full_name" to displayName,
                    "email" to email,
                    "phone" to "",
                    "registration_date" to java.util.Date().toString(),
                    
                    // Additional fields that might be used by the admin dashboard
                    "customerName" to displayName,
                    "customerEmail" to email,
                    "customerPhone" to "",
                    "address" to "",
                    "is_active" to true,
                    "email_verified" to user.isEmailVerified,
                    "login_attempts" to 0,
                    "created_at" to com.google.firebase.Timestamp.now(),
                    "updated_at" to com.google.firebase.Timestamp.now()
                )

                db.collection("customers").document(user.uid).set(customerData).await()
                Log.d(TAG, "Google user saved to Firestore")

                // Save session data
                saveUserSession(user, rememberMe)

            } catch (e: Exception) {
                Log.e(TAG, "Error saving Google user to Firestore", e)
                runOnUiThread {
                    saveUserSession(user, rememberMe)
                }
            }
        }
    }

    private fun validateLogin(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
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

        return true
    }

    private fun resetLoginButton() {
        val btnLogin = findViewById<MaterialButton>(R.id.login_btn)
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
    }

    private fun togglePasswordVisibility(editText: EditText) {
        if (editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        editText.setSelection(editText.text.length)
    }

    private fun navigateToMain() {
        // Register FCM token for push notifications
        FCMHelper.registerFCMToken(this)
        FCMHelper.subscribeToBookingUpdates()
        
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.reset_email)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.reset_progress)
        
        // Pre-fill with the email from login screen if available
        val loginEmailEditText = findViewById<EditText>(R.id.login_email)
        val emailText = loginEmailEditText.text.toString().trim()
        if (emailText.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            emailEditText.setText(emailText)
        }
        
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Reset", null) // Set to null to override default behavior
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
        
        alertDialog.setOnShowListener { dialog ->
            val resetButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            resetButton.setOnClickListener {
                val email = emailEditText.text.toString().trim()
                
                if (email.isEmpty()) {
                    emailEditText.error = "Email is required"
                    emailEditText.requestFocus()
                    return@setOnClickListener
                }
                
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailEditText.error = "Please enter a valid email"
                    emailEditText.requestFocus()
                    return@setOnClickListener
                }
                
                // Show progress and disable button
                progressBar.visibility = View.VISIBLE
                resetButton.isEnabled = false
                
                // Send password reset email
                Log.d(TAG, "Attempting to send password reset email to: $email")
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        progressBar.visibility = View.GONE
                        resetButton.isEnabled = true
                        
                        if (task.isSuccessful) {
                            Log.d(TAG, "Password reset email sent successfully to: $email")
                            showToast("Password reset email sent to $email\nPlease check your inbox and spam folder")
                            dialog.dismiss()
                        } else {
                            val exception = task.exception
                            val errorMessage = exception?.message ?: "Failed to send reset email"
                            Log.e(TAG, "Failed to send password reset email: $errorMessage", exception)
                            
                            // More specific error messages based on exception type
                            val userFriendlyMessage = when {
                                errorMessage.contains("no user record") -> "No account exists with this email address"
                                errorMessage.contains("network") -> "Network error. Please check your internet connection"
                                else -> "Error: $errorMessage"
                            }
                            
                            showToast(userFriendlyMessage)
                        }
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        resetButton.isEnabled = true
                        Log.e(TAG, "Exception in password reset: ${e.message}", e)
                        showToast("Error: ${e.message}")
                    }
            }
        }
        
        alertDialog.show()
    }
}