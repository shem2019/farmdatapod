package com.example.farmdatapod.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.StakingActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class StakingActivityAdapter(
    private var stakingActivities: MutableList<StakingActivity>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<StakingActivityAdapter.StakingViewHolder>() {

    inner class StakingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val unitStakesInput: TextInputEditText = itemView.findViewById(R.id.unitStakesInput)
        private val costPerUnitInput: TextInputEditText = itemView.findViewById(R.id.costPerUnitInput)
        private val manDaysInput: TextInputEditText = itemView.findViewById(R.id.manDaysInput)
        private val unitCostInput: TextInputEditText = itemView.findViewById(R.id.unitCostInput)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.removeButton)

        fun bind(stakingActivity: StakingActivity) {
            // Set initial values
            unitStakesInput.setText(stakingActivity.unit_stakes.toString())
            costPerUnitInput.setText(stakingActivity.cost_per_unit.toString())
            manDaysInput.setText(stakingActivity.man_days.toString())
            unitCostInput.setText(stakingActivity.unit_cost_of_labor.toString())

            // Handle remove button click
            removeButton.setOnClickListener {
                onRemoveClick(adapterPosition)
            }

            // Handle input changes
            setupInputListeners()
        }

        private fun setupInputListeners() {
            unitStakesInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateStakingActivity()
                }
            }

            costPerUnitInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateStakingActivity()
                }
            }

            manDaysInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateStakingActivity()
                }
            }

            unitCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    updateStakingActivity()
                }
            }
        }

        private fun updateStakingActivity() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                stakingActivities[position] = stakingActivities[position].copy(
                    unit_stakes = unitStakesInput.text?.toString()?.toIntOrNull() ?: 0,
                    cost_per_unit = costPerUnitInput.text?.toString()?.toDoubleOrNull() ?: 0.0,
                    man_days = manDaysInput.text?.toString()?.toIntOrNull() ?: 0,
                    unit_cost_of_labor = unitCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StakingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_staking, parent, false)
        return StakingViewHolder(view)
    }

    override fun onBindViewHolder(holder: StakingViewHolder, position: Int) {
        holder.bind(stakingActivities[position])
    }

    override fun getItemCount() = stakingActivities.size

    fun updateData(newData: List<StakingActivity>) {
        stakingActivities.clear()
        stakingActivities.addAll(newData)
        notifyDataSetChanged()
    }
}