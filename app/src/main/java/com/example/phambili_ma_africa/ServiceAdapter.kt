package com.example.phambili_ma_africa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.phambili_ma_africa.R
import com.example.phambili_ma_africa.data.model.Service
import com.example.phambili_ma_africa.util.ImageLoader

class ServiceAdapter(
    private val services: List<Service>,
    private val onItemClick: (Service) -> Unit,
    private val onBookClick: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceImage: ImageView = itemView.findViewById(R.id.service_image)
        val serviceName: TextView = itemView.findViewById(R.id.service_name)
        val servicePrice: TextView = itemView.findViewById(R.id.service_price)
        val serviceRating: TextView = itemView.findViewById(R.id.service_rating)
        val serviceReviews: TextView = itemView.findViewById(R.id.service_reviews)
        val bookButton: TextView? = itemView.findViewById(R.id.book_now_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_card, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]

        // Load image with ImageLoader
        ImageLoader.loadServiceImage(service.Image_URL, holder.serviceImage)

        // Set text values
        holder.serviceName.text = service.Name
        holder.servicePrice.text = "${service.Duration} min"
        holder.serviceRating.text = "★ 4.5" // Default rating since it's not in the model
        holder.serviceReviews.text = "(10 reviews)" // Default reviews since it's not in the model

        // Set click listeners
        holder.itemView.setOnClickListener {
            onItemClick(service)
        }

        holder.bookButton?.setOnClickListener {
            onBookClick(service)
        }
    }

    override fun getItemCount() = services.size
}