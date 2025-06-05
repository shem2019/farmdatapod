package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemDiseaseManagementFormBinding
import com.example.farmdatapod.PreventativeDisease
import com.example.farmdatapod.R
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputLayout

class DiseaseManagementAdapter : RecyclerView.Adapter<DiseaseManagementAdapter.FormViewHolder>() {

    private val forms = mutableListOf<PreventativeDisease>()
    private val products = listOf("Product 1", "Product 2", "Product 3") // Update with actual products as needed

    // Add a new form to the list
    fun addForm() {
        forms.add(PreventativeDisease().apply {
            disease = ""
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
            !form.disease.isNullOrBlank() &&
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
    fun getForms(): List<PreventativeDisease> {
        return forms
    }

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
        fun bind(form: PreventativeDisease) {
            setupCheckboxes(form)
            setupProductSelection(form)
            setupTextInputs(form)
        }

        // Setup checkboxes and concatenate their states into a string
        private fun setupCheckboxes(form: PreventativeDisease) {
            val checkboxIds = listOf(
                R.id.checkboxCharcoalCooler to "Charcoal Cooler",
                R.id.checkboxHandWashingFacility to "Hand Washing Facility",
                R.id.checkboxWashrooms to "Washrooms",
                R.id.checkboxOthers to "Others"
            )

            val checkboxes = mapOf(
                R.id.checkboxCharcoalCooler to binding.checkboxCharcoalCooler,
                R.id.checkboxHandWashingFacility to binding.checkboxHandWashingFacility,
                R.id.checkboxWashrooms to binding.checkboxWashrooms,
                R.id.checkboxOthers to binding.checkboxOthers
            )

            // Set the initial state of checkboxes
            checkboxIds.forEach { (id, label) ->
                val checkBox = checkboxes[id] ?: return@forEach
                checkBox.isChecked = form.disease?.contains(label) == true
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    val newDisease = form.disease?.let { current ->
                        if (isChecked) "$current,$label" else current.replace(",$label", "")
                    } ?: if (isChecked) label else ""
                    form.disease = newDisease
                }
            }
        }

        // Setup the product selection dropdown
        private fun setupProductSelection(form: PreventativeDisease) {
            val adapter = ArrayAdapter(binding.root.context, android.R.layout.simple_dropdown_item_1line, products)
            binding.selectProductAutoComplete.setAdapter(adapter)
            binding.selectProductAutoComplete.setText(form.product, false)
            binding.selectProductAutoComplete.setOnItemClickListener { _, _, position, _ ->
                form.product = products[position]
                validateField(binding.selectProductInputLayout, form.product ?: "")
            }
        }

        // Setup all other text input fields with validation
        private fun setupTextInputs(form: PreventativeDisease) {
            setupTextInput(binding.categoryEditText, binding.categoryInputLayout, form.category ?: "") { form.category = it }
            setupTextInput(binding.formulationEditText, binding.formulationInputLayout, form.formulation ?: "") { form.formulation = it }
            setupTextInput(binding.dosageRateEditText, binding.dosageRateInputLayout, form.dosage ?: "") { form.dosage = it }
            setupTextInput(binding.unitEditText, binding.unitInputLayout, form.unit ?: "") { form.unit = it }
            setupTextInput(binding.costPerUnitEditText, binding.costPerUnitInputLayout, form.cost_per_unit ?: "") { form.cost_per_unit = it }
            setupTextInput(binding.volumeOfWaterEditText, binding.volumeOfWaterInputLayout, form.volume_of_water ?: "") { form.volume_of_water = it }
            setupTextInput(binding.frequencyOfApplicationEditText, binding.frequencyOfApplicationInputLayout, form.frequency_of_application ?: "") { form.frequency_of_application = it }
            setupTextInput(binding.totalCostEditText, binding.totalCostInputLayout, form.total_cost ?: "") { form.total_cost = it }
        }

        // Helper method to setup individual text inputs with validation
        private fun setupTextInput(
            editText: com.google.android.material.textfield.TextInputEditText,
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
