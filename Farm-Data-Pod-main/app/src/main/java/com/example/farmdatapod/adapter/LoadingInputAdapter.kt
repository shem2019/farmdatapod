package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoadingInputAdapter : RecyclerView.Adapter<LoadingInputAdapter.ViewHolder>() {

    private val loadingInputs = mutableListOf<LoadingInput>()
    private var onDataChangedListener: ((List<LoadingInput>) -> Unit)? = null

    fun setOnDataChangedListener(listener: (List<LoadingInput>) -> Unit) {
        onDataChangedListener = listener
    }

    fun addLoadingInput() {
        loadingInputs.add(LoadingInput())
        notifyItemInserted(loadingInputs.size - 1)
        onDataChangedListener?.invoke(loadingInputs)
    }

    fun getLoadingInputs(): List<LoadingInput> = loadingInputs

    // Clear loading inputs
    fun clearLoadingInputs() {
        val size = loadingInputs.size
        loadingInputs.clear()
        notifyItemRangeRemoved(0, size)
        onDataChangedListener?.invoke(loadingInputs)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dispatchers_input, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(loadingInputs[position])
    }

    override fun getItemCount(): Int = loadingInputs.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val grnLayout: TextInputLayout = itemView.findViewById(R.id.grnLayout)
        private val grnInput: TextInputEditText = itemView.findViewById(R.id.grnInput)
        private val inputLayout: TextInputLayout = itemView.findViewById(R.id.inputLayout)
        private val inputInput: TextInputEditText = itemView.findViewById(R.id.inputInput)
        private val descriptionLayout: TextInputLayout = itemView.findViewById(R.id.descriptionLayout)
        private val descriptionInput: TextInputEditText = itemView.findViewById(R.id.descriptionInput)
        private val numberOfUnitsLayout: TextInputLayout = itemView.findViewById(R.id.numberOfUnitsLayout)
        private val numberOfUnitsInput: TextInputEditText = itemView.findViewById(R.id.numberOfUnitsInput)

        fun bind(loadingInput: LoadingInput) {
            grnInput.setText(loadingInput.grn)
            inputInput.setText(loadingInput.input)
            descriptionInput.setText(loadingInput.description)
            numberOfUnitsInput.setText(loadingInput.numberOfUnits)

            setupTextWatcher(grnInput) { loadingInput.grn = it }
            setupTextWatcher(inputInput) { loadingInput.input = it }
            setupTextWatcher(descriptionInput) { loadingInput.description = it }
            setupTextWatcher(numberOfUnitsInput) { loadingInput.numberOfUnits = it }
        }

        private fun setupTextWatcher(editText: TextInputEditText, onTextChanged: (String) -> Unit) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    onTextChanged(s.toString())
                    validateField(editText)
                    onDataChangedListener?.invoke(loadingInputs)
                }
            })
        }

        private fun validateField(editText: TextInputEditText) {
            val layout = editText.parent.parent as TextInputLayout
            if (editText.text.isNullOrBlank()) {
                layout.error = "This field is required"
            } else {
                layout.error = null
            }
        }
    }
}

data class LoadingInput(
    var grn: String = "",
    var input: String = "",
    var description: String = "",
    var numberOfUnits: String = "",
    val initialGrn: String = grn,
    val initialInput: String = input,
    val initialDescription: String = description,
    val initialNumberOfUnits: String = numberOfUnits
)