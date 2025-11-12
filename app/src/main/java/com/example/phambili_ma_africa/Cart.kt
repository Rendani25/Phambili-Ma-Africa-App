package com.example.phambili_ma_africa

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Cart : BaseActivity() {

    private lateinit var cartItemsText: TextView
    private lateinit var totalAmountText: TextView
    private lateinit var checkoutButton: Button
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var emptyCartText: TextView
    private lateinit var continueShoppingButton: Button
    private lateinit var clearCartButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_cart)

        initializeViews()
        setupClickListeners()
        updateCartDisplay()
    }

    private fun initializeViews() {
        cartItemsText = findViewById(R.id.cartItemsText)
        totalAmountText = findViewById(R.id.totalAmountText)
        checkoutButton = findViewById(R.id.checkoutButton)
        cartRecyclerView = findViewById(R.id.cartRecyclerView)
        emptyCartText = findViewById(R.id.emptyCartText)
        continueShoppingButton = findViewById(R.id.continueShoppingButton)
        clearCartButton = findViewById(R.id.clearCartButton)

        // Setup RecyclerView
        cartRecyclerView.layoutManager = LinearLayoutManager(this)
        cartRecyclerView.adapter = CartAdapter(cartItems) { item, action ->
            when (action) {
                "remove" -> removeFromCart(item)
                "quantity_change" -> updateTotalAmount()
            }
        }
    }

    private fun setupClickListeners() {
        // Back button listener
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }

        checkoutButton.setOnClickListener {
            proceedToCheckout()
        }

        continueShoppingButton.setOnClickListener {
            continueShopping()
        }

        clearCartButton.setOnClickListener {
            clearCart()
        }
    }

    private fun updateCartDisplay() {
        val isEmpty = cartItems.isEmpty()

        // Update visibility based on cart state
        cartRecyclerView.visibility = if (isEmpty) RecyclerView.GONE else RecyclerView.VISIBLE
        emptyCartText.visibility = if (isEmpty) TextView.VISIBLE else TextView.GONE
        checkoutButton.visibility = if (isEmpty) Button.GONE else Button.VISIBLE
        clearCartButton.visibility = if (isEmpty) Button.GONE else Button.VISIBLE
        continueShoppingButton.visibility = if (isEmpty) Button.VISIBLE else Button.GONE

        // Update adapter
        (cartRecyclerView.adapter as? CartAdapter)?.updateItems(cartItems)
        updateTotalAmount()
    }

    private fun updateTotalAmount() {
        cartTotalAmount = cartItems.sumOf { it.price * it.quantity }
        totalAmountText.text = "Total: R${String.format("%.2f", cartTotalAmount)}"

        // Update checkout button text with total
        checkoutButton.text = "Proceed to Checkout - R${String.format("%.2f", cartTotalAmount)}"

        // Update badge in base activity
        updateCartBadge(cartItems.size)
    }

    override fun removeFromCart(item: CartItem) {
        super.removeFromCart(item)
        updateTotalAmount()
        updateCartDisplay()

        // If cart becomes empty after removal
        if (cartItems.isEmpty()) {
            showToast("Your cart is now empty")
        }
    }

    override fun clearCart() {
        super.clearCart()
        updateTotalAmount()
        updateCartDisplay()
        showToast("Cart cleared successfully")
    }

    private fun proceedToCheckout() {
        if (cartItems.isNotEmpty()) {
            val intent = Intent(this, Payment::class.java).apply {
                putExtra("TOTAL_AMOUNT", cartTotalAmount)
                putExtra("CART_ITEMS_COUNT", cartItems.size)
                // You can also pass the entire cart items list if needed
            }
            startActivity(intent)
        } else {
            showToast("Please add items to cart before checkout")
        }
    }

    private fun continueShopping() {
        // Navigate back to services/main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // CartAdapter class implementation
    class CartAdapter(
        private var items: List<BaseActivity.CartItem>,
        private val onItemAction: (BaseActivity.CartItem, String) -> Unit
    ) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

        class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.itemName)
            val price: TextView = itemView.findViewById(R.id.itemPrice)
            val quantity: TextView = itemView.findViewById(R.id.itemQuantity)
            val description: TextView = itemView.findViewById(R.id.itemDescription)
            val removeButton: Button = itemView.findViewById(R.id.removeButton)
            val increaseButton: Button = itemView.findViewById(R.id.increaseButton)
            val decreaseButton: Button = itemView.findViewById(R.id.decreaseButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cart_sidebar, parent, false)
            return CartViewHolder(view)
        }

        override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
            val item = items[position]

            holder.name.text = item.name
            holder.price.text = "R${String.format("%.2f", item.price)}"
            holder.quantity.text = item.quantity.toString()
            holder.description.text = item.description

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
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<BaseActivity.CartItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}