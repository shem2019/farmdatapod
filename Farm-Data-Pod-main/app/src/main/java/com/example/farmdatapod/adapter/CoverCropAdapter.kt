package com.example.farmdatapod.adapter

import android.R
import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemCoverCropBinding
import com.example.farmdatapod.models.CoverCrop
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// CoverCropAdapter.kt

class CoverCropAdapter : RecyclerView.Adapter<CoverCropAdapter.CoverCropViewHolder>() {
    private val items = mutableListOf<CoverCrop>()
    private var onItemRemoved: ((Int) -> Unit)? = null

    companion object {
        val COVER_CROP_TYPES = listOf(
            "Legumes - Clover",
            "Legumes - Alfalfa",
            "Legumes - Vetch",
            "Grasses - Ryegrass",
            "Grasses - Barley",
            "Grasses - Oats",
            "Brassicas - Radish",
            "Brassicas - Mustard",
            "Brassicas - Turnips",
            "Cereals - Wheat",
            "Cereals - Rye",
            "Cereals - Millet",
            "Broadleaf - Buckwheat",
            "Broadleaf - Sunflower",
            "Forage - Sorghum",
            "Forage - Sudangrass",
            "Perennial - Perennial Ryegrass",
            "Perennial - White Clover",
            "Green Manures - Cowpea",
            "Green Manures - Lupin"
        )

        val INOCULANT_TYPES = listOf(
            "Rhizobium",
            "Azotobacter",
            "Azospirillum",
            "Phosphate-Solubilizing Bacteria (PSB)",
            "Mycorrhizae",
            "Cyanobacteria (Blue-Green Algae)",
            "Nitrogen-Fixing Bacteria",
            "Biofertilizer Consortia",
            "Actinobacteria",
            "Trichoderma"
        )
    }

    inner class CoverCropViewHolder(
        private val binding: ItemCoverCropBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            setupCoverCropDropdown()
            setupInoculantDropdown()
            setupRemoveButton()
            setupDatePickers()
            disableManualDateInput()
        }

        private fun setupCoverCropDropdown() {
            val adapter = ArrayAdapter(
                binding.root.context,
                R.layout.simple_dropdown_item_1line,
                COVER_CROP_TYPES
            )
            binding.coverCropDropdown.setAdapter(adapter)
        }

        private fun setupInoculantDropdown() {
            val adapter = ArrayAdapter(
                binding.root.context,
                R.layout.simple_dropdown_item_1line,
                INOCULANT_TYPES
            )
            (binding.inoculantInput as? AutoCompleteTextView)?.setAdapter(adapter)
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

        private fun setupDatePickers() {
            // Setup for date of establishment
            binding.dateEstablishmentInput.setOnClickListener {
                showDatePicker(binding.dateEstablishmentInput, "establishment")
            }

            // Setup for date of incorporation
            binding.dateIncorporationInput.setOnClickListener {
                showDatePicker(binding.dateIncorporationInput, "incorporation")
            }
        }

        private fun disableManualDateInput() {
            binding.dateEstablishmentInput.apply {
                isFocusable = false
                isFocusableInTouchMode = false
                isClickable = true
            }

            binding.dateIncorporationInput.apply {
                isFocusable = false
                isFocusableInTouchMode = false
                isClickable = true
            }
        }

        private fun showDatePicker(dateInput: TextInputEditText, dateType: String) {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                binding.root.context,
                { _, year, month, day ->
                    // Format for API (ISO format)
                    val apiDate = String.format(
                        "%04d-%02d-%02dT00:00:00",
                        year,
                        month + 1,
                        day
                    )

                    // Format for display
                    val displayDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                        .format(calendar.apply {
                            set(year, month, day)
                        }.time)

                    // Update the UI with display format
                    dateInput.setText(displayDate)

                    // Update the data model with API format
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val item = items[adapterPosition]
                        items[adapterPosition] = when(dateType) {
                            "establishment" -> item.copy(dateOfEstablishment = apiDate)
                            "incorporation" -> item.copy(dateOfIncorporation = apiDate)
                            else -> item
                        }
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        fun bind(item: CoverCrop) {
            with(binding) {
                coverCropDropdown.setText(item.coverCrop, false)

                // Format dates for display if they exist
                if (item.dateOfEstablishment.isNotEmpty()) {
                    val establishmentDate = try {
                        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                        formatter.format(parser.parse(item.dateOfEstablishment)!!)
                    } catch (e: Exception) {
                        item.dateOfEstablishment
                    }
                    dateEstablishmentInput.setText(establishmentDate)
                }

                if (item.dateOfIncorporation.isNotEmpty()) {
                    val incorporationDate = try {
                        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                        formatter.format(parser.parse(item.dateOfIncorporation)!!)
                    } catch (e: Exception) {
                        item.dateOfIncorporation
                    }
                    dateIncorporationInput.setText(incorporationDate)
                }

                unitInput.setText(item.unit)
                unitCostInput.setText(item.unitCost.toString())
                (inoculantInput as? AutoCompleteTextView)?.setText(item.typeOfInoculant, false)
                manDaysInput.setText(item.manDays.toString())
                laborCostInput.setText(item.unitCostOfLabor.toString())

                // Add text change listeners to update the data
                coverCropDropdown.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        items[adapterPosition] = items[adapterPosition].copy(coverCrop = text.toString())
                    }
                }

                unitInput.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        items[adapterPosition] = items[adapterPosition].copy(unit = text.toString())
                    }
                }

                unitCostInput.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        text.toString().toDoubleOrNull()?.let { value ->
                            items[adapterPosition] = items[adapterPosition].copy(unitCost = value)
                        }
                    }
                }

                // Updated inoculant listener
                (inoculantInput as? AutoCompleteTextView)?.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        items[adapterPosition] = items[adapterPosition].copy(typeOfInoculant = text.toString())
                    }
                }

                manDaysInput.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        text.toString().toIntOrNull()?.let { value ->
                            items[adapterPosition] = items[adapterPosition].copy(manDays = value)
                        }
                    }
                }

                laborCostInput.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        text.toString().toDoubleOrNull()?.let { value ->
                            items[adapterPosition] = items[adapterPosition].copy(unitCostOfLabor = value)
                        }
                    }
                }
            }
        }
    }

    // Rest of the adapter implementation remains the same
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoverCropViewHolder {
        val binding = ItemCoverCropBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CoverCropViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CoverCropViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: List<CoverCrop>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: CoverCrop) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    fun getItems(): List<CoverCrop> = items.toList()

    fun setOnItemRemovedListener(listener: (Int) -> Unit) {
        onItemRemoved = listener
    }
}