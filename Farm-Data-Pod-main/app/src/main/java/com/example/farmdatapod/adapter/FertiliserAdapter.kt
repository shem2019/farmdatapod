package com.example.farmdatapod.adapter

import android.R
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemFertiliserUsageBinding
import com.example.farmdatapod.FertilizerUsed

class FertiliserAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<FertiliserAdapter.FertiliserViewHolder>() {

    private val fertiliserList = mutableListOf<FertilizerUsed>()

    inner class FertiliserViewHolder(private val binding: ItemFertiliserUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fertiliserUsage: FertilizerUsed) {
            binding.apply {
                // Bind data to views
                rgRegister.check(if (fertiliserUsage.register == "Yes") rbYes.id else rbNo.id)
                actvSelectProduct.setText(fertiliserUsage.product)
                etCostPerUnit.setText(fertiliserUsage.cost_per_unit)
                etCategory.setText(fertiliserUsage.category)
                etVolumeOfWater.setText(fertiliserUsage.volume_of_water)
                etFormulation.setText(fertiliserUsage.formulation)
                etFrequency.setText(fertiliserUsage.frequency_of_application)
                etDosage.setText(fertiliserUsage.dosage)
                etTotalCost.setText(fertiliserUsage.total_cost)
                etUnit.setText(fertiliserUsage.unit)

                // Add TextWatchers to update the form data in real-time
                actvSelectProduct.addTextChangedListener { fertiliserUsage.product = it.toString() }
                etCostPerUnit.addTextChangedListener { fertiliserUsage.cost_per_unit = it.toString() }
                etCategory.addTextChangedListener { fertiliserUsage.category = it.toString() }
                etVolumeOfWater.addTextChangedListener { fertiliserUsage.volume_of_water = it.toString() }
                etFormulation.addTextChangedListener { fertiliserUsage.formulation = it.toString() }
                etFrequency.addTextChangedListener { fertiliserUsage.frequency_of_application = it.toString() }
                etDosage.addTextChangedListener { fertiliserUsage.dosage = it.toString() }
                etTotalCost.addTextChangedListener { fertiliserUsage.total_cost = it.toString() }
                etUnit.addTextChangedListener { fertiliserUsage.unit = it.toString() }

                // Set OnCheckedChangeListener to capture changes in the register value
                rgRegister.setOnCheckedChangeListener { _, checkedId ->
                    fertiliserUsage.register = if (checkedId == rbYes.id) "Yes" else "No"
                }

                etCostPerUnit.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val cost = etCostPerUnit.text.toString().toDoubleOrNull()
                        if (cost == null || cost <= 0) {
                            etCostPerUnit.error = "Invalid cost"
                        } else {
                            fertiliserUsage.cost_per_unit = cost.toString()
                        }
                    }
                }

                // Set up product dropdown
                val products = listOf("product1", "product2", "product3")
                val adapter = ArrayAdapter(binding.root.context, R.layout.simple_dropdown_item_1line, products)
                actvSelectProduct.setAdapter(adapter)
            }
        }


        private fun ItemFertiliserUsageBinding.addTextWatchers(fertiliserUsage: FertilizerUsed) {
            actvSelectProduct.addTextChangedListener { fertiliserUsage.product = it.toString() }
            etCostPerUnit.addTextChangedListener { fertiliserUsage.cost_per_unit = it.toString() }
            etCategory.addTextChangedListener { fertiliserUsage.category = it.toString() }
            etVolumeOfWater.addTextChangedListener { fertiliserUsage.volume_of_water = it.toString() }
            etFormulation.addTextChangedListener { fertiliserUsage.formulation = it.toString() }
            etFrequency.addTextChangedListener { fertiliserUsage.frequency_of_application = it.toString() }
            etDosage.addTextChangedListener { fertiliserUsage.dosage = it.toString() }
            etTotalCost.addTextChangedListener { fertiliserUsage.total_cost = it.toString() }
            etUnit.addTextChangedListener { fertiliserUsage.unit = it.toString() }

            // Validation for cost per unit
            etCostPerUnit.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    validateCost(fertiliserUsage)
                }
            }
        }

        private fun ItemFertiliserUsageBinding.validateCost(fertiliserUsage: FertilizerUsed) {
            val cost = etCostPerUnit.text.toString().toDoubleOrNull()
            if (cost == null || cost <= 0) {
                etCostPerUnit.error = "Invalid cost"
            } else {
                fertiliserUsage.cost_per_unit = cost.toString()
            }
        }

        private fun ItemFertiliserUsageBinding.setupProductDropdown() {
            val products = listOf("product1", "product2", "product3")
            val adapter = ArrayAdapter(root.context, R.layout.simple_dropdown_item_1line, products)
            actvSelectProduct.setAdapter(adapter)
        }

        fun validate(): Boolean {
            var isValid = true
            binding.apply {
                isValid = validateField(actvSelectProduct, "Product is required") && isValid
                isValid = validateField(etCategory, "Category is required") && isValid
                isValid = validateNumberField(etCostPerUnit, "Invalid cost") && isValid
                isValid = validateNumberField(etVolumeOfWater, "Invalid volume") && isValid
                isValid = validateField(etFormulation, "Formulation is required") && isValid
                isValid = validateField(etFrequency, "Frequency is required") && isValid
                isValid = validateNumberField(etDosage, "Invalid dosage") && isValid
                isValid = validateNumberField(etTotalCost, "Invalid total cost") && isValid
                isValid = validateField(etUnit, "Unit is required") && isValid
            }
            return isValid
        }

        private fun validateField(field: AutoCompleteTextView, errorMessage: String): Boolean {
            return if (field.text.isNullOrBlank()) {
                field.error = errorMessage
                false
            } else {
                true
            }
        }

        private fun validateField(field: EditText, errorMessage: String): Boolean {
            return if (field.text.isNullOrBlank()) {
                field.error = errorMessage
                false
            } else {
                true
            }
        }

        private fun validateNumberField(field: EditText, errorMessage: String): Boolean {
            val value = field.text.toString().toDoubleOrNull()
            return if (value == null || value <= 0) {
                field.error = errorMessage
                false
            } else {
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FertiliserViewHolder {
        val binding = ItemFertiliserUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FertiliserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FertiliserViewHolder, position: Int) {
        holder.bind(fertiliserList[position])
    }

    override fun getItemCount(): Int = fertiliserList.size

    fun addFertiliserUsage() {
        fertiliserList.add(FertilizerUsed()) // Create a new instance with default values
        notifyItemInserted(fertiliserList.size - 1)
    }

    fun validateForms(): Boolean {
        return fertiliserList.mapIndexed { index, _ ->
            (recyclerView.findViewHolderForAdapterPosition(index) as? FertiliserViewHolder)?.validate() ?: false
        }.all { it }
    }

    fun getForms(): List<FertilizerUsed> = fertiliserList.toList()
}
