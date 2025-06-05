package com.example.farmdatapod.adapter

// SoilAnalysisAdapter.kt

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemSoilAnalysisBinding
import com.example.farmdatapod.models.LandPreparationSoilAnalysis
import com.google.android.material.textfield.TextInputEditText

class SoilAnalysisAdapter : RecyclerView.Adapter<SoilAnalysisAdapter.SoilAnalysisViewHolder>() {
    private val items = mutableListOf<LandPreparationSoilAnalysis>()
    private var onItemRemoved: ((Int) -> Unit)? = null

    companion object {
        val ANALYSIS_TYPES = listOf(
            "Basic Soil Fertility Test",
            "Micro-Nutrient Analysis",
            "Soil Texture Analysis",
            "Salinity and Sodicity Test",
            "Soil Organic Carbon (SOC) Test",
            "Soil Moisture Content Test",
            "Cation Exchange Capacity (CEC) Test",
            "Heavy Metal Contamination Test",
            "Biological Soil Analysis",
            "Soil Compaction Test"
        )
    }

    inner class SoilAnalysisViewHolder(
        private val binding: ItemSoilAnalysisBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            setupAnalysisTypeDropdown()
            setupRemoveButton()
            setupTextChangeListeners()
        }

        private fun setupAnalysisTypeDropdown() {
            // Convert the regular TextInputLayout to ExposedDropdownMenu
            val adapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_dropdown_item_1line,
                ANALYSIS_TYPES
            )

            // Create and set up the AutoCompleteTextView
            binding.typeOfAnalysisInput.setAdapter(adapter)
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
            binding.typeOfAnalysisInput.addTextChangedListener(createTextWatcher { text ->
                updateItem { it.copy(typeOfAnalysis = text) }
            })

            binding.costOfAnalysisInput.addTextChangedListener(createTextWatcher { text ->
                text.toDoubleOrNull()?.let { value ->
                    updateItem { it.copy(costOfAnalysis = value) }
                }
            })

            binding.labInput.addTextChangedListener(createTextWatcher { text ->
                updateItem { it.copy(lab = text) }
            })
        }

        private fun TextInputEditText.addTextChangedListener(watcher: TextWatcher) {
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

        private fun updateItem(update: (LandPreparationSoilAnalysis) -> LandPreparationSoilAnalysis) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                items[position] = update(items[position])
            }
        }

        fun bind(item: LandPreparationSoilAnalysis) {
            with(binding) {
                typeOfAnalysisInput.setText(item.typeOfAnalysis, false)
                costOfAnalysisInput.setText(item.costOfAnalysis.toString())
                labInput.setText(item.lab)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoilAnalysisViewHolder {
        val binding = ItemSoilAnalysisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SoilAnalysisViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SoilAnalysisViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: List<LandPreparationSoilAnalysis>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: LandPreparationSoilAnalysis) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    fun getItems(): List<LandPreparationSoilAnalysis> = items.toList()

    fun setOnItemRemovedListener(listener: (Int) -> Unit) {
        onItemRemoved = listener
    }
}