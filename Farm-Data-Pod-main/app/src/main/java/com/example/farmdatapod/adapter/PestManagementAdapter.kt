package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.PreventativePest
import com.example.farmdatapod.databinding.ItemDiseaseManagementFormBinding
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class PestManagementAdapter : RecyclerView.Adapter<PestManagementAdapter.FormViewHolder>() {

    private val forms = mutableListOf<PreventativePest>()
    private val productOptions = listOf("Product 1", "Product 2", "Product 3")

    // Add a new form to the list
    fun addForm() {
        forms.add(PreventativePest().apply {
            pest = ""
            product = ""
            category = ""
            formulation = ""
            dosage = ""
            unit = ""
            cost_per_unit = ""
            volume_of_water = ""
            frequency_of_application = ""
            total_cost = ""
        })
        notifyItemInserted(forms.size - 1)
    }

    // Validate all forms, ensuring no required field is empty
    fun validateForms(): Boolean {
        return forms.all { form ->
            !form.pest.isNullOrBlank() &&
                    !form.product.isNullOrBlank() &&
                    !form.category.isNullOrBlank() &&
                    !form.formulation.isNullOrBlank() &&
                    !form.dosage.isNullOrBlank() &&
                    !form.unit.isNullOrBlank() &&
                    !form.cost_per_unit.isNullOrBlank() &&
                    !form.volume_of_water.isNullOrBlank() &&
                    !form.frequency_of_application.isNullOrBlank() &&
                    !form.total_cost.isNullOrBlank()
        }
    }

    // Get all forms from the list
    fun getForms(): List<PreventativePest> = forms

    // Create a new ViewHolder for the RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
        val binding = ItemDiseaseManagementFormBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FormViewHolder(binding)
    }

    // Bind a form to the ViewHolder
    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
        holder.bind(forms[position])
    }

    // Return the total number of forms
    override fun getItemCount(): Int = forms.size

    inner class FormViewHolder(private val binding: ItemDiseaseManagementFormBinding) : RecyclerView.ViewHolder(binding.root) {

        // Bind the form data to the UI
        fun bind(form: PreventativePest) {
            setupProductSelection(form)
            setupTextInputs(form)
            setupPestCheckboxes(form)
        }

        // Setup the product selection dropdown
        private fun setupProductSelection(form: PreventativePest) {
            val adapter = ArrayAdapter(binding.root.context, android.R.layout.simple_dropdown_item_1line, productOptions)
            binding.selectProductAutoComplete.setAdapter(adapter)
            binding.selectProductAutoComplete.setText(form.product, false)
            binding.selectProductAutoComplete.setOnItemClickListener { _, _, position, _ ->
                form.product = productOptions[position]
                validateField(binding.selectProductInputLayout, form.product ?: "")
            }
        }

        // Setup all other text input fields with validation
        private fun setupTextInputs(form: PreventativePest) {
            setupTextInput(binding.categoryEditText, binding.categoryInputLayout, form.category ?: "") { form.category = it }
            setupTextInput(binding.formulationEditText, binding.formulationInputLayout, form.formulation ?: "") { form.formulation = it }
            setupTextInput(binding.dosageRateEditText, binding.dosageRateInputLayout, form.dosage ?: "") { form.dosage = it }
            setupTextInput(binding.unitEditText, binding.unitInputLayout, form.unit ?: "") { form.unit = it }
            setupTextInput(binding.costPerUnitEditText, binding.costPerUnitInputLayout, form.cost_per_unit ?: "") { form.cost_per_unit = it }
            setupTextInput(binding.volumeOfWaterEditText, binding.volumeOfWaterInputLayout, form.volume_of_water ?: "") { form.volume_of_water = it }
            setupTextInput(binding.frequencyOfApplicationEditText, binding.frequencyOfApplicationInputLayout, form.frequency_of_application ?: "") { form.frequency_of_application = it }
            setupTextInput(binding.totalCostEditText, binding.totalCostInputLayout, form.total_cost ?: "") { form.total_cost = it }
        }

        // Setup the pest checkboxes
        private fun setupPestCheckboxes(form: PreventativePest) {
            val checkBoxes = listOf(
                binding.checkboxCharcoalCooler to "Charcoal Cooler",
                binding.checkboxHandWashingFacility to "Hand Washing Facility",
                binding.checkboxWashrooms to "Washrooms",
                binding.checkboxOthers to "Others"
            )

            checkBoxes.forEach { (checkbox, pestValue) ->
                checkbox.isChecked = form.pest?.contains(pestValue) == true
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    updatePestList(pestValue, isChecked, form)
                }
            }
        }

        // Update pest list in the form
        private fun updatePestList(pestValue: String, isChecked: Boolean, form: PreventativePest) {
            val pests = form.pest?.split(",")?.toMutableSet() ?: mutableSetOf()
            if (isChecked) {
                pests.add(pestValue)
            } else {
                pests.remove(pestValue)
            }
            form.pest = pests.joinToString(",")
        }

        // Helper method to setup individual text inputs with validation
        private fun setupTextInput(
            editText: TextInputEditText,
            inputLayout: TextInputLayout,
            initialValue: String,
            onTextChanged: (String) -> Unit
        ) {
            editText.setText(initialValue)
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // No action needed here
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val text = s?.toString() ?: ""
                    onTextChanged(text)
                    validateField(inputLayout, text)
                }

                override fun afterTextChanged(s: Editable?) {
                    // No action needed here
                }
            })
        }

        // Validate an individual field and show an error if it's empty
        private fun validateField(inputLayout: TextInputLayout, text: String) {
            if (text.isBlank()) {
                inputLayout.error = "This field is required"
            } else {
                inputLayout.error = null
            }
        }
    }
}