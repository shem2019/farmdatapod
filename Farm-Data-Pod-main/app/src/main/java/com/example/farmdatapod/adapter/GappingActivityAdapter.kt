package com.example.farmdatapod.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.GappingActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class GappingActivityAdapter(
    private var gappingActivities: MutableList<GappingActivity>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<GappingActivityAdapter.GappingViewHolder>() {

    inner class GappingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val manDaysInput: TextInputEditText = itemView.findViewById(R.id.manDaysInput)
        private val unitCostInput: TextInputEditText = itemView.findViewById(R.id.unitCostInput)
        private val cropPopulationInput: TextInputEditText = itemView.findViewById(R.id.cropPopulationInput)
        private val targetPopulationInput: TextInputEditText = itemView.findViewById(R.id.targetPopulationInput)
        private val plantingMaterialInput: TextInputEditText = itemView.findViewById(R.id.plantingMaterialInput)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.removeButton)

        fun bind(gappingActivity: GappingActivity) {
            // Set initial values
            manDaysInput.setText(gappingActivity.man_days.toString())
            unitCostInput.setText(gappingActivity.unit_cost_of_labor.toString())
            cropPopulationInput.setText(gappingActivity.crop_population.toString())
            targetPopulationInput.setText(gappingActivity.target_population.toString())
            plantingMaterialInput.setText(gappingActivity.planting_material)

            // Handle remove button click
            removeButton.setOnClickListener {
                onRemoveClick(adapterPosition)
            }

            // Handle input changes
            setupInputListeners()
        }

        private fun setupInputListeners() {
            manDaysInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateGappingActivity()
                }
            }

            unitCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateGappingActivity()
                }
            }

            cropPopulationInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateGappingActivity()
                }
            }

            targetPopulationInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateGappingActivity()
                }
            }

            plantingMaterialInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateGappingActivity()
                }
            }
        }

        private fun updateGappingActivity() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                gappingActivities[position] = gappingActivities[position].copy(
                    man_days = manDaysInput.text?.toString()?.toIntOrNull() ?: 0,
                    unit_cost_of_labor = unitCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0,
                    crop_population = cropPopulationInput.text?.toString()?.toIntOrNull() ?: 0,
                    target_population = targetPopulationInput.text?.toString()?.toIntOrNull() ?: 0,
                    planting_material = plantingMaterialInput.text?.toString() ?: ""
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GappingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gapping, parent, false)
        return GappingViewHolder(view)
    }

    override fun onBindViewHolder(holder: GappingViewHolder, position: Int) {
        holder.bind(gappingActivities[position])
    }

    override fun getItemCount() = gappingActivities.size

    fun updateData(newData: List<GappingActivity>) {
        gappingActivities.clear()
        gappingActivities.addAll(newData)
        notifyDataSetChanged()
    }
}