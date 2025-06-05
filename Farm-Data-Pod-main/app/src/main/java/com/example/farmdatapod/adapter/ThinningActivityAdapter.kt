package com.example.farmdatapod.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.ThinningActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ThinningActivityAdapter(
    private var thinningActivities: MutableList<ThinningActivity>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ThinningActivityAdapter.ThinningViewHolder>() {

    inner class ThinningViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val equipmentUsedInput: TextInputEditText = itemView.findViewById(R.id.equipmentUsedInput)
        private val manDaysInput: TextInputEditText = itemView.findViewById(R.id.manDaysInput)
        private val unitCostInput: TextInputEditText = itemView.findViewById(R.id.unitCostInput)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.removeButton)

        fun bind(thinningActivity: ThinningActivity) {
            // Set initial values
            equipmentUsedInput.setText(thinningActivity.equipment_used)
            manDaysInput.setText(thinningActivity.man_days.toString())
            unitCostInput.setText(thinningActivity.unit_cost_of_labor.toString())

            // Handle remove button click
            removeButton.setOnClickListener {
                onRemoveClick(adapterPosition)
            }

            // Handle input changes
            equipmentUsedInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateThinningActivity()
                }
            }

            manDaysInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateThinningActivity()
                }
            }

            unitCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateThinningActivity()
                }
            }
        }

        private fun updateThinningActivity() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                thinningActivities[position] = thinningActivities[position].copy(
                    equipment_used = equipmentUsedInput.text?.toString() ?: "",
                    man_days = manDaysInput.text?.toString()?.toIntOrNull() ?: 0,
                    unit_cost_of_labor = unitCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThinningViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thinning, parent, false)
        return ThinningViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThinningViewHolder, position: Int) {
        holder.bind(thinningActivities[position])
    }

    override fun getItemCount() = thinningActivities.size

    fun updateData(newData: List<ThinningActivity>) {
        thinningActivities.clear()
        thinningActivities.addAll(newData)
        notifyDataSetChanged()
    }
}