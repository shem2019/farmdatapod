package com.example.farmdatapod.adapter

// MulchingAdapter.kt

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemMulchingBinding
import com.example.farmdatapod.models.LandPreparationMulching
import com.google.android.material.textfield.TextInputEditText

class MulchingAdapter : RecyclerView.Adapter<MulchingAdapter.MulchingViewHolder>() {
    private val items = mutableListOf<LandPreparationMulching>()
    private var onItemRemoved: ((Int) -> Unit)? = null

    companion object {
        val MULCH_TYPES = listOf(
            "Straw",
            "Grass clippings",
            "Wood chips/Bark",
            "Leaves",
            "Compost",
            "Cocoa hulls",
            "Sawdust",
            "Hay",
            "Pine needles",
            "Newspaper/Cardboard"
        )
    }

    inner class MulchingViewHolder(
        private val binding: ItemMulchingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            setupMulchTypeDropdown()
            setupRemoveButton()
            setupTextChangeListeners()
        }

        private fun setupMulchTypeDropdown() {
            val adapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_dropdown_item_1line,
                MULCH_TYPES
            )
            binding.typeOfMulchDropdown.setAdapter(adapter)
        }

        private fun setupRemoveButton() {
            binding.removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    onItemRemoved?.invoke(position)
                }
            }
        }

        private fun setupTextChangeListeners() {
            binding.typeOfMulchDropdown.addTextChangedListener(createTextWatcher { text ->
                updateItem { it.copy(typeOfMulch = text) }
            })

            binding.costOfMulchInput.addTextWatcher(createTextWatcher { text ->
                text.toDoubleOrNull()?.let { value ->
                    updateItem { it.copy(costOfMulch = value) }
                }
            })

            binding.lifeCycleInput.addTextWatcher(createTextWatcher { text ->
                text.toIntOrNull()?.let { value ->
                    updateItem { it.copy(lifeCycleOfMulchInSeasons = value) }
                }
            })

            binding.manDaysInput.addTextWatcher(createTextWatcher { text ->
                text.toIntOrNull()?.let { value ->
                    updateItem { it.copy(manDays = value) }
                }
            })

            binding.unitCostOfLaborInput.addTextWatcher(createTextWatcher { text ->
                text.toDoubleOrNull()?.let { value ->
                    updateItem { it.copy(unitCostOfLabor = value) }
                }
            })
        }

        private fun TextInputEditText.addTextWatcher(watcher: TextWatcher) {
            this.addTextChangedListener(watcher)
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

        private fun updateItem(update: (LandPreparationMulching) -> LandPreparationMulching) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                items[position] = update(items[position])
            }
        }

        fun bind(item: LandPreparationMulching) {
            with(binding) {
                typeOfMulchDropdown.setText(item.typeOfMulch, false)
                costOfMulchInput.setText(item.costOfMulch.toString())
                lifeCycleInput.setText(item.lifeCycleOfMulchInSeasons.toString())
                manDaysInput.setText(item.manDays.toString())
                unitCostOfLaborInput.setText(item.unitCostOfLabor.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MulchingViewHolder {
        val binding = ItemMulchingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MulchingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MulchingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: List<LandPreparationMulching>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: LandPreparationMulching) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    fun getItems(): List<LandPreparationMulching> = items.toList()

    fun setOnItemRemovedListener(listener: (Int) -> Unit) {
        onItemRemoved = listener
    }
}