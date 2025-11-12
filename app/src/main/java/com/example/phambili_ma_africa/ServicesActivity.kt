package com.example.phambili_ma_africa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phambili_ma_africa.data.model.Service
import com.example.phambili_ma_africa.data.repository.ServicesRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ServicesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var servicesCount: TextView
    private lateinit var progressBar: ProgressBar
    
    private val allServices = mutableListOf<Service>()
    private lateinit var serviceAdapter: ServiceAdapter
    private val servicesRepository = ServicesRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_services)
        setupBaseActivity()

        initializeViews()
        loadAllServices()
        setupRecyclerView()
        setupBackButton()
    }

    private fun initializeViews() {
        try {
            servicesCount = findViewById(R.id.services_count)
            recyclerView = findViewById(R.id.services_list)
            progressBar = findViewById(R.id.progress_bar)
            
            servicesCount.text = getString(R.string.services_available, 0)
            progressBar.visibility = android.view.View.VISIBLE
        } catch (e: Exception) {
            Log.e("ServicesActivity", "Error initializing views: ${e.message}")
        }
    }

    private fun setupBackButton() {
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow?.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadAllServices() {
        allServices.clear()
        progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            servicesRepository.getAvailableServices().collectLatest { services ->
                allServices.clear()
                allServices.addAll(services)
                serviceAdapter.notifyDataSetChanged()
                updateServicesCount()
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        try {
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.setHasFixedSize(true)

            serviceAdapter = ServiceAdapter(
                services = allServices,
                onItemClick = { service -> openServiceDetails(service) },
                onBookClick = { service -> openBooking(service) }
            )
            
            recyclerView.adapter = serviceAdapter
            updateServicesCount()
        } catch (e: Exception) {
            Log.e("ServicesActivity", "Error setting up RecyclerView: ${e.message}")
        }
    }


    private fun openServiceDetails(service: Service) {
        try {
            Log.d("ServicesActivity", "Opening details for: ${service.Name} with ID: ${service.ID}")
            val intent = Intent(this, ServiceDetailsActivity::class.java).apply {
                // Pass the service ID as the primary identifier
                putExtra("serviceId", service.ID)
                
                // Also pass other details as fallback
                putExtra("title", service.Name)
                putExtra("price", "${service.Duration} min")
                putExtra("rating", "4.5") // Default rating
                putExtra("reviews", "10") // Default reviews
                putExtra("imageUrl", service.Image_URL)
                putExtra("description", service.Description)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        } catch (e: Exception) {
            Log.e("ServicesActivity", "Error opening service details: ${e.message}")
            Toast.makeText(this, "Error opening service details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBooking(service: Service) {
        try {
            Log.d("ServicesActivity", "Opening booking for: ${service.Name}")
            val intent = Intent(this, Booking::class.java).apply {
                putExtra("serviceName", service.Name)
                putExtra("serviceId", service.ID)
                putExtra("duration", service.Duration.toString())
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        } catch (e: Exception) {
            Log.e("ServicesActivity", "Error opening booking: ${e.message}")
            Toast.makeText(this, "Error opening booking", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateServicesCount() {
        try {
            val count = serviceAdapter.itemCount
            servicesCount.text = getString(R.string.services_available, count)
        } catch (e: Exception) {
            Log.e("ServicesActivity", "Error updating count: ${e.message}")
        }
    }
}