package com.example.phambili_ma_africa

import android.os.Bundle
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_gallery)
        setupBaseActivity()

        val galleryRecycler = findViewById<RecyclerView>(R.id.full_gallery_recycler)
        galleryRecycler.layoutManager = GridLayoutManager(this, 3)

        val serviceName = intent.getStringExtra("serviceName") ?: ""
        val images = getGalleryImagesForService(serviceName)

        galleryRecycler.adapter = GalleryAdapter(images)

        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener { finish() }
    }

    private fun getGalleryImagesForService(serviceName: String): List<Int> {
        return when {
            serviceName.contains("carpet", ignoreCase = true) -> listOf(
                R.drawable.carpet_cleaning,
                R.drawable.cartpt,
                R.drawable.carpet,
                R.drawable.carpet_cleaning,
                R.drawable.cartpt,
                R.drawable.carpet
            )
            serviceName.contains("window", ignoreCase = true) -> listOf(
                R.drawable.cleaningwindows,
                R.drawable.windowcleaning,
                R.drawable.windowss,
                R.drawable.windowsss,
                R.drawable.wwww

            )
            serviceName.contains("office", ignoreCase = true) -> listOf(
                R.drawable.officecleaning,
                R.drawable.offices,
                R.drawable.okffice,
                R.drawable.officess
            )
            serviceName.contains("upholstery", ignoreCase = true) -> listOf(
                R.drawable.upholsterycleaning,
                R.drawable.upp,
                R.drawable.upholestry,
                R.drawable.up,
                R.drawable.upholsterypcleaning


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
                R.drawable.gar,
                R.drawable.gardem,
                R.drawable.garden
            )
            else -> listOf(
                R.drawable.cleanings,
                R.drawable.deep,
                R.drawable.deepcleaning
            )
        }
    }
}
