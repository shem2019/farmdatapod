// MarketProduceAdapter.kt
package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemCropBinding
import com.example.farmdatapod.MarketProduce

class MarketProduceAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<MarketProduceAdapter.MarketProduceViewHolder>() {

    private val marketProduces = mutableListOf<MarketProduce>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketProduceViewHolder {
        val binding = ItemCropBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MarketProduceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarketProduceViewHolder, position: Int) {
        holder.bind(marketProduces[position])
    }

    override fun getItemCount(): Int = marketProduces.size

    fun addMarketProduce() {
        // Initialize a new MarketProduce object with nullable fields
        marketProduces.add(MarketProduce().apply {
            product = ""
            product_category = ""
            acerage = ""
        })
        notifyItemInserted(marketProduces.size - 1)
    }

    fun getMarketProduces(): List<MarketProduce> = marketProduces.toList()

    inner class MarketProduceViewHolder(private val binding: ItemCropBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(marketProduce: MarketProduce) {
            // Bind existing data from marketProduce object to the views
            binding.cropEditText.setText(marketProduce.product)
            binding.productCategoryAutoComplete.setText(marketProduce.product_category)
            binding.acreageEditText.setText(marketProduce.acerage)

            // Setup default categories for the dropdown
            val defaultCategories = listOf("Category 1", "Category 2", "Category 3")
            val categoryAdapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_dropdown_item_1line,
                defaultCategories
            )
            binding.productCategoryAutoComplete.setAdapter(categoryAdapter)

            // Setup text watchers to update the MarketProduce object when text changes
            binding.cropEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    marketProduce.product = s?.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            binding.productCategoryAutoComplete.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    marketProduce.product_category = s?.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            binding.acreageEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    marketProduce.acerage = s?.toString()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }
}