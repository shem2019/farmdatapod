package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.VegetableDiv
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class VegetableAdapter(private var items: List<VegetableDiv>) : RecyclerView.Adapter<VegetableAdapter.ViewHolder>() {

    private var onItemChangedListener: ((Int, String) -> Unit)? = null

    fun setOnItemChangedListener(listener: (Int, String) -> Unit) {
        onItemChangedListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gradeLayout: TextInputLayout = itemView.findViewById(R.id.gradeLayout)
        val gradeInput: TextInputEditText = itemView.findViewById(R.id.gradeInput)
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vegetation_quality, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.gradeInput.setText(item.grade)
        holder.maturityInput.setText(item.maturity)
        holder.foreignMatterInput.setText(item.foreign_matter)
        holder.mechanicalDamageInput.setText(item.mechanical_damage)
        holder.sizeInput.setText(item.size)
        holder.pestDiseaseInput.setText(item.pest_and_disease)
        holder.mouldInput.setText(item.mould)

        setupValidation(holder)
        setupTextChangedListeners(holder)
    }

    override fun getItemCount() = items.size

    private fun setupValidation(holder: ViewHolder) {
        holder.gradeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateGrade(holder.gradeLayout, holder.gradeInput.text.toString())
        }
        holder.maturityInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePercentage(holder.maturityLayout, holder.maturityInput.text.toString(), "Maturity")
        }
        holder.foreignMatterInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePercentage(holder.foreignMatterLayout, holder.foreignMatterInput.text.toString(), "Foreign matter")
        }
        holder.mechanicalDamageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateTextOrNone(holder.mechanicalDamageLayout, holder.mechanicalDamageInput.text.toString(), "Mechanical damage")
        }
        holder.sizeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePercentage(holder.sizeLayout, holder.sizeInput.text.toString(), "Size")
        }
        holder.pestDiseaseInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateTextOrNone(holder.pestDiseaseLayout, holder.pestDiseaseInput.text.toString(), "Pest and disease")
        }
        holder.mouldInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateTextOrNone(holder.mouldLayout, holder.mouldInput.text.toString(), "Mould")
        }
    }

    private fun setupTextChangedListeners(holder: ViewHolder) {
        holder.gradeInput.addTextChangedListener(createTextWatcher(0))
        holder.maturityInput.addTextChangedListener(createTextWatcher(1))
        holder.foreignMatterInput.addTextChangedListener(createTextWatcher(2))
        holder.mechanicalDamageInput.addTextChangedListener(createTextWatcher(3))
        holder.sizeInput.addTextChangedListener(createTextWatcher(4))
        holder.pestDiseaseInput.addTextChangedListener(createTextWatcher(5))
        holder.mouldInput.addTextChangedListener(createTextWatcher(6))
    }

    private fun createTextWatcher(position: Int): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onItemChangedListener?.invoke(position, s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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

    private fun validateTextOrNone(layout: TextInputLayout, input: String, fieldName: String) {
        when {
            input.isEmpty() -> layout.error = "$fieldName cannot be empty"
            input != "None" && !input.matches(Regex("^[A-Za-z\\s]+$")) -> layout.error = "$fieldName should be 'None' or contain only letters and spaces"
            else -> layout.error = null
        }
    }

    fun updateItems(newItems: List<VegetableDiv>) {
        items = newItems
        notifyDataSetChanged()
    }
}