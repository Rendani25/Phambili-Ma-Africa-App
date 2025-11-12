package com.example.phambili_ma_africa

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.*

open class BaseActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navView: NavigationView
    protected lateinit var cartSidebar: NavigationView
    protected lateinit var hamburgerBtn: ImageView
    protected lateinit var logoBtn: ImageView
    protected lateinit var userBtn: ImageView
    protected lateinit var cartBtn: ImageView
    protected lateinit var cartBadge: TextView
    protected lateinit var prefs: SharedPreferences

    // Cart sidebar views
    protected lateinit var closeCartBtn: ImageButton
    protected lateinit var cartItemsRecycler: RecyclerView
    protected lateinit var emptyCartView: LinearLayout
    protected lateinit var cartTotal: TextView
    protected lateinit var checkoutBtn: Button
    protected lateinit var cartSidebarPayment: LinearLayout

    // Cart data
    protected val cartItems = mutableListOf<CartItem>()
    protected var cartTotalAmount = 0.0
    protected lateinit var cartSidebarAdapter: CartSidebarAdapter

    // Toast management
    private var currentToast: Toast? = null

    // ============ LANGUAGE SUPPORT ADDITIONS ============
    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        super.attachBaseContext(updateBaseContextLocale(newBase, languageCode))
    }

    override fun getResources(): Resources {
        val resources = super.getResources()
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("language", "en") ?: "en"
        val locale = Locale(languageCode)
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        return createConfigurationContext(configuration).resources
    }

    private fun updateBaseContextLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return context.createConfigurationContext(configuration)
    }

    private fun applySavedLanguage() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("language", "en") ?: "en"
        setAppLocale(savedLanguage)
    }

    private fun applyDarkMode() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val nightMode = sharedPreferences.getBoolean("nightMode", false)
        if (nightMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = resources
        val configuration: Configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
    // ============ END LANGUAGE SUPPORT ============

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language and theme before setting content
        applySavedLanguage()
        applyDarkMode()

        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.cart_sidebar)
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        setupBaseActivity()
        setupBackPressHandler()
    }

    protected fun setupBaseActivity() {
        try {
            initializeViews()
            setupNavigationDrawer()
            setupCartSidebar()
            setupClickListeners()
            setupBottomNavigation()
            loadCartData()
            updateCartBadgeFromPrefs()
            updateCartSidebar()
        } catch (e: Exception) {
            showToast("Error setting up base activity: ${e.message}")
        }
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        cartSidebar = findViewById(R.id.cart_sidebar)
        hamburgerBtn = findViewById(R.id.hamburger_btn)
        logoBtn = findViewById(R.id.logo)
        userBtn = findViewById(R.id.user_btn)
        cartBtn = findViewById(R.id.cart_btn)
        cartBadge = findViewById(R.id.cart_badge)

        // Initialize cart sidebar views
        val headerView = cartSidebar.getHeaderView(0)
        closeCartBtn = headerView.findViewById(R.id.close_cart_btn)
        cartItemsRecycler = headerView.findViewById(R.id.cart_items_recycler)
        emptyCartView = headerView.findViewById(R.id.empty_cart)
        cartTotal = headerView.findViewById(R.id.cart_total)
        checkoutBtn = headerView.findViewById(R.id.checkout_btn)
        cartSidebarPayment = headerView.findViewById(R.id.cart_payment_methods)
    }

    private fun setupClickListeners() {
        hamburgerBtn.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        logoBtn.setOnClickListener { navigateToMainActivity() }
        userBtn.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        cartBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
            updateCartSidebar()
        }
        closeCartBtn.setOnClickListener { drawerLayout.closeDrawer(GravityCompat.END) }
    }

    private fun setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> navigateTo(MainActivity::class.java)
                R.id.nav_services -> navigateTo(ServicesActivity::class.java)
                R.id.nav_products -> navigateTo(Products::class.java)
                R.id.nav_booking -> navigateTo(Booking::class.java)
                R.id.nav_booking_history -> navigateTo(BookingHistoryActivity::class.java) // Added  line
                R.id.nav_profile -> navigateTo(ProfileActivity::class.java)
                R.id.nav_settings -> navigateTo(SettingsActivity::class.java)
                R.id.nav_logout -> performLogout()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateTo(MainActivity::class.java)
                R.id.nav_services -> navigateTo(ServicesActivity::class.java)
                R.id.nav_products -> navigateTo(Products::class.java)
                R.id.nav_profile -> navigateTo(ProfileActivity::class.java)
                R.id.nav_settings -> navigateTo(SettingsActivity::class.java)
            }
            true
        }
    }

    private fun setupCartSidebar() {
        // Setup RecyclerView for sidebar
        cartItemsRecycler.layoutManager = LinearLayoutManager(this)
        cartSidebarAdapter = CartSidebarAdapter(cartItems) { item, action ->
            when (action) {
                "remove" -> removeFromCart(item)
                "quantity_change" -> updateCartTotal()
                "view_details" -> openCartDetails()
            }
        }
        cartItemsRecycler.adapter = cartSidebarAdapter

        checkoutBtn.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                openCartDetails()
            }
        }
    }

    protected fun openCartDetails() {
        drawerLayout.closeDrawer(GravityCompat.END)
        val intent = Intent(this, Cart::class.java)
        startActivity(intent)
    }

    // Cart management methods
    protected fun loadCartData() {
        // Load cart data from shared preferences or database
        // For now, using sample data if empty
        if (cartItems.isEmpty()) {
            // You can load from SharedPreferences or database here
            val savedCount = prefs.getInt("cart_count", 0)
            if (savedCount > 0) {
                // Load actual cart items from your data source
                cartItems.addAll(getSavedCartItems())
            }
        }
        updateCartTotal()
    }

    protected fun updateCartTotal() {
        cartTotalAmount = cartItems.sumOf { it.price * it.quantity }
        updateCartBadge(cartItems.size)
        updateCartSidebar()
    }

    protected fun updateCartBadge(count: Int) {
        runOnUiThread {
            cartBadge.text = count.toString()
            cartBadge.visibility = if (count > 0) TextView.VISIBLE else TextView.GONE
            prefs.edit().putInt("cart_count", count).apply()
        }
    }

    protected fun updateCartSidebar() {
        runOnUiThread {
            val itemCount = cartItems.size

            // Update empty state
            if (itemCount == 0) {
                emptyCartView.visibility = View.VISIBLE
                cartItemsRecycler.visibility = View.GONE
                checkoutBtn.isEnabled = false
                checkoutBtn.alpha = 0.5f
                cartSidebarPayment.visibility = View.GONE
            } else {
                emptyCartView.visibility = View.GONE
                cartItemsRecycler.visibility = View.VISIBLE
                checkoutBtn.isEnabled = true
                checkoutBtn.alpha = 1f
                cartSidebarPayment.visibility = View.VISIBLE

                // Update adapter
                cartSidebarAdapter.updateItems(cartItems)
            }

            // Update total
            cartTotal.text = "R${String.format("%.2f", cartTotalAmount)}"
            checkoutBtn.text = "View Cart - R${String.format("%.2f", cartTotalAmount)}"
        }
    }

    protected fun addToCart(item: CartItem) {
        val existingItem = cartItems.find { it.name == item.name }
        if (existingItem != null) {
            existingItem.quantity += item.quantity
        } else {
            cartItems.add(item)
        }
        updateCartTotal()
        showToast("${item.name} added to cart")
    }

    protected open fun removeFromCart(item: CartItem) {
        cartItems.remove(item)
        updateCartTotal()
        showToast("${item.name} removed from cart")
    }

    protected open fun clearCart() {
        cartItems.clear()
        updateCartTotal()
        showToast("Cart cleared")
    }

    private fun updateCartBadgeFromPrefs() {
        val count = prefs.getInt("cart_count", cartItems.size)
        updateCartBadge(count)
    }

    private fun getSavedCartItems(): List<CartItem> {
        // Implement your logic to load cart items from database or SharedPreferences
        // This is a sample implementation
        return listOf(
            CartItem("Deep Cleaning Service", 350.0, 1, "Professional deep cleaning for your home"),
            CartItem("Carpet Cleaning", 300.0, 1, "Steam cleaning for carpets and rugs")
        )
    }

    // Updated showToast to prevent spam
    protected fun showToast(message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }

    protected fun setContentLayout(layoutResId: Int) {
        val contentFrame = findViewById<FrameLayout>(R.id.fragment_container)
        contentFrame?.removeAllViews()
        layoutInflater.inflate(layoutResId, contentFrame, true)
    }

    private fun navigateTo(activity: Class<*>) {
        if (this::class.java != activity) {
            startActivity(Intent(this, activity))
        }
    }

    private fun navigateToMainActivity() {
        navigateTo(MainActivity::class.java)
    }

    private fun performLogout() {
        try {
            // Sign out from Firebase Authentication
            FirebaseAuth.getInstance().signOut()
            
            // Clear all SharedPreferences data
            prefs.edit().clear().apply()
            
            // Clear app preferences (language and theme settings are preserved)
            val appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val savedLanguage = appPrefs.getString("language", "en")
            val savedNightMode = appPrefs.getBoolean("nightMode", false)
            
            // Clear cart preferences
            val cartPrefs = getSharedPreferences("cart_prefs", MODE_PRIVATE)
            cartPrefs.edit().clear().apply()
            
            // Restore language and theme preferences
            appPrefs.edit().apply {
                clear()
                putString("language", savedLanguage)
                putBoolean("nightMode", savedNightMode)
                apply()
            }
            
            // Show logout message
            showToast("Logged out successfully")
            
            // Navigate to Login screen and clear activity stack
            val intent = Intent(this, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            showToast("Error during logout: ${e.message}")
            // Still navigate to login even if there's an error
            val intent = Intent(this, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // Cart Item data class
    data class CartItem(
        val name: String,
        val price: Double,
        var quantity: Int,
        val description: String
    )

    // Cart Sidebar Adapter
    inner class CartSidebarAdapter(
        private var items: List<CartItem>,
        private val onItemAction: (CartItem, String) -> Unit
    ) : RecyclerView.Adapter<CartSidebarAdapter.CartSidebarViewHolder>() {

        inner class CartSidebarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.sidebar_item_name)
            val price: TextView = itemView.findViewById(R.id.sidebar_item_price)
            val quantity: TextView = itemView.findViewById(R.id.sidebar_item_quantity)
            val removeButton: ImageButton = itemView.findViewById(R.id.sidebar_remove_btn)
            val increaseButton: ImageButton = itemView.findViewById(R.id.sidebar_increase_btn)
            val decreaseButton: ImageButton = itemView.findViewById(R.id.sidebar_decrease_btn)
            val itemLayout: LinearLayout = itemView.findViewById(R.id.sidebar_item_layout)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartSidebarViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cart_sidebar, parent, false)
            return CartSidebarViewHolder(view)
        }

        override fun onBindViewHolder(holder: CartSidebarViewHolder, position: Int) {
            val item = items[position]

            holder.name.text = item.name
            holder.price.text = "R${String.format("%.2f", item.price)}"
            holder.quantity.text = item.quantity.toString()

            holder.removeButton.setOnClickListener {
                onItemAction(item, "remove")
            }

            holder.increaseButton.setOnClickListener {
                item.quantity++
                holder.quantity.text = item.quantity.toString()
                onItemAction(item, "quantity_change")
            }

            holder.decreaseButton.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    holder.quantity.text = item.quantity.toString()
                    onItemAction(item, "quantity_change")
                } else {
                    onItemAction(item, "remove")
                }
            }

            holder.itemLayout.setOnClickListener {
                onItemAction(item, "view_details")
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<CartItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}