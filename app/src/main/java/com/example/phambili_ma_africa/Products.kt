package com.example.phambili_ma_africa

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

class Products : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_products)

        // Back button listener
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // Just setup the notify button for now
        val btnNotify = findViewById<Button>(R.id.notify_btn)
        val emailInput = findViewById<EditText>(R.id.email_input)

        btnNotify?.setOnClickListener {
            val email = emailInput?.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "You'll be notified when products launch!", Toast.LENGTH_SHORT).show()
                emailInput?.text?.clear()
            } else {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            }
        }
    }
}