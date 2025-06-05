package com.example.farmdatapod.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.WeedingActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class WeedingActivityAdapter(
    private var weedingActivities: MutableList<WeedingActivity>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<WeedingActivityAdapter.WeedingViewHolder>() {

    private val weedingMethods = listOf(
        "Manual Weeding (Hand pulling or hoeing)",
        "Mechanical Weeding (Weeders, cultivators)",
        "Chemical Weeding (Herbicides)",
        "Mulching",
        "Crop Rotation",
        "Cover Cropping",
        "Biological Weeding (Using insects or pathogens)",
        "Burning (Flame weeding or controlled fire)",
        "Smothering (Using dense planting or weed barriers)",
        "Solarization (Using plastic sheets to trap solar heat)"
    )

    inner class WeedingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val weedingMethodLayout: TextInputLayout = itemView.findViewById(R.id.weedingMethodLayout)
        private val weedingMethodDropdown: AutoCompleteTextView = itemView.findViewById(R.id.weedingMethodDropdown)
        private val inputEditText: TextInputEditText = itemView.findViewById(R.id.inputInput)
        private val manDaysEditText: TextInputEditText = itemView.findViewById(R.id.manDaysInput)
        private val unitCostEditText: TextInputEditText = itemView.findViewById(R.id.unitCostInput)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.removeButton)

        fun bind(weedingActivity: WeedingActivity) {
            // Setup dropdown adapter
            val dropdownAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_dropdown_item_1line,
                weedingMethods
            )
            weedingMethodDropdown.setAdapter(dropdownAdapter)

            // Set values
            weedingMethodDropdown.setText(weedingActivity.method_of_weeding, false)
            inputEditText.setText(weedingActivity.input)
            manDaysEditText.setText(weedingActivity.man_days.toString())
            unitCostEditText.setText(weedingActivity.unit_cost_of_labor.toString())

            // Handle remove button click
            removeButton.setOnClickListener {
                onRemoveClick(adapterPosition)
            }

            // Handle dropdown selection changes
            weedingMethodDropdown.setOnItemClickListener { _, _, position, _ ->
                weedingActivities[adapterPosition] = weedingActivities[adapterPosition].copy(
                    method_of_weeding = weedingMethods[position]
                )
            }

            // Handle input changes
            inputEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    weedingActivities[adapterPosition] = weedingActivities[adapterPosition].copy(
                        input = inputEditText.text?.toString() ?: ""
                    )
                }
            }

            manDaysEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val manDays = manDaysEditText.text?.toString()?.toIntOrNull() ?: 0
                    weedingActivities[adapterPosition] = weedingActivities[adapterPosition].copy(
                        man_days = manDays
                    )
                }
            }

            unitCostEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val unitCost = unitCostEditText.text?.toString()?.toDoubleOrNull() ?: 0.0
                    weedingActivities[adapterPosition] = weedingActivities[adapterPosition].copy(
                        unit_cost_of_labor = unitCost
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeedingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weeding, parent, false)
        return WeedingViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeedingViewHolder, position: Int) {
        holder.bind(weedingActivities[position])
    }

    override fun getItemCount() = weedingActivities.size

    fun updateData(newData: List<WeedingActivity>) {
        weedingActivities.clear()
        weedingActivities.addAll(newData)
        notifyDataSetChanged()
    }
}