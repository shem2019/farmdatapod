package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemFieldCropBinding
import java.util.UUID

class FieldCropAdapter(
    private val onEditClick: (FieldCrop, Int) -> Unit,
    private val onDeleteClick: (FieldCrop, Int) -> Unit,
    private val onPlantingDateClick: (Int) -> Unit,
    private val onHarvestDateClick: (Int) -> Unit
) : RecyclerView.Adapter<FieldCropAdapter.FieldCropViewHolder>() {

    private val crops = mutableListOf<FieldCrop>()

    inner class FieldCropViewHolder(private val binding: ItemFieldCropBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatchers = mutableListOf<TextWatcher>()

        fun bind(crop: FieldCrop, position: Int) {
            // Clear previous text watchers
            textWatchers.forEach { watcher ->
                binding.mainCropEditText.removeTextChangedListener(watcher)
                binding.mainCropVarietyEditText.removeTextChangedListener(watcher)
                binding.mainCropPlantPopulationEditText.removeTextChangedListener(watcher)
                binding.mainCropBaselineYieldEditText.removeTextChangedListener(watcher)
                binding.mainCropBaselineIncomeEditText.removeTextChangedListener(watcher)
                binding.mainCropBaselineCostEditText.removeTextChangedListener(watcher)
            }
            textWatchers.clear()

            binding.apply {
                // Set all field values
                mainCropEditText.setText(crop.cropName)
                mainCropVarietyEditText.setText(crop.variety)
                mainCropPlantPopulationEditText.setText(crop.plantPopulation)
                mainCropDatePlantedEditText.setText(
                    if (crop.displayPlantingDate.isNotEmpty()) crop.displayPlantingDate
                    else crop.plantingDate
                )
                mainCropDateHarvestEditText.setText(
                    if (crop.displayHarvestDate.isNotEmpty()) crop.displayHarvestDate
                    else crop.harvestDate
                )
                mainCropBaselineYieldEditText.setText(crop.baselineYield.toString())
                mainCropBaselineIncomeEditText.setText(crop.baselineIncome)
                mainCropBaselineCostEditText.setText(crop.baselineCost)

                // Configure date picker fields
                mainCropDatePlantedEditText.apply {
                    isFocusable = false
                    isClickable = true
                    isCursorVisible = false
                    keyListener = null
                    setOnClickListener {
                        onPlantingDateClick(position)
                    }
                }

                mainCropDateHarvestEditText.apply {
                    isFocusable = false
                    isClickable = true
                    isCursorVisible = false
                    keyListener = null
                    setOnClickListener {
                        onHarvestDateClick(position)
                    }
                }

                // Add text watchers for all editable fields
                addTextWatcher(mainCropEditText) { text ->
                    updateCrop(position) { it.copy(cropName = text) }
                }

                addTextWatcher(mainCropVarietyEditText) { text ->
                    updateCrop(position) { it.copy(variety = text) }
                }

                addTextWatcher(mainCropPlantPopulationEditText) { text ->
                    updateCrop(position) { it.copy(plantPopulation = text) }
                }

                addTextWatcher(mainCropBaselineYieldEditText) { text ->
                    updateCrop(position) { it.copy(baselineYield = text.toDoubleOrNull() ?: 0.0) }
                }

                addTextWatcher(mainCropBaselineIncomeEditText) { text ->
                    updateCrop(position) { it.copy(baselineIncome = text) }
                }

                addTextWatcher(mainCropBaselineCostEditText) { text ->
                    updateCrop(position) { it.copy(baselineCost = text) }
                }

                // Handle click events
                root.setOnClickListener {
                    onEditClick(crop, position)
                }
            }
        }

        private fun addTextWatcher(
            view: android.widget.EditText,
            onTextChanged: (String) -> Unit
        ) {
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    onTextChanged(s?.toString() ?: "")
                }
            }
            view.addTextChangedListener(watcher)
            textWatchers.add(watcher)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldCropViewHolder {
        val binding = ItemFieldCropBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FieldCropViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FieldCropViewHolder, position: Int) {
        holder.bind(crops[position], position)
    }

    override fun getItemCount(): Int = crops.size

    // Public methods to manipulate the crops list
    fun addCrop(crop: FieldCrop) {
        crops.add(crop)
        notifyItemInserted(crops.size - 1)
    }

    fun updateCrop(position: Int, crop: FieldCrop) {
        if (position in crops.indices) {
            crops[position] = crop
            notifyItemChanged(position)
        }
    }

    private fun updateCrop(position: Int, update: (FieldCrop) -> FieldCrop) {
        if (position in crops.indices) {
            crops[position] = update(crops[position])
        }
    }

    fun deleteCrop(position: Int) {
        if (position in crops.indices) {
            crops.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, crops.size)
        }
    }

    fun getCrops(): List<FieldCrop> = crops.toList()

    fun clearCrops() {
        crops.clear()
        notifyDataSetChanged()
    }
}

data class FieldCrop(
    val id: String = UUID.randomUUID().toString(),
    var cropName: String = "",
    var variety: String = "",
    var plantingDate: String = "",
    var harvestDate: String = "",
    var displayPlantingDate: String = "",
    var displayHarvestDate: String = "",
    var plantPopulation: String = "",
    var baselineYield: Double = 0.0,
    var baselineIncome: String = "",
    var baselineCost: String = ""
)