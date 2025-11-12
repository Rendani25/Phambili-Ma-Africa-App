package com.example.phambili_ma_africa

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog

class Payment : BaseActivity() {

    private lateinit var totalAmountText: TextView
    private lateinit var paymentMethods: RadioGroup
    private lateinit var confirmButton: Button

    private var totalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment) // Use the layout with drawer
        setupBaseActivity()

        initializeViews()
        setupPaymentData()
        setupConfirmButton()
    }

    private fun initializeViews() {
        totalAmountText = findViewById(R.id.totalAmountText)
        paymentMethods = findViewById(R.id.paymentMethods)
        confirmButton = findViewById(R.id.confirmPaymentButton)
    }

    private fun setupPaymentData() {
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        totalAmountText.text = "Total to Pay: R${"%.2f".format(totalAmount)}"
    }

    private fun setupConfirmButton() {
        confirmButton.setOnClickListener {
            val selectedId = paymentMethods.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            } else {
                val selectedMethod = findViewById<RadioButton>(selectedId).text.toString()
                processPayment(selectedMethod)
            }
        }
    }

    private fun processPayment(method: String) {
        AlertDialog.Builder(this)
            .setTitle("Payment Successful!")
            .setMessage("Thank you for your payment.\n\nMethod: $method\nAmount: R${"%.2f".format(totalAmount)}")
            .setPositiveButton("OK") { _, _ ->
                // Clear cart and navigate to main screen
                clearCart()
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
            }
            .show()
    }

    override fun clearCart() {
        // Clear cart data from SharedPreferences or database
        val sharedPreferences = getSharedPreferences("cart_prefs", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // Also update cart badge
        updateCartBadge(0)
    }
}