package com.example.phambili_ma_africa

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private val TAG = "SettingsActivity"

    private lateinit var switchDarkMode: Switch
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnSaveSettings: Button
    private lateinit var progressBar: ProgressBar
    
    private val auth = FirebaseAuth.getInstance()
    private var progressDialog: ProgressDialog? = null

    // Fixed language codes - using correct Android locale codes
    private val languageCodes = arrayOf("en", "zu", "xh", "af", "st")
    private val languageNames = arrayOf("English", "Zulu", "Xhosa", "Afrikaans", "Sesotho")

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before setting content view
        applySavedTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Back button listener
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }

        // Initialize views
        switchDarkMode = findViewById(R.id.switchDarkMode)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        progressBar = findViewById(R.id.progressBar)

        // Setup language spinner
        setupLanguageSpinner()

        // Load saved settings
        loadSettings()

        // Dark mode toggle listener
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }

        // Save settings button
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val nightMode = sharedPreferences.getBoolean("nightMode", false)
        if (nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        // Set current language selection
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentLangCode = sharedPref.getString("language", "en") ?: "en"
        val position = languageCodes.indexOf(currentLangCode)
        if (position >= 0) {
            spinnerLanguage.setSelection(position)
        }
    }

    private fun toggleDarkMode(enable: Boolean) {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (enable) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Toast.makeText(this, getString(R.string.dark_mode_enabled), Toast.LENGTH_SHORT).show()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Toast.makeText(this, getString(R.string.light_mode_enabled), Toast.LENGTH_SHORT).show()
        }

        editor.putBoolean("nightMode", enable)
        editor.apply()
    }

    private fun loadSettings() {
        progressBar.visibility = View.VISIBLE
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Dark Mode
        switchDarkMode.isChecked = sharedPref.getBoolean("nightMode", false)
        
        // Get current user ID from Firebase Auth
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            // Fallback to SharedPreferences if not logged in with Firebase
            loadFromSharedPreferences(sharedPref)
            return
        }
        
        // Try to load from Firebase
        lifecycleScope.launch {
            try {
                val customerRepository = com.example.phambili_ma_africa.repository.CustomerRepository()
                val customer = customerRepository.getCustomerById(currentUserId)
                
                if (customer != null) {
                    // Update UI with Firebase data
                    etUsername.setText(customer.Full_Name)
                    etEmail.setText(customer.Email)
                    etPhone.setText(customer.Phone)
                    
                    // Also save to SharedPreferences for other parts of the app
                    sharedPref.edit().apply {
                        putString("username", customer.Full_Name)
                        putString("email", customer.Email)
                        putString("phone", customer.Phone)
                        apply()
                    }
                } else {
                    // Fallback to SharedPreferences
                    loadFromSharedPreferences(sharedPref)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading customer data: ${e.message}")
                Toast.makeText(this@SettingsActivity, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                
                // Fallback to SharedPreferences
                loadFromSharedPreferences(sharedPref)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadFromSharedPreferences(sharedPref: android.content.SharedPreferences) {
        etUsername.setText(sharedPref.getString("username", ""))
        etEmail.setText(sharedPref.getString("email", ""))
        etPhone.setText(sharedPref.getString("phone", ""))
        progressBar.visibility = View.GONE
    }

    private fun saveSettings() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val selectedLanguagePosition = spinnerLanguage.selectedItemPosition

        if (selectedLanguagePosition < 0 || selectedLanguagePosition >= languageCodes.size) {
            Toast.makeText(this, "Invalid language selection", Toast.LENGTH_SHORT).show()
            return
        }

        val languageCode = languageCodes[selectedLanguagePosition]

        // Validate inputs
        if (username.isEmpty()) {
            etUsername.error = getString(R.string.username_required)
            etUsername.requestFocus()
            return
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.valid_email_required)
            etEmail.requestFocus()
            return
        }
        
        // Show loading dialog
        showProgressDialog("Saving settings...")

        // Save to SharedPreferences
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val currentLangCode = sharedPref.getString("language", "en") ?: "en"

        with(sharedPref.edit()) {
            putString("username", username)
            putString("email", email)
            putString("phone", phone)
            putString("language", languageCode)
            apply()
        }
        
        // Get current user ID from Firebase Auth
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId != null) {
            // Save to Firebase if user is logged in
            lifecycleScope.launch {
                try {
                    val customerRepository = com.example.phambili_ma_africa.repository.CustomerRepository()
                    val success = customerRepository.updateCustomerProfile(
                        customerId = currentUserId, 
                        fullName = username, 
                        phone = phone
                    )
                    
                    if (success) {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Saved locally, but failed to update database", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving profile: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "Settings saved locally only", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    hideProgressDialog()
                    
                    // Check if language changed and restart app properly
                    if (currentLangCode != languageCode) {
                        // Set the new locale
                        setAppLocale(languageCode)
                        // Restart the application properly
                        restartApp()
                    } else {
                        finish() // Just close if no language change
                    }
                }
            }
        } else {
            // No Firebase user, just finish
            hideProgressDialog()
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            
            // Check if language changed and restart app properly
            if (currentLangCode != languageCode) {
                // Set the new locale
                setAppLocale(languageCode)
                // Restart the application properly
                restartApp()
            } else {
                finish() // Just close if no language change
            }
        }
    }

    private fun setAppLocale(languageCode: String) {
        val locale = when (languageCode) {
            "st" -> Locale("st") // Southern Sotho
            "zu" -> Locale("zu") // Zulu
            "xh" -> Locale("xh") // Xhosa
            "af" -> Locale("af") // Afrikaans
            else -> Locale.ENGLISH // English
        }

        Locale.setDefault(locale)

        val resources: Resources = resources
        val configuration: Configuration = resources.configuration
        configuration.setLocale(locale)

        // For newer Android versions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            createConfigurationContext(configuration)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finishAffinity()
    }

    // Helper function to get current language name
    fun getCurrentLanguageName(): String {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val langCode = sharedPref.getString("language", "en") ?: "en"
        val position = languageCodes.indexOf(langCode)
        return if (position >= 0) languageNames[position] else "English"
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