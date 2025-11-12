package com.example.phambili_ma_africa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.phambili_ma_africa.data.model.TopService

class TopServiceAdapter(
    private var services: List<TopService>,
    private val onItemClick: (TopService) -> Unit
) : RecyclerView.Adapter<TopServiceAdapter.TopServiceViewHolder>() {

    class TopServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceImage: ImageView = itemView.findViewById(R.id.service_image)
        val serviceName: TextView = itemView.findViewById(R.id.service_name)
        val servicePrice: TextView = itemView.findViewById(R.id.service_price)
        val serviceCategory: TextView = itemView.findViewById(R.id.service_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_service, parent, false) // Make sure this matches your layout
        return TopServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopServiceViewHolder, position: Int) {
        val service = services[position]

        // Set service data
        holder.serviceName.text = service.name
        holder.servicePrice.text = service.price
        holder.serviceCategory.text = service.category

        // Set image safely
        try {
            if (service.imageResId != 0) {
                holder.serviceImage.setImageResource(service.imageResId)
            } else {
                // Fallback to a default image
                holder.serviceImage.setImageResource(android.R.drawable.ic_menu_edit)
            }
        } catch (e: Exception) {
            // If image loading fails, use default
            holder.serviceImage.setImageResource(android.R.drawable.ic_menu_edit)
        }

        holder.itemView.setOnClickListener {
            onItemClick(service)
        }
    }

    override fun getItemCount() = services.size

    fun updateList(newList: List<TopService>) {
        services = newList
        notifyDataSetChanged()
    }
}