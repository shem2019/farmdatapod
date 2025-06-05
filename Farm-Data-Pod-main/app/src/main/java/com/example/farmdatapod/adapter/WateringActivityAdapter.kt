package com.example.farmdatapod.adapter


import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.models.WateringActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class WateringActivityAdapter(
    private var wateringActivities: MutableList<WateringActivity>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<WateringActivityAdapter.WateringViewHolder>() {

    private val irrigationTypes = listOf(
        "Surface Irrigation",
        "Drip Irrigation",
        "Sprinkler Irrigation",
        "Subsurface Irrigation",
        "Furrow Irrigation",
        "Basin Irrigation",
        "Border Irrigation",
        "Pivot Irrigation (Center Pivot)",
        "Lateral Move Irrigation",
        "Manual Irrigation (Using buckets or watering cans)"
    )

    private val wateringFrequencies = listOf(
        "Daily",
        "Every 2-3 days",
        "Once a week",
        "Twice a week",
        "As needed (based on soil moisture)",
        "During critical growth stages"
    )

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class WateringViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val irrigationTypeDropdown: AutoCompleteTextView = itemView.findViewById(R.id.irrigationTypeDropdown)
        private val startTimeInput: TextInputEditText = itemView.findViewById(R.id.startTimeInput)
        private val endTimeInput: TextInputEditText = itemView.findViewById(R.id.endTimeInput)
        private val dischargeHoursInput: TextInputEditText = itemView.findViewById(R.id.dischargeHoursInput)
        private val frequencyDropdown: AutoCompleteTextView = itemView.findViewById(R.id.frequencyDropdown)
        private val manDaysInput: TextInputEditText = itemView.findViewById(R.id.manDaysInput)
        private val fuelCostInput: TextInputEditText = itemView.findViewById(R.id.fuelCostInput)
        private val unitCostInput: TextInputEditText = itemView.findViewById(R.id.unitCostInput)
        private val laborCostInput: TextInputEditText = itemView.findViewById(R.id.laborCostInput)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.removeButton)

        fun bind(wateringActivity: WateringActivity) {
            // Setup dropdown adapters
            setupDropdowns()

            // Set initial values
            irrigationTypeDropdown.setText(wateringActivity.type_of_irrigation, false)
            startTimeInput.setText(wateringActivity.start_time)
            endTimeInput.setText(wateringActivity.end_time)
            dischargeHoursInput.setText(wateringActivity.discharge_hours.toString())
            frequencyDropdown.setText(wateringActivity.frequency_of_watering, false)
            manDaysInput.setText(wateringActivity.man_days.toString())
            fuelCostInput.setText(wateringActivity.cost_of_fuel.toString())
            unitCostInput.setText(wateringActivity.unit_cost.toString())
            laborCostInput.setText(wateringActivity.unit_cost_of_labor.toString())

            // Setup time pickers
            setupTimePickers()

            // Handle remove button click
            removeButton.setOnClickListener {
                onRemoveClick(adapterPosition)
            }

            // Handle field changes
            setupFieldListeners()
        }

        private fun setupDropdowns() {
            val irrigationAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_dropdown_item_1line,
                irrigationTypes
            )
            irrigationTypeDropdown.setAdapter(irrigationAdapter)

            val frequencyAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_dropdown_item_1line,
                wateringFrequencies
            )
            frequencyDropdown.setAdapter(frequencyAdapter)
        }

        private fun setupTimePickers() {
            startTimeInput.setOnClickListener {
                showTimePicker(startTimeInput)
            }
            endTimeInput.setOnClickListener {
                showTimePicker(endTimeInput)
            }
        }

        private fun showTimePicker(input: TextInputEditText) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                itemView.context,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    input.setText(timeFormat.format(calendar.time))
                    updateWateringActivity()
                },
                currentHour,
                currentMinute,
                true
            ).show()
        }

        private fun setupFieldListeners() {
            irrigationTypeDropdown.setOnItemClickListener { _, _, position, _ ->
                wateringActivities[adapterPosition] = wateringActivities[adapterPosition].copy(
                    type_of_irrigation = irrigationTypes[position]
                )
            }

            frequencyDropdown.setOnItemClickListener { _, _, position, _ ->
                wateringActivities[adapterPosition] = wateringActivities[adapterPosition].copy(
                    frequency_of_watering = wateringFrequencies[position]
                )
            }

            dischargeHoursInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) updateWateringActivity()
            }

            manDaysInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) updateWateringActivity()
            }

            fuelCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) updateWateringActivity()
            }

            unitCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) updateWateringActivity()
            }

            laborCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) updateWateringActivity()
            }
        }

        private fun updateWateringActivity() {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                wateringActivities[position] = wateringActivities[position].copy(
                    start_time = startTimeInput.text?.toString() ?: "",
                    end_time = endTimeInput.text?.toString() ?: "",
                    discharge_hours = dischargeHoursInput.text?.toString()?.toIntOrNull() ?: 0,
                    man_days = manDaysInput.text?.toString()?.toIntOrNull() ?: 0,
                    cost_of_fuel = fuelCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0,
                    unit_cost = unitCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0,
                    unit_cost_of_labor = laborCostInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WateringViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watering, parent, false)
        return WateringViewHolder(view)
    }

    override fun onBindViewHolder(holder: WateringViewHolder, position: Int) {
        holder.bind(wateringActivities[position])
    }

    override fun getItemCount() = wateringActivities.size

    fun updateData(newData: List<WateringActivity>) {
        wateringActivities.clear()
        wateringActivities.addAll(newData)
        notifyDataSetChanged()
    }
}