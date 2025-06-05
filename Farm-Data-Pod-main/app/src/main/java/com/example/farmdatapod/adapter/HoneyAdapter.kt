package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.HoneyDiv
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class HoneyAdapter(private var items: List<HoneyDiv>) : RecyclerView.Adapter<HoneyAdapter.ViewHolder>() {

    private var onItemChangedListener: ((Int, String) -> Unit)? = null

    fun setOnItemChangedListener(listener: (Int, String) -> Unit) {
        onItemChangedListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gradeLayout: TextInputLayout = itemView.findViewById(R.id.gradeLayout)
        val gradeInput: TextInputEditText = itemView.findViewById(R.id.gradeInput)
        val crystallizationLayout: TextInputLayout = itemView.findViewById(R.id.crystallizationLayout)
        val crystallizationInput: TextInputEditText = itemView.findViewById(R.id.crystallizationInput)
        val viscosityLayout: TextInputLayout = itemView.findViewById(R.id.viscosityLayout)
        val viscosityInput: TextInputEditText = itemView.findViewById(R.id.viscosityInput)
        val sizeLayout: TextInputLayout = itemView.findViewById(R.id.sizeLayout)
        val sizeInput: TextInputEditText = itemView.findViewById(R.id.sizeInput)
        val foreignMatterLayout: TextInputLayout = itemView.findViewById(R.id.foreignMatterLayout)
        val foreignMatterInput: TextInputEditText = itemView.findViewById(R.id.foreignMatterInput)
        val other1Layout: TextInputLayout = itemView.findViewById(R.id.other1Layout)
        val other1Input: TextInputEditText = itemView.findViewById(R.id.other1Input)
        val other2Layout: TextInputLayout = itemView.findViewById(R.id.other2Layout)
        val other2Input: TextInputEditText = itemView.findViewById(R.id.other2Input)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_honey, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.gradeInput.setText(item.grade)
        holder.crystallizationInput.setText(item.crystallization)
        holder.viscosityInput.setText(item.viscosity)
        holder.sizeInput.setText(item.size)
        holder.foreignMatterInput.setText(item.foreign_matter)
        holder.other1Input.setText(item.other1)
        holder.other2Input.setText(item.other2)

        setupValidation(holder)
        setupTextChangedListeners(holder)
    }

    override fun getItemCount() = items.size

    private fun setupTextChangedListeners(holder: ViewHolder) {
        holder.gradeInput.addTextChangedListener(createTextWatcher(0))
        holder.crystallizationInput.addTextChangedListener(createTextWatcher(1))
        holder.viscosityInput.addTextChangedListener(createTextWatcher(2))
        holder.sizeInput.addTextChangedListener(createTextWatcher(3))
        holder.foreignMatterInput.addTextChangedListener(createTextWatcher(4))
        holder.other1Input.addTextChangedListener(createTextWatcher(5))
        holder.other2Input.addTextChangedListener(createTextWatcher(6))
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

    private fun setupValidation(holder: ViewHolder) {
        holder.gradeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateGrade(holder.gradeLayout, holder.gradeInput.text.toString())
            }
        }

        holder.crystallizationInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateCrystallization(holder.crystallizationLayout, holder.crystallizationInput.text.toString())
            }
        }

        holder.viscosityInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateViscosity(holder.viscosityLayout, holder.viscosityInput.text.toString())
            }
        }

        holder.sizeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateSize(holder.sizeLayout, holder.sizeInput.text.toString())
            }
        }

        holder.foreignMatterInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateForeignMatter(holder.foreignMatterLayout, holder.foreignMatterInput.text.toString())
            }
        }

        holder.other1Input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateOther(holder.other1Layout, holder.other1Input.text.toString(), "Other 1")
            }
        }

        holder.other2Input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateOther(holder.other2Layout, holder.other2Input.text.toString(), "Other 2")
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

    private fun validateCrystallization(layout: TextInputLayout, input: String) {
        when {
            input.isEmpty() -> layout.error = "Crystallization cannot be empty"
            !input.matches(Regex("^[A-Za-z\\s]+$")) -> layout.error = "Crystallization should only contain letters and spaces"
            else -> layout.error = null
        }
    }

    private fun validateViscosity(layout: TextInputLayout, input: String) {
        when {
            input.isEmpty() -> layout.error = "Viscosity cannot be empty"
            !input.matches(Regex("^[A-Za-z\\s]+$")) -> layout.error = "Viscosity should only contain letters and spaces"
            else -> layout.error = null
        }
    }

    private fun validateSize(layout: TextInputLayout, input: String) {
        when {
            input.isEmpty() -> layout.error = "Size cannot be empty"
            !input.matches(Regex("^[A-Za-z0-9\\s]+$")) -> layout.error = "Size should only contain letters, numbers, and spaces"
            else -> layout.error = null
        }
    }

    private fun validateForeignMatter(layout: TextInputLayout, input: String) {
        when {
            input.isEmpty() -> layout.error = "Foreign matter cannot be empty"
            !input.matches(Regex("^\\d+(\\.\\d+)?%?$")) -> layout.error = "Invalid foreign matter value"
            input.replace("%", "").toFloat() !in 0f..100f -> layout.error = "Foreign matter should be between 0% and 100%"
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

    fun updateItems(newItems: List<HoneyDiv>) {
        items = newItems
        notifyDataSetChanged()
    }
}