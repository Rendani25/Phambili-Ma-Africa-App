package com.example.phambili_ma_africa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phambili_ma_africa.data.model.Service
import com.example.phambili_ma_africa.data.repository.ServicesRepository
import com.example.phambili_ma_africa.util.ImageLoader
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ServiceDetailsActivity : BaseActivity() {

    private val servicesRepository = ServicesRepository()
    private val db = FirebaseFirestore.getInstance()
    private var serviceId = ""
    private var service: Service? = null
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_service_details)
        setupBaseActivity()
        
        // Initialize progress bar
        progressBar = findViewById(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE
        
        // Get service ID from intent
        serviceId = intent.getStringExtra("serviceId") ?: ""
        
        if (serviceId.isNotEmpty()) {
            loadServiceFromDatabase(serviceId)
        } else {
            // Fallback to old method if no service ID is provided
            setupServiceDetailsFromIntent()
            setupGallery()
            progressBar.visibility = View.GONE
        }
        
        setupClickListeners()

        // Back button listener
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }
    }

    private fun loadServiceFromDatabase(serviceId: String) {
        lifecycleScope.launch {
            try {
                // Get service details from repository
                service = servicesRepository.getServiceById(serviceId)
                
                if (service != null) {
                    // Display service details
                    displayServiceDetails(service!!)
                    // Setup gallery based on service name
                    setupGallery(service!!.Name)
                } else {
                    // Fallback to intent data if service not found
                    showToast("Service details not found")
                    setupServiceDetailsFromIntent()
                    setupGallery()
                }
            } catch (e: Exception) {
                Log.e("ServiceDetails", "Error loading service: ${e.message}")
                showToast("Error loading service details")
                // Fallback to intent data
                setupServiceDetailsFromIntent()
                setupGallery()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun displayServiceDetails(service: Service) {
        // Set service title
        findViewById<TextView>(R.id.service_title).text = service.Name
        
        // Set service price/duration
        val durationText = "${service.Duration} min"
        findViewById<TextView>(R.id.service_price).text = durationText
        
        // Set default rating and reviews
        findViewById<TextView>(R.id.service_rating).text = "★ 4.5"
        findViewById<TextView>(R.id.service_reviews).text = "(10 reviews)"
        
        // Load service image
        val serviceImage = findViewById<ImageView>(R.id.service_image)
        ImageLoader.loadServiceImage(service.Image_URL, serviceImage)
        
        // Set service description
        val formattedDescription = HtmlCompat.fromHtml(service.Description, HtmlCompat.FROM_HTML_MODE_LEGACY)
        findViewById<TextView>(R.id.service_description).text = formattedDescription
    }
    
    private fun setupServiceDetailsFromIntent() {
        val title = intent.getStringExtra("title") ?: "Service"
        val price = intent.getStringExtra("price") ?: "R0"
        val rating = intent.getDoubleExtra("rating", 0.0)
        val reviews = intent.getIntExtra("reviews", 0)
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val description = intent.getStringExtra("description") ?: "No description available."

        findViewById<TextView>(R.id.service_title).text = title
        findViewById<TextView>(R.id.service_price).text = price
        findViewById<TextView>(R.id.service_rating).text = "★ $rating"
        findViewById<TextView>(R.id.service_reviews).text = "($reviews reviews)"

        // Load image using ImageLoader
        val serviceImage = findViewById<ImageView>(R.id.service_image)
        if (imageUrl.isNotEmpty()) {
            ImageLoader.loadServiceImage(imageUrl, serviceImage)
        } else {
            // Fallback to resource ID if provided
            val imageResId = intent.getIntExtra("imageResId", R.drawable.cleanings)
            serviceImage.setImageResource(imageResId)
        }

        val formattedDescription = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY)
        findViewById<TextView>(R.id.service_description).text = formattedDescription
    }

    private fun setupGallery(serviceName: String = "") {
        val name = if (serviceName.isNotEmpty()) serviceName else intent.getStringExtra("title") ?: ""
        val galleryImages = getGalleryImagesForService(name)
        val galleryRecycler = findViewById<RecyclerView>(R.id.gallery_recycler)
        galleryRecycler.layoutManager = GridLayoutManager(this, 3)
        galleryRecycler.adapter = GalleryAdapter(galleryImages)
    }

    private fun getGalleryImagesForService(serviceName: String): List<Int> {
        return when {
            serviceName.contains("carpet", ignoreCase = true) -> listOf(
                R.drawable.carpet_cleaning,
                R.drawable.cartpt,
                R.drawable.carpet
            )
            serviceName.contains("window", ignoreCase = true) -> listOf(
                R.drawable.cleaningwindows,
                R.drawable.windowcleaning,
                R.drawable.windowsss,
                R.drawable.wwww,
                R.drawable.windowss
            )
            serviceName.contains("office", ignoreCase = true) -> listOf(
                R.drawable.officecleaning,
                R.drawable.offices,
                R.drawable.of,
                R.drawable.officess
            )
            serviceName.contains("upholstery", ignoreCase = true) -> listOf(
                R.drawable.upholsterycleaning,
                R.drawable.upp,
                R.drawable.up,
                R.drawable.upholsterypcleaning,
                R.drawable.upholestry
            )
            serviceName.contains("fumigation", ignoreCase = true) -> listOf(
                R.drawable.fumigation,
                R.drawable.gation,
                R.drawable.fume
            )
            serviceName.contains("Pest Control", ignoreCase = true) -> listOf(
                R.drawable.pest,
                R.drawable.pestcontrol,
                R.drawable.pests,
                R.drawable.pestq
            )
            serviceName.contains("Gardening", ignoreCase = true) -> listOf(
                R.drawable.gardening,
                R.drawable.gardening,
                R.drawable.gardening
            )
            else -> listOf(
                R.drawable.cleanings,
                R.drawable.deep,
                R.drawable.deepcleaning
            )
        }
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.book_now_btn).setOnClickListener {
            // Use service object if available, otherwise fall back to intent extras
            val serviceName = service?.Name ?: intent.getStringExtra("title") ?: ""
            val serviceId = service?.ID ?: intent.getStringExtra("serviceId") ?: ""
            val duration = service?.Duration?.toString() ?: ""
            val price = if (duration.isNotEmpty()) "$duration min" else intent.getStringExtra("price") ?: ""

            val intent = Intent(this, Booking::class.java).apply {
                putExtra("serviceName", serviceName)
                putExtra("serviceId", serviceId)
                putExtra("duration", duration)
                putExtra("price", price)
            }
            startActivity(intent)
        }

        findViewById<TextView>(R.id.see_all).setOnClickListener {
            val serviceName = service?.Name ?: intent.getStringExtra("title") ?: ""
            val intent = Intent(this, GalleryActivity::class.java).apply {
                putExtra("serviceName", serviceName)
            }
            startActivity(intent)
        }
    }

    // Inner GalleryAdapter class
    private inner class GalleryAdapter(private val images: List<Int>) :
        RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

        inner class GalleryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val img: ImageView = view.findViewById(R.id.gallery_image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery_image, parent, false)
            return GalleryViewHolder(view)
        }

        override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
            holder.img.setImageResource(images[position])

            holder.itemView.setOnClickListener {
                // You can implement full screen image viewer here
            }
        }

        override fun getItemCount() = images.size
    }
}