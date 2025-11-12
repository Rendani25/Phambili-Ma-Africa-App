package com.example.phambili_ma_africa

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.phambili_ma_africa.data.model.TopService
import com.example.phambili_ma_africa.util.FCMHelper
import com.google.firebase.storage.FirebaseStorage

class MainActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore
    private lateinit var userNameTextView: TextView
    private lateinit var welcomeTextView: TextView
    private lateinit var profileImage: ImageView
    private lateinit var bookNowBtn: Button
    private lateinit var seeAllTopServices: TextView
    private lateinit var seeAllFeatured: TextView
    private lateinit var searchField: EditText
    private lateinit var featuredServiceCard: androidx.cardview.widget.CardView

    private lateinit var topServicesAdapter: TopServiceAdapter
    private lateinit var topServicesRecycler: RecyclerView

    private var servicesListener: ListenerRegistration? = null
    private val allTopServices = mutableListOf<TopService>()
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentLayout(R.layout.activity_main)
            setupBaseActivity()

            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()
            initializeViews()
            checkUserLoggedIn()
        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error in onCreate: ${e.message}", e)
            Toast.makeText(this, "App initialization failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        try {
            userNameTextView = findViewById(R.id.user_name)
            welcomeTextView = findViewById(R.id.welcome_text)
            profileImage = findViewById(R.id.profile_image)
            bookNowBtn = findViewById(R.id.book_now_btn)
            seeAllTopServices = findViewById(R.id.see_all_top_services)
            seeAllFeatured = findViewById(R.id.see_all_featured)
            searchField = findViewById(R.id.search_field)
            featuredServiceCard = findViewById(R.id.featured_service_card)
            topServicesRecycler = findViewById(R.id.top_services_recycler)

            Log.d("MainActivity", "All views initialized successfully")
            setupClickListeners()
            setupSearchFunctionality()
            animateWelcomeSection()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing views: ${e.message}", e)
        }
    }

    private fun checkUserLoggedIn() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.d("MainActivity", "No user logged in, redirecting to login")
                startActivity(Intent(this, Login::class.java))
                finish()
            } else {
                Log.d("MainActivity", "User logged in: ${currentUser.email}")
                setupUserInfo(currentUser)
                setupRecyclerViews()
                setupChipGroup()
                
                // Register FCM token for push notifications
                FCMHelper.registerFCMToken(this)
                FCMHelper.subscribeToBookingUpdates()
                // Load default services immediately, then try Firestore
                loadDefaultServices()
                loadServicesFromFirestore()
                checkForBookingSuccess()
                checkUserBookingsCount()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in checkUserLoggedIn: ${e.message}", e)
        }
    }

    private fun setupUserInfo(currentUser: FirebaseUser) {
        try {
            val displayName = currentUser.displayName
            val email = currentUser.email

            userNameTextView.text = when {
                !displayName.isNullOrEmpty() -> displayName
                !email.isNullOrEmpty() -> email.substringBefore('@')
                else -> "User"
            }

            welcomeTextView.text = when {
                !displayName.isNullOrEmpty() -> "Welcome back,"
                !email.isNullOrEmpty() -> "Hello,"
                else -> "Welcome,"
            }

            Log.d("MainActivity", "User info set: ${userNameTextView.text}")
            loadUserProfileData(currentUser.uid)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up user info: ${e.message}")
            userNameTextView.text = "User"
            welcomeTextView.text = "Welcome,"
        }
    }

    private fun loadUserProfileData(userId: String) {
        try {
            // First try to load profile image from SharedPreferences
            val profileImageUri = prefs.getString("profile_image_uri", "")
            if (!profileImageUri.isNullOrEmpty()) {
                try {
                    com.bumptech.glide.Glide.with(this)
                        .load(android.net.Uri.parse(profileImageUri))
                        .circleCrop()
                        .into(profileImage)
                    Log.d("MainActivity", "Loaded profile image from SharedPreferences")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading profile image: ${e.message}")
                }
            }
            
            // Then load user data from Firestore
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        if (document.exists()) {
                            val fullName = document.getString("fullName")
                            fullName?.let { name ->
                                runOnUiThread {
                                    userNameTextView.text = name
                                    welcomeTextView.text = "Welcome back,"
                                }
                                Log.d("MainActivity", "Updated user name from profile: $name")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error processing user profile: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error loading user profile: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in loadUserProfileData: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        try {
            bookNowBtn.setOnClickListener {
                animateButtonClick(it)
                startActivityWithTransition(ServicesActivity::class.java)
            }

            profileImage.setOnClickListener {
                animateButtonClick(it)
                startActivityWithTransition(ProfileActivity::class.java)
            }

            seeAllTopServices.setOnClickListener {
                animateButtonClick(it)
                startActivityWithTransition(ServicesActivity::class.java)
            }

            seeAllFeatured.setOnClickListener {
                showFeaturedServices()
            }

            featuredServiceCard.setOnClickListener {
                animateCardClick(it)
                showFeaturedServices()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up click listeners: ${e.message}")
        }
    }

    private fun setupSearchFunctionality() {
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    performSearch(s.toString())
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun performSearch(query: String) {
        try {
            if (query.isEmpty()) {
                topServicesAdapter.updateList(allTopServices)
                findViewById<TextView>(R.id.search_results_info)?.visibility = View.GONE
                return
            }

            // First try to search in Firestore for more up-to-date results
            searchInFirestore(query)
            
            // Then filter local results as a fallback
            val filteredServices = allTopServices.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.category?.contains(query, ignoreCase = true) == true ||
                        it.description.contains(query, ignoreCase = true)
            }

            topServicesAdapter.updateList(filteredServices)

            // Update search results info
            val searchResultsInfo = findViewById<TextView>(R.id.search_results_info)
            if (filteredServices.isEmpty()) {
                searchResultsInfo?.text = "No services found for \"$query\""
                searchResultsInfo?.visibility = View.VISIBLE
            } else {
                searchResultsInfo?.text = "${filteredServices.size} services found for \"$query\""
                searchResultsInfo?.visibility = View.VISIBLE
            }

            Log.d("MainActivity", "Search results: ${filteredServices.size} services found")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error performing search: ${e.message}")
        }
    }
    
    private fun searchInFirestore(query: String) {
        try {
            db.collection("services")
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener { documents ->
                    val searchResults = mutableListOf<TopService>()
                    for (document in documents) {
                        val service = document.toObject(TopService::class.java)
                        service.id = document.id
                        
                        // Check if service matches search query
                        if (service.name.contains(query, ignoreCase = true) ||
                            service.category.contains(query, ignoreCase = true) ||
                            service.description.contains(query, ignoreCase = true)) {
                            searchResults.add(service)
                        }
                    }
                    
                    if (searchResults.isNotEmpty()) {
                        // Update adapter with Firestore results
                        allTopServices.clear()
                        allTopServices.addAll(searchResults)
                        topServicesAdapter.updateList(searchResults)
                        
                        // Update search results info
                        val searchResultsInfo = findViewById<TextView>(R.id.search_results_info)
                        searchResultsInfo?.text = "${searchResults.size} services found for \"$query\""
                        searchResultsInfo?.visibility = View.VISIBLE
                        
                        Log.d("MainActivity", "Firestore search results: ${searchResults.size} services found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error searching in Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in searchInFirestore: ${e.message}")
        }
    }

    private fun setupRecyclerViews() {
        try {
            setupTopServicesRecyclerView()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up recycler views: ${e.message}")
        }
    }

    private fun setupTopServicesRecyclerView() {
        try {
            Log.d("MainActivity", "Setting up RecyclerView")

            topServicesAdapter = TopServiceAdapter(emptyList()) { service ->
                openServiceDetailsFromTop(service)
            }

            if (::topServicesRecycler.isInitialized) {
                // Use VERTICAL layout manager for vertical scrolling
                topServicesRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                topServicesRecycler.adapter = topServicesAdapter
                // Disable nested scrolling for better performance in ScrollView
                topServicesRecycler.isNestedScrollingEnabled = false
                Log.d("MainActivity", "RecyclerView setup completed - VERTICAL layout")
            } else {
                Log.e("MainActivity", "topServicesRecycler not initialized")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up RecyclerView: ${e.message}", e)
        }
    }

    private fun loadServicesFromFirestore() {
        try {
            Log.d("MainActivity", "Loading services from Firestore...")

            servicesListener = db.collection("services")
                .whereEqualTo("isAvailable", true)
                .addSnapshotListener { snapshots, error ->
                    try {
                        if (error != null) {
                            Log.e("MainActivity", "Error loading services: ${error.message}")
                            return@addSnapshotListener
                        }

                        snapshots?.let { documents ->
                            val newServices = mutableListOf<TopService>()
                            documents.forEach { document ->
                                try {
                                    val service = document.toObject(TopService::class.java)
                                    service.id = document.id
                                    newServices.add(service)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error parsing service document: ${e.message}")
                                }
                            }

                            if (newServices.isNotEmpty()) {
                                Log.d("MainActivity", "Loaded ${newServices.size} services from Firestore")
                                allTopServices.clear()
                                allTopServices.addAll(newServices)
                                updateServicesList()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error in Firestore listener: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up Firestore listener: ${e.message}")
        }
    }

    private fun loadDefaultServices() {
        try {
            Log.d("MainActivity", "Loading default services...")

            // Use your custom drawable resources
            val defaultServices = listOf(
                TopService(
                    name = "Deep Cleaning",
                    price = "From R350",
                    imageResId = R.drawable.deep2,
                    category = "Cleaning",
                    description = "Transform your space with our comprehensive deep cleaning service. We clean every corner to perfection using eco-friendly products."
                ),
                TopService(
                    name = "Office Cleaning",
                    price = "From R500",
                    imageResId = R.drawable.offices,
                    category = "Office",
                    description = "Professional office cleaning services for a productive and healthy work environment. We handle everything from desks to common areas."
                ),
                TopService(
                    name = "Carpet Cleaning",
                    price = "From R300",
                    imageResId = R.drawable.carpet_cleaning,
                    category = "Carpet",
                    description = "Deep carpet cleaning using advanced steam technology. Removes stains, odors, and allergens for fresh, clean carpets."
                ),
                TopService(
                    name = "Window Cleaning",
                    price = "From R250",
                    imageResId = R.drawable.windowss,
                    category = "Windows",
                    description = "Crystal clear window cleaning with streak-free results. Perfect for homes, offices, and commercial buildings."
                ),
                TopService(
                    name = "Upholstery Cleaning",
                    price = "From R400",
                    imageResId = R.drawable.upholsterypcleaning,
                    category = "Furniture",
                    description = "Professional upholstery cleaning for sofas, chairs, and mattresses. Restores your furniture to like-new condition."
                ),
                TopService(
                    name = "Fumigation",
                    price = "From R550",
                    imageResId = R.drawable.fumigation,
                    category = "Pest Control",
                    description = "Complete fumigation services for pest eradication and prevention. Safe and effective treatment for all types of pests."
                )
            )

            // Only use default services temporarily while waiting for Firestore data
            if (allTopServices.isEmpty()) {
                allTopServices.clear()
                allTopServices.addAll(defaultServices)
                updateServicesList()
                Log.d("MainActivity", "Default services loaded temporarily: ${allTopServices.size}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading default services: ${e.message}", e)
            // Fallback to system images if custom images fail
            loadFallbackServices()
        }
    }

    private fun loadFallbackServices() {
        try {
            Log.d("MainActivity", "Loading fallback services with system images...")

            val fallbackServices = listOf(
                TopService(
                    name = "Deep Cleaning",
                    price = "From R350",
                    imageResId = android.R.drawable.ic_menu_edit,
                    category = "Cleaning",
                    description = "Transform your space with comprehensive deep cleaning service."
                ),
                TopService(
                    name = "Office Cleaning",
                    price = "From R500",
                    imageResId = android.R.drawable.ic_menu_edit,
                    category = "Office",
                    description = "Comprehensive office cleaning for professional workspace."
                )
            )

            allTopServices.clear()
            allTopServices.addAll(fallbackServices)
            updateServicesList()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading fallback services: ${e.message}")
        }
    }

    private fun updateServicesList() {
        try {
            runOnUiThread {
                if (::topServicesAdapter.isInitialized && ::topServicesRecycler.isInitialized) {
                    topServicesAdapter.updateList(allTopServices)
                    Log.d("MainActivity", "Services list updated with ${allTopServices.size} items")

                    // Show success message
                    if (allTopServices.isNotEmpty()) {
                        Log.d("MainActivity", "${allTopServices.size} services ready for vertical display")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating services list: ${e.message}")
        }
    }

    private fun setupChipGroup() {
        try {
            val chipGroup = findViewById<ChipGroup>(R.id.chip_group)
            chipGroup?.setOnCheckedChangeListener { group, checkedId ->
                try {
                    if (checkedId != -1) {
                        val selectedChip = findViewById<Chip>(checkedId)
                        val category = when (checkedId) {
                            R.id.chip_all -> "All"
                            R.id.chip_cleaning -> "Cleaning"
                            R.id.chip_carpet -> "Carpet"
                            else -> "All"
                        }
                        filterServicesByCategory(category)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in chip selection: ${e.message}")
                }
            }

            // Set default selection
            val chipAll = findViewById<Chip>(R.id.chip_all)
            chipAll?.isChecked = true

        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up chip group: ${e.message}")
        }
    }

    private fun filterServicesByCategory(category: String) {
        try {
            val filteredServices = when (category) {
                "All" -> allTopServices
                "Cleaning" -> allTopServices.filter {
                    it.category?.contains("Cleaning", true) == true || it.name.contains("Cleaning", true)
                }
                "Carpet" -> allTopServices.filter {
                    it.category?.contains("Carpet", true) == true || it.name.contains("Carpet", true)
                }
                "Office" -> allTopServices.filter {
                    it.category?.contains("Office", true) == true || it.name.contains("Office", true)
                }
                else -> allTopServices
            }

            if (::topServicesAdapter.isInitialized) {
                topServicesAdapter.updateList(filteredServices)
                Log.d("MainActivity", "Filtered to ${filteredServices.size} services in category: $category")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error filtering services: ${e.message}")
        }
    }

    private fun openServiceDetailsFromTop(service: TopService) {
        try {
            val intent = Intent(this, ServiceDetailsActivity::class.java).apply {
                putExtra("service_id", service.id)
                putExtra("title", service.name)
                putExtra("price", service.price)
                putExtra("description", service.description.ifEmpty { getDescriptionForService(service.name) })
                putExtra("imageResId", service.imageResId)
                putExtra("category", service.category)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening service details: ${e.message}")
            Toast.makeText(this, "Error opening service details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFeaturedServices() {
        try {
            // Get featured services from Firestore
            db.collection("services")
                .whereEqualTo("isFeatured", true)
                .whereEqualTo("isAvailable", true)
                .limit(5)
                .get()
                .addOnSuccessListener { documents ->
                    val featuredCount = documents.size()
                    if (featuredCount > 0) {
                        // Navigate to services activity with filter for featured services
                        val intent = Intent(this, ServicesActivity::class.java)
                        intent.putExtra("filter_featured", true)
                        startActivity(intent)
                    } else {
                        // If no featured services found, just show all services
                        Toast.makeText(this, "Explore our premium quality services", Toast.LENGTH_SHORT).show()
                        startActivityWithTransition(ServicesActivity::class.java)
                    }
                }
                .addOnFailureListener {
                    // On failure, just navigate to all services
                    Toast.makeText(this, "Explore our professional cleaning services", Toast.LENGTH_SHORT).show()
                    startActivityWithTransition(ServicesActivity::class.java)
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing featured services: ${e.message}")
            Toast.makeText(this, "Explore our professional cleaning services", Toast.LENGTH_SHORT).show()
            startActivityWithTransition(ServicesActivity::class.java)
        }
    }

    private fun checkUserBookingsCount() {
        try {
            val user = auth.currentUser
            user?.uid?.let { userId ->
                db.collection("bookings")
                    .whereEqualTo("customerId", userId)
                    .get()
                    .addOnSuccessListener { documents ->
                        try {
                            val bookingCount = documents.size()
                            if (bookingCount > 0) {
                                updateBookingBadge(bookingCount)
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error processing bookings: ${e.message}")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Error checking bookings: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in checkUserBookingsCount: ${e.message}")
        }
    }

    private fun updateBookingBadge(count: Int) {
        try {
            runOnUiThread {
                bookNowBtn.text = "My Bookings ($count)"
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating booking badge: ${e.message}")
        }
    }

    private fun getDescriptionForService(serviceName: String): String {
        return when {
            serviceName.contains("deep", true) -> "Transform your space with comprehensive deep cleaning service using eco-friendly products and advanced equipment."
            serviceName.contains("office", true) -> "Professional office cleaning services that create a productive and healthy work environment for your team."
            serviceName.contains("carpet", true) -> "Deep carpet cleaning using steam technology to remove stains, odors, and allergens effectively."
            serviceName.contains("window", true) -> "Streak-free window cleaning service for crystal clear views and enhanced natural lighting."
            serviceName.contains("upholstery", true) -> "Professional furniture cleaning that restores sofas, chairs, and mattresses to like-new condition."
            serviceName.contains("fumigation", true) -> "Complete pest control and fumigation services for a safe and pest-free environment."
            else -> "Professional cleaning service to keep your space spotless, hygienic, and comfortable."
        }
    }

    private fun checkForBookingSuccess() {
        try {
            if (intent.getBooleanExtra("booking_success", false)) {
                Toast.makeText(this, "Booking submitted successfully!", Toast.LENGTH_SHORT).show()
                intent.removeExtra("booking_success")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking booking success: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "Activity resumed - refreshing data")

        // Refresh user info and services when returning to main activity
        auth.currentUser?.let { user ->
            setupUserInfo(user)
        }

        if (::topServicesAdapter.isInitialized && allTopServices.isNotEmpty()) {
            topServicesAdapter.updateList(allTopServices)
        }
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        try {
            if (auth.currentUser != null) {
                moveTaskToBack(true)
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            super.onBackPressed()
        }
    }

    // ============ ANIMATION & UI HELPERS ============

    private fun animateWelcomeSection() {
        try {
            welcomeTextView.alpha = 0f
            userNameTextView.alpha = 0f
            profileImage.alpha = 0f

            welcomeTextView.animate()
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            userNameTextView.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            profileImage.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error animating welcome section: ${e.message}")
        }
    }

    private fun animateButtonClick(view: View) {
        try {
            val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 0.95f)
            scaleDown.duration = 100
            scaleDown.start()

            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f)
            scaleDownY.duration = 100
            scaleDownY.start()

            Handler(Looper.getMainLooper()).postDelayed({
                val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1f)
                scaleUp.duration = 100
                scaleUp.start()

                val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f)
                scaleUpY.duration = 100
                scaleUpY.start()
            }, 100)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error animating button: ${e.message}")
        }
    }

    private fun animateCardClick(view: View) {
        try {
            val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 0.98f)
            scaleDown.duration = 150
            scaleDown.start()

            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.98f)
            scaleDownY.duration = 150
            scaleDownY.start()

            Handler(Looper.getMainLooper()).postDelayed({
                val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1f)
                scaleUp.duration = 150
                scaleUp.start()

                val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f)
                scaleUpY.duration = 150
                scaleUpY.start()
            }, 150)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error animating card: ${e.message}")
        }
    }

    private fun startActivityWithTransition(activityClass: Class<*>) {
        try {
            val intent = Intent(this, activityClass)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting activity: ${e.message}")
            startActivity(Intent(this, activityClass))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            servicesListener?.remove()
            searchRunnable?.let { searchHandler.removeCallbacks(it) }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onDestroy: ${e.message}")
        }
    }
}