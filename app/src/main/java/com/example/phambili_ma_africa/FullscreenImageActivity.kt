package com.example.phambili_ma_africa

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class FullscreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        val imageView = findViewById<ImageView>(R.id.fullscreen_image)
        val drawableRes = intent.getStringExtra("image")?.removePrefix("@drawable/") ?: ""

        if (drawableRes.isNotEmpty()) {
            val resId = resources.getIdentifier(drawableRes, "drawable", packageName)
            imageView.setImageResource(resId)
        }

        imageView.setOnClickListener { finish() } // tap to close
    }
}
