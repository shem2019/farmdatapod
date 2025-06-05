package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.DefaultDiv
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DefaultAdapter(private var items: List<DefaultDiv>) : RecyclerView.Adapter<DefaultAdapter.ViewHolder>() {

    private var itemChangedListener: ((Int, DefaultDiv) -> Unit)? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gradeInput: TextInputEditText = itemView.findViewById(R.id.gradeInput)
        val moistureInput: TextInputEditText = itemView.findViewById(R.id.moistureInput)
        val maturityInput: TextInputEditText = itemView.findViewById(R.id.maturityInput)
        val foreignMatterInput: TextInputEditText = itemView.findViewById(R.id.foreignMatterInput)
        val sizeInput: TextInputEditText = itemView.findViewById(R.id.sizeInput)
        val pestDiseaseInput: TextInputEditText = itemView.findViewById(R.id.pestDiseaseInput)
        val mouldInput: TextInputEditText = itemView.findViewById(R.id.mouldInput)
        val other1Input: TextInputEditText = itemView.findViewById(R.id.other1Input)
        val other2Input: TextInputEditText = itemView.findViewById(R.id.other2Input)

        // Bind data and set up change listeners
        fun bind(item: DefaultDiv) {
            gradeInput.setText(item.grade)
            moistureInput.setText(item.moisture)
            maturityInput.setText(item.maturity)
            foreignMatterInput.setText(item.foreign_matter)
            sizeInput.setText(item.size)
            pestDiseaseInput.setText(item.pest_and_disease)
            mouldInput.setText(item.mould)
            other1Input.setText(item.other1)
            other2Input.setText(item.other2)

            setListeners(item)
        }

        // Set listeners on input fields
        private fun setListeners(item: DefaultDiv) {
            gradeInput.addTextChangedListener(createTextWatcher { text ->
                item.grade = text
                notifyItemChanged(item)
            })
            moistureInput.addTextChangedListener(createTextWatcher { text ->
                item.moisture = text
                notifyItemChanged(item)
            })
            maturityInput.addTextChangedListener(createTextWatcher { text ->
                item.maturity = text
                notifyItemChanged(item)
            })
            foreignMatterInput.addTextChangedListener(createTextWatcher { text ->
                item.foreign_matter = text
                notifyItemChanged(item)
            })
            sizeInput.addTextChangedListener(createTextWatcher { text ->
                item.size = text
                notifyItemChanged(item)
            })
            pestDiseaseInput.addTextChangedListener(createTextWatcher { text ->
                item.pest_and_disease = text
                notifyItemChanged(item)
            })
            mouldInput.addTextChangedListener(createTextWatcher { text ->
                item.mould = text
                notifyItemChanged(item)
            })
            other1Input.addTextChangedListener(createTextWatcher { text ->
                item.other1 = text
                notifyItemChanged(item)
            })
            other2Input.addTextChangedListener(createTextWatcher { text ->
                item.other2 = text
                notifyItemChanged(item)
            })
        }

        // TextWatcher to track changes
        private fun createTextWatcher(onTextChanged: (String) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextChanged(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        }

        private fun notifyItemChanged(item: DefaultDiv) {
            itemChangedListener?.invoke(adapterPosition, item)
        }
    }

    // Inflate the view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_default_check, parent, false)
        return ViewHolder(view)
    }

    // Bind the view holder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // Get item count
    override fun getItemCount() = items.size

    // Set a listener for item changes
    fun setOnItemChangedListener(listener: (Int, DefaultDiv) -> Unit) {
        this.itemChangedListener = listener
    }

    // Update items in the adapter
    fun updateItems(newItems: List<DefaultDiv>) {
        items = newItems
        notifyDataSetChanged()
    }
}
