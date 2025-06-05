package com.example.farmdatapod.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.PruningActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PruningActivityAdapter(
    private var pruningActivities: MutableList<PruningActivity>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<PruningActivityAdapter.PruningViewHolder>() {

    inner class PruningViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val equipmentUsedInput: TextInputEditText = itemView.findViewById(R.id.equipmentUsedInput)
        private val manDaysInput: TextInputEditText = itemView.findViewById(R.id.manDaysInput)
        private val unitCostInput: TextInputEditText = itemView.findViewById(R.id.unitCostInput)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.removeButton)

        fun bind(pruningActivity: PruningActivity) {
            // Set initial values
            equipmentUsedInput.setText(pruningActivity.equipment_used)
            manDaysInput.setText(pruningActivity.man_days.toString())
            unitCostInput.setText(pruningActivity.unit_cost_of_labor.toString())

            // Handle remove button click
            removeButton.setOnClickListener {
                onRemoveClick(adapterPosition)
            }

            // Handle input changes
            setupInputListeners()
        }

        private fun setupInputListeners() {
            equipmentUsedInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updatePruningActivity()
                }
            }

            manDaysInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updatePruningActivity()
                }
            }

            unitCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updatePruningActivity()
                }
            }
        }

        private fun updatePruningActivity() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                pruningActivities[position] = pruningActivities[position].copy(
                    equipment_used = equipmentUsedInput.text?.toString() ?: "",
                    man_days = manDaysInput.text?.toString()?.toIntOrNull() ?: 0,
                    unit_cost_of_labor = unitCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PruningViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prunning, parent, false)
        return PruningViewHolder(view)
    }

    override fun onBindViewHolder(holder: PruningViewHolder, position: Int) {
        holder.bind(pruningActivities[position])
    }

    override fun getItemCount() = pruningActivities.size

    fun updateData(newData: List<PruningActivity>) {
        pruningActivities.clear()
        pruningActivities.addAll(newData)
        notifyDataSetChanged()
    }
}