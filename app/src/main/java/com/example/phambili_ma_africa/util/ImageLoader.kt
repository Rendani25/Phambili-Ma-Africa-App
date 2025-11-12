package com.example.phambili_ma_africa.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.phambili_ma_africa.R
import com.google.firebase.storage.FirebaseStorage

object ImageLoader {
    
    fun loadServiceImage(imageUrl: String, imageView: ImageView) {
        if (imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder_image)
            return
        }
        
        // If it's a Firebase Storage URL
        if (imageUrl.startsWith("gs://")) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(imageView.context)
                    .load(uri)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
            }
        } else {
            // Direct HTTP URL
            Glide.with(imageView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }
    }
    
    fun loadProfileImage(imageUrl: String, imageView: ImageView) {
        if (imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.profile_placeholder)
            return
        }
        
        // If it's a Firebase Storage URL
        if (imageUrl.startsWith("gs://")) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(imageView.context)
                    .load(uri)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
            }
        } else {
            // Direct HTTP URL
            Glide.with(imageView.context)
                .load(imageUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }
    }
    
    fun loadProductImage(imageUrl: String, imageView: ImageView) {
        if (imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder_image)
            return
        }
        
        // If it's a Firebase Storage URL
        if (imageUrl.startsWith("gs://")) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(imageView.context)
                    .load(uri)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
            }
        } else {
            // Direct HTTP URL
            Glide.with(imageView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
        }
    }
}
