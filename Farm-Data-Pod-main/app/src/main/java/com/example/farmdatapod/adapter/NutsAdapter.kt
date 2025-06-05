package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.NutsDiv
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class NutsAdapter(private var items: List<NutsDiv>) : RecyclerView.Adapter<NutsAdapter.ViewHolder>() {

    private var onItemChangedListener: ((Int, String) -> Unit)? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gradeLayout: TextInputLayout = itemView.findViewById(R.id.gradeLayout)
        val gradeInput: TextInputEditText = itemView.findViewById(R.id.gradeInput)
        val oilContentLayout: TextInputLayout = itemView.findViewById(R.id.oilContentLayout)
        val oilContentInput: TextInputEditText = itemView.findViewById(R.id.oilContentInput)
        val maturityLayout: TextInputLayout = itemView.findViewById(R.id.maturityLayout)
        val maturityInput: TextInputEditText = itemView.findViewById(R.id.maturityInput)
        val foreignMatterLayout: TextInputLayout = itemView.findViewById(R.id.foreignMatterLayout)
        val foreignMatterInput: TextInputEditText = itemView.findViewById(R.id.foreignMatterInput)
        val mechanicalDamageLayout: TextInputLayout = itemView.findViewById(R.id.mechanicalDamageLayout)
        val mechanicalDamageInput: TextInputEditText = itemView.findViewById(R.id.mechanicalDamageInput)
        val sizeLayout: TextInputLayout = itemView.findViewById(R.id.sizeLayout)
        val sizeInput: TextInputEditText = itemView.findViewById(R.id.sizeInput)
        val pestDiseaseLayout: TextInputLayout = itemView.findViewById(R.id.pestDiseaseLayout)
        val pestDiseaseInput: TextInputEditText = itemView.findViewById(R.id.pestDiseaseInput)
        val mouldLayout: TextInputLayout = itemView.findViewById(R.id.mouldLayout)
        val mouldInput: TextInputEditText = itemView.findViewById(R.id.mouldInput)
        val other1Layout: TextInputLayout = itemView.findViewById(R.id.other1Layout)
        val other1Input: TextInputEditText = itemView.findViewById(R.id.other1Input)
        val other2Layout: TextInputLayout = itemView.findViewById(R.id.other2Layout)
        val other2Input: TextInputEditText = itemView.findViewById(R.id.other2Input)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nuts, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.gradeInput.setText(item.grade)
        holder.oilContentInput.setText(item.oil_content)
        holder.maturityInput.setText(item.maturity)
        holder.foreignMatterInput.setText(item.foreign_matter)
        holder.mechanicalDamageInput.setText(item.mechanical_damage)
        holder.sizeInput.setText(item.size)
        holder.pestDiseaseInput.setText(item.pest_and_disease)
        holder.mouldInput.setText(item.mould)
        holder.other1Input.setText(item.other1)
        holder.other2Input.setText(item.other2)

        setupValidationAndTextWatchers(holder, position)
    }

    override fun getItemCount() = items.size

    private fun setupValidationAndTextWatchers(holder: ViewHolder, position: Int) {
        setupInputField(holder.gradeInput, holder.gradeLayout, position, ::validateGrade)
        setupInputField(holder.oilContentInput, holder.oilContentLayout, position) { layout, input -> validatePercentage(layout, input, "Oil content") }
        setupInputField(holder.maturityInput, holder.maturityLayout, position) { layout, input -> validatePercentage(layout, input, "Maturity") }
        setupInputField(holder.foreignMatterInput, holder.foreignMatterLayout, position) { layout, input -> validatePercentage(layout, input, "Foreign matter") }
        setupInputField(holder.mechanicalDamageInput, holder.mechanicalDamageLayout, position) { layout, input -> validatePercentage(layout, input, "Mechanical damage") }
        setupInputField(holder.sizeInput, holder.sizeLayout, position, ::validateSize)
        setupInputField(holder.pestDiseaseInput, holder.pestDiseaseLayout, position) { layout, input -> validatePercentage(layout, input, "Pest and disease") }
        setupInputField(holder.mouldInput, holder.mouldLayout, position) { layout, input -> validatePercentage(layout, input, "Mould") }
        setupInputField(holder.other1Input, holder.other1Layout, position) { layout, input -> validateOther(layout, input, "Other 1") }
        setupInputField(holder.other2Input, holder.other2Layout, position) { layout, input -> validateOther(layout, input, "Other 2") }
    }

    private fun setupInputField(
        input: TextInputEditText,
        layout: TextInputLayout,
        position: Int,
        validationFunction: (TextInputLayout, String) -> Unit
    ) {
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validationFunction(layout, s.toString())
                onItemChangedListener?.invoke(position, s.toString())
            }
        })

        input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validationFunction(layout, input.text.toString())
            }
        }
    }

    private fun validateGrade(layout: TextInputLayout, input: String) {
        when {
            input.isEmpty() -> layout.error = "Grade cannot be empty"
            !input.matches(Regex("^[A-Za-z0-9]+$")) -> layout.error = "Grade should only contain letters and numbers"
            else -> layout.error = null
        }
    }

    private fun validatePercentage(layout: TextInputLayout, input: String, fieldName: String) {
        when {
            input.isEmpty() -> layout.error = "$fieldName cannot be empty"
            !input.matches(Regex("^\\d+(\\.\\d+)?%?$")) -> layout.error = "Invalid $fieldName value"
            input.replace("%", "").toFloat() !in 0f..100f -> layout.error = "$fieldName should be between 0% and 100%"
            else -> layout.error = null
        }
    }

    private fun validateSize(layout: TextInputLayout, input: String) {
        when {
            input.isEmpty() -> layout.error = "Size cannot be empty"
            !input.matches(Regex("^\\d+(\\.\\d+)?$")) -> layout.error = "Invalid size value"
            else -> layout.error = null
        }
    }

    private fun validateOther(layout: TextInputLayout, input: String, fieldName: String) {
        when {
            input.isEmpty() -> layout.error = "$fieldName cannot be empty"
            !input.matches(Regex("^[A-Za-z0-9\\s]+$")) -> layout.error = "$fieldName should only contain letters, numbers, and spaces"
            else -> layout.error = null
        }
    }

    fun updateItems(newItems: List<NutsDiv>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setOnItemChangedListener(listener: (Int, String) -> Unit) {
        onItemChangedListener = listener
    }
}