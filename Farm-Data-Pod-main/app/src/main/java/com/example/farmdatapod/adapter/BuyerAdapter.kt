package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemBuyerBinding
import com.example.farmdatapod.models.Buyer

class BuyerAdapter : RecyclerView.Adapter<BuyerAdapter.BuyerViewHolder>() {

    private val buyers = mutableListOf<Buyer>()
    private val textWatchers = mutableMapOf<Int, List<TextWatcher>>()

    inner class BuyerViewHolder(
        private val binding: ItemBuyerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val nameWatcher = createTextWatcher { newText ->
            updateBuyer(adapterPosition) { it.copy(name = newText) }
        }

        private val contactWatcher = createTextWatcher { newText ->
            updateBuyer(adapterPosition) { it.copy(contact_info = newText) }
        }

        private val quantityWatcher = createTextWatcher { newText ->
            updateBuyer(adapterPosition) { it.copy(quantity = newText.toIntOrNull() ?: 0) }
        }

        fun bind(buyer: Buyer) {
            // Remove previous text watchers if they exist
            removeTextWatchers()

            // Set values
            binding.nameInput.setText(buyer.name)
            binding.contactInfoInput.setText(buyer.contact_info)
            binding.quantityInput.setText(buyer.quantity.toString())

            // Add text watchers
            binding.nameInput.addTextChangedListener(nameWatcher)
            binding.contactInfoInput.addTextChangedListener(contactWatcher)
            binding.quantityInput.addTextChangedListener(quantityWatcher)

            // Store text watchers for this position
            textWatchers[adapterPosition] = listOf(nameWatcher, contactWatcher, quantityWatcher)

            // Set up remove button
            binding.removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    removeBuyer(position)
                }
            }
        }

        private fun removeTextWatchers() {
            textWatchers[adapterPosition]?.forEach { watcher ->
                binding.nameInput.removeTextChangedListener(watcher)
                binding.contactInfoInput.removeTextChangedListener(watcher)
                binding.quantityInput.removeTextChangedListener(watcher)
            }
            textWatchers.remove(adapterPosition)
        }

        private fun createTextWatcher(onTextChanged: (String) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onTextChanged(s?.toString() ?: "")
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyerViewHolder {
        val binding = ItemBuyerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BuyerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuyerViewHolder, position: Int) {
        holder.bind(buyers[position])
    }

    override fun getItemCount(): Int = buyers.size

    override fun onViewRecycled(holder: BuyerViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            textWatchers.remove(position)
        }
    }

    // Public methods for managing buyers
    fun addItem(buyer: Buyer) {
        buyers.add(buyer)
        notifyItemInserted(buyers.lastIndex)
    }

    fun removeBuyer(position: Int) {
        if (position in buyers.indices) {
            buyers.removeAt(position)
            textWatchers.remove(position)
            notifyItemRemoved(position)
            // Update positions for remaining items
            for (i in position until buyers.size) {
                textWatchers[i]?.let { watchers ->
                    textWatchers.remove(i)
                    textWatchers[i + 1] = watchers
                }
            }
            notifyItemRangeChanged(position, buyers.size)
        }
    }

    private fun updateBuyer(position: Int, update: (Buyer) -> Buyer) {
        if (position in buyers.indices) {
            buyers[position] = update(buyers[position])
        }
    }

    fun getItems(): List<Buyer> = buyers.toList()

    fun clearItems() {
        val size = buyers.size
        buyers.clear()
        textWatchers.clear()
        notifyItemRangeRemoved(0, size)
    }
}