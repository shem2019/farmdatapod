package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemFertiliserUsageBinding
import com.example.farmdatapod.PesticideUsed

class PesticideApplicationAdapter(private val recyclerView: RecyclerView) :
    RecyclerView.Adapter<PesticideApplicationAdapter.PesticideViewHolder>() {

    private val pesticideList = mutableListOf<PesticideUsed>()
    private val productOptions = listOf("Product 1", "Product 2", "Product 3")

    inner class PesticideViewHolder(private val binding: ItemFertiliserUsageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pesticideUsage: PesticideUsed) {
            binding.apply {
                // Set the RadioGroup based on the 'register' value
                rgRegister.check(
                    if (pesticideUsage.register == "Yes") rbYes.id else rbNo.id
                )

                // Set up product options
                val adapter = ArrayAdapter(
                    binding.root.context,
                    android.R.layout.simple_dropdown_item_1line,
                    productOptions
                )
                actvSelectProduct.setAdapter(adapter)

                // Bind the remaining fields
                actvSelectProduct.setText(pesticideUsage.product)
                etCostPerUnit.setText(pesticideUsage.cost_per_unit)
                etCategory.setText(pesticideUsage.category)
                etVolumeOfWater.setText(pesticideUsage.volume_of_water)
                etFormulation.setText(pesticideUsage.formulation)
                etFrequency.setText(pesticideUsage.frequency_of_application)
                etDosage.setText(pesticideUsage.dosage)
                etTotalCost.setText(pesticideUsage.total_cost)
                etUnit.setText(pesticideUsage.unit)

                // Add TextWatchers to update the form data in real-time
                actvSelectProduct.addTextChangedListener { pesticideUsage.product = it.toString() }
                etCostPerUnit.addTextChangedListener { pesticideUsage.cost_per_unit = it.toString() }
                etCategory.addTextChangedListener { pesticideUsage.category = it.toString() }
                etVolumeOfWater.addTextChangedListener { pesticideUsage.volume_of_water = it.toString() }
                etFormulation.addTextChangedListener { pesticideUsage.formulation = it.toString() }
                etFrequency.addTextChangedListener { pesticideUsage.frequency_of_application = it.toString() }
                etDosage.addTextChangedListener { pesticideUsage.dosage = it.toString() }
                etTotalCost.addTextChangedListener { pesticideUsage.total_cost = it.toString() }
                etUnit.addTextChangedListener { pesticideUsage.unit = it.toString() }

                // Add RadioGroup listener to update the 'register' field
                rgRegister.setOnCheckedChangeListener { _, checkedId ->
                    pesticideUsage.register = when (checkedId) {
                        rbYes.id -> "Yes"
                        rbNo.id -> "No"
                        else -> null
                    }
                }

                // Add validation logic
                etCostPerUnit.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val cost = etCostPerUnit.text.toString().toDoubleOrNull()
                        if (cost == null || cost <= 0) {
                            etCostPerUnit.error = "Invalid cost"
                        } else {
                            pesticideUsage.cost_per_unit = cost.toString()
                        }
                    }
                }

                etVolumeOfWater.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val volume = etVolumeOfWater.text.toString().toDoubleOrNull()
                        if (volume == null || volume <= 0) {
                            etVolumeOfWater.error = "Invalid volume"
                        } else {
                            pesticideUsage.volume_of_water = volume.toString()
                        }
                    }
                }

                etDosage.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val dosage = etDosage.text.toString().toDoubleOrNull()
                        if (dosage == null || dosage <= 0) {
                            etDosage.error = "Invalid dosage"
                        } else {
                            pesticideUsage.dosage = dosage.toString()
                        }
                    }
                }

                etTotalCost.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val totalCost = etTotalCost.text.toString().toDoubleOrNull()
                        if (totalCost == null || totalCost <= 0) {
                            etTotalCost.error = "Invalid total cost"
                        } else {
                            pesticideUsage.total_cost = totalCost.toString()
                        }
                    }
                }
            }
        }

        fun isValid(): Boolean {
            binding.apply {
                val costPerUnit = etCostPerUnit.text?.toString()?.toDoubleOrNull()
                val volumeOfWater = etVolumeOfWater.text?.toString()?.toDoubleOrNull()
                val dosage = etDosage.text?.toString()?.toDoubleOrNull()
                val totalCost = etTotalCost.text?.toString()?.toDoubleOrNull()

                return costPerUnit != null && costPerUnit > 0 &&
                        volumeOfWater != null && volumeOfWater > 0 &&
                        dosage != null && dosage > 0 &&
                        totalCost != null && totalCost > 0 &&
                        actvSelectProduct.text?.isNotBlank() == true &&
                        etCategory.text?.isNotBlank() == true &&
                        etFormulation.text?.isNotBlank() == true &&
                        etFrequency.text?.isNotBlank() == true &&
                        etUnit.text?.isNotBlank() == true &&
                        (rgRegister.checkedRadioButtonId == rbYes.id || rgRegister.checkedRadioButtonId == rbNo.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PesticideViewHolder {
        val binding = ItemFertiliserUsageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PesticideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PesticideViewHolder, position: Int) {
        holder.bind(pesticideList[position])
    }

    override fun getItemCount(): Int = pesticideList.size

    fun addPesticideUsage() {
        pesticideList.add(PesticideUsed().apply {
            product = ""
            cost_per_unit = ""
            category = ""
            volume_of_water = ""
            formulation = ""
            frequency_of_application = ""
            dosage = ""
            total_cost = ""
            unit = ""
        })
        notifyItemInserted(pesticideList.size - 1)
    }

    fun validateForms(): Boolean {
        return pesticideList.isNotEmpty() &&
                (0 until itemCount).all { position ->
                    (recyclerView.findViewHolderForAdapterPosition(position) as? PesticideViewHolder)?.isValid() == true
                }
    }

    fun getForms(): List<PesticideUsed> {
        return pesticideList.toList()
    }
}