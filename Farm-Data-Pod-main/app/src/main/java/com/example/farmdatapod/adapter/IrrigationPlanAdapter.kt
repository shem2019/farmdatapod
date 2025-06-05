package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemIrrigationFormBinding
import com.example.farmdatapod.PlanIrrigation // Corrected import

class IrrigationPlanAdapter(
    private val onValidationError: (String) -> Unit
) : RecyclerView.Adapter<IrrigationPlanAdapter.IrrigationPlanViewHolder>() {

    private val irrigationPlans = mutableListOf<PlanIrrigation>()

    private val irrigationTypes = listOf("Canal", "Drip", "Overhead", "Flood")
    private val frequencyOptions = listOf("Planting", "Early Vegetative", "Late Vegetative", "Flowering", "Fruiting")

    fun addNewIrrigationPlan() {
        irrigationPlans.add(PlanIrrigation())
        notifyItemInserted(irrigationPlans.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IrrigationPlanViewHolder {
        val binding = ItemIrrigationFormBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IrrigationPlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IrrigationPlanViewHolder, position: Int) {
        holder.bind(irrigationPlans[position])
    }

    override fun getItemCount(): Int = irrigationPlans.size

    fun validateForms(): Boolean {
        return irrigationPlans.all { plan ->
            !plan.type_of_irrigation.isNullOrBlank() &&
                    !plan.frequency.isNullOrBlank() &&
                    !plan.discharge_hours.isNullOrBlank() &&
                    !plan.unit_cost.isNullOrBlank() &&
                    !plan.cost_of_fuel.isNullOrBlank()
        }
    }

    fun getIrrigationPlans(): List<PlanIrrigation> = irrigationPlans

    inner class IrrigationPlanViewHolder(private val binding: ItemIrrigationFormBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            val irrigationTypeAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_dropdown_item_1line, irrigationTypes)
            binding.typeOfIrrigationAutoComplete.setAdapter(irrigationTypeAdapter)

            val frequencyAdapter = ArrayAdapter(binding.root.context, android.R.layout.simple_dropdown_item_1line, frequencyOptions)
            binding.frequencyAutoComplete.setAdapter(frequencyAdapter)

            setupTextChangedListeners()
        }

        private fun setupTextChangedListeners() {
            binding.typeOfIrrigationAutoComplete.setOnItemClickListener { _, _, position, _ ->
                irrigationPlans[adapterPosition].type_of_irrigation = irrigationTypes[position]
                validateField(binding.typeOfIrrigationAutoComplete, irrigationPlans[adapterPosition].type_of_irrigation ?: "")
            }

            binding.frequencyAutoComplete.setOnItemClickListener { _, _, position, _ ->
                irrigationPlans[adapterPosition].frequency = frequencyOptions[position]
                validateField(binding.frequencyAutoComplete, irrigationPlans[adapterPosition].frequency ?: "")
            }

            binding.dischargeHoursEditText.addTextChangedListener { text ->
                irrigationPlans[adapterPosition].discharge_hours = text.toString()
                validateField(binding.dischargeHoursEditText, text.toString())
            }

            binding.unitCostEditText.addTextChangedListener { text ->
                irrigationPlans[adapterPosition].unit_cost = text.toString()
                validateField(binding.unitCostEditText, text.toString())
            }

            binding.costOfFuelEditText.addTextChangedListener { text ->
                irrigationPlans[adapterPosition].cost_of_fuel = text.toString()
                validateField(binding.costOfFuelEditText, text.toString())
            }
        }

        fun bind(irrigationPlan: PlanIrrigation) {
            binding.typeOfIrrigationAutoComplete.setText(irrigationPlan.type_of_irrigation ?: "", false)
            binding.frequencyAutoComplete.setText(irrigationPlan.frequency ?: "", false)
            binding.dischargeHoursEditText.setText(irrigationPlan.discharge_hours ?: "")
            binding.unitCostEditText.setText(irrigationPlan.unit_cost ?: "")
            binding.costOfFuelEditText.setText(irrigationPlan.cost_of_fuel ?: "")
        }

        private fun validateField(view: View, input: String) {
            if (input.isBlank()) {
                onValidationError("This field is required")
            }
        }
    }
}