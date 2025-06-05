package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.example.farmdatapod.R
import com.example.farmdatapod.models.Product

class ProductAdapter : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val products = mutableListOf<Product>()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryEditText: TextInputEditText = itemView.findViewById(R.id.categoryEditText)
        val packagingEditText: TextInputEditText = itemView.findViewById(R.id.packagingEditText)
        val productsInterestedEditText: TextInputEditText = itemView.findViewById(R.id.productsInterestedEditText)
        val qualityEditText: TextInputEditText = itemView.findViewById(R.id.qualityEditText)
        val volumeEditText: TextInputEditText = itemView.findViewById(R.id.volumeEditText)
        val frequencyEditText: TextInputEditText = itemView.findViewById(R.id.frequencyEditText)

        init {
            categoryEditText.addTextChangedListener(createTextWatcher { text ->
                products[adapterPosition].category = text
            })
            packagingEditText.addTextChangedListener(createTextWatcher { text ->
                products[adapterPosition].packaging = text
            })
            productsInterestedEditText.addTextChangedListener(createTextWatcher { text ->
                products[adapterPosition].products_interested_in = text
            })
            qualityEditText.addTextChangedListener(createTextWatcher { text ->
                products[adapterPosition].quality = text
            })
            volumeEditText.addTextChangedListener(createTextWatcher { text ->
                products[adapterPosition].volume_in_kgs = text
            })
            frequencyEditText.addTextChangedListener(createTextWatcher { text ->
                products[adapterPosition].frequency = text
            })
        }

        private fun createTextWatcher(onTextChanged: (String) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextChanged(s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.categoryEditText.setText(product.category)
        holder.packagingEditText.setText(product.packaging)
        holder.productsInterestedEditText.setText(product.products_interested_in)
        holder.qualityEditText.setText(product.quality)
        holder.volumeEditText.setText(product.volume_in_kgs)
        holder.frequencyEditText.setText(product.frequency)
    }

    override fun getItemCount(): Int = products.size

    fun addProduct(product: Product) {
        products.add(product)
        notifyItemInserted(products.size - 1)
    }

    fun getProducts(): List<Product> = products

    fun clearProducts() {
        products.clear()
        notifyDataSetChanged()
    }
}