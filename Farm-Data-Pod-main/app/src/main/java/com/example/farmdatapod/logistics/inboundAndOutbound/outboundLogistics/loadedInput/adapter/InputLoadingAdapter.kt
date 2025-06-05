package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.ItemInputLayoutBinding
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsEntity
import com.example.farmdatapod.logistics.planJourney.data.JourneyBasicInfo
import com.example.farmdatapod.logistics.planJourney.data.JourneyStopPointInfo
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class InputLoadingAdapter : RecyclerView.Adapter<InputLoadingAdapter.ViewHolder>() {
    private var inputs = listOf<PlanJourneyInputsEntity>()
    private var journeyNamesMap = mutableMapOf<Long, String>() // Journey ID to Name mapping
    private var stopPointNamesMap = mutableMapOf<Long, String>() // Stop Point ID to Name mapping
    private val selectedInputs = mutableSetOf<PlanJourneyInputsEntity>()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "KE"))

    inner class ViewHolder(private val binding: ItemInputLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(input: PlanJourneyInputsEntity) {
            with(binding) {
                // Basic Information
                setupBasicInfo(input)

                // Journey and Stop Point Information
                setupJourneyAndStopPointInfo(input)

                // Additional Details
                setupAdditionalDetails(input)

                // Authorization Status
                setupAuthorizationStatus(input)

                // Selection State
                setupSelectionState(input)

                // Click Listeners
                setupClickListeners(input)
            }
        }

        private fun ItemInputLayoutBinding.setupBasicInfo(input: PlanJourneyInputsEntity) {
            // Input name with null safety
            inputNameText.text = input.input.takeIf { it.isNotBlank() } ?: "N/A"

            // Delivery note
            deliveryNoteText.text = "DN: ${input.dn_number.takeIf { it.isNotBlank() } ?: "N/A"}"

            // Units and cost
            unitsText.text = input.number_of_units.toString()
            unitCostText.text = formatCurrency(input.unit_cost)
        }

        private fun ItemInputLayoutBinding.setupJourneyAndStopPointInfo(input: PlanJourneyInputsEntity) {
            // Journey information
            journeyIdText.apply {
                val journeyId = input.journey_id.toLongOrNull() ?: 0L
                text = journeyNamesMap[journeyId] ?: "Journey $journeyId"
                tag = journeyId
            }

            // Stop point information
            stopPointText.apply {
                val stopPointId = input.stop_point_id.toLongOrNull() ?: 0L
                text = stopPointNamesMap[stopPointId] ?: "Stop $stopPointId"
                tag = stopPointId
            }
        }

        private fun ItemInputLayoutBinding.setupAdditionalDetails(input: PlanJourneyInputsEntity) {
            // Description
            descriptionText.text = input.description.takeIf { it.isNotBlank() }
                ?: "No description available"
        }

        private fun ItemInputLayoutBinding.setupAuthorizationStatus(input: PlanJourneyInputsEntity) {
            val authColor = if (input.authorised) R.color.green else R.color.text_secondary
            val authText = if (input.authorised) "Authorized" else "Pending Authorization"

            authStatusText.apply {
                text = authText
                setTextColor(ContextCompat.getColor(context, authColor))
                isVisible = true
            }
        }

        private fun ItemInputLayoutBinding.setupSelectionState(input: PlanJourneyInputsEntity) {
            val isSelected = selectedInputs.contains(input)

            // Card selection state
            (root as MaterialCardView).apply {
                isChecked = isSelected
                strokeWidth = if (isSelected) 2 else 1
            }

            // Selection button state
            selectButton.apply {
                text = if (isSelected) "Selected" else "Select"
                isEnabled = !input.authorised
            }
        }

        private fun ItemInputLayoutBinding.setupClickListeners(input: PlanJourneyInputsEntity) {
            root.setOnClickListener {
                if (!input.authorised) toggleSelection(input)
            }

            selectButton.setOnClickListener {
                if (!input.authorised) toggleSelection(input)
            }
        }
    }

    // Journey and Stop Point Name Updates
    fun updateJourneyNames(journeyInfo: List<JourneyBasicInfo>) {
        journeyNamesMap.clear()
        journeyInfo.forEach { info ->
            journeyNamesMap[info.journey_id] = info.journey_name
        }
        notifyDataSetChanged()
    }

    fun updateStopPointNames(journeyDetails: List<JourneyStopPointInfo>) {
        stopPointNamesMap.clear()
        journeyDetails.forEach { detail ->
            detail.stop_point_id?.let { id ->
                stopPointNamesMap[id] = detail.stop_point_name ?: "Unknown Stop"
            }
        }
        notifyDataSetChanged()
    }

    // Input List Updates
    fun setInputs(newInputs: List<PlanJourneyInputsEntity>) {
        inputs = newInputs
        selectedInputs.clear()
        notifyDataSetChanged()
    }

    // Selection Management
    private fun toggleSelection(input: PlanJourneyInputsEntity) {
        if (selectedInputs.contains(input)) {
            selectedInputs.remove(input)
        } else {
            selectedInputs.add(input)
        }
        notifyItemChanged(inputs.indexOf(input))
    }

    fun clearSelections() {
        selectedInputs.clear()
        notifyDataSetChanged()
    }

    fun getSelectedInputs(): Set<PlanJourneyInputsEntity> = selectedInputs.toSet()

    fun hasSelectedInputs(): Boolean = selectedInputs.isNotEmpty()

    // Currency Formatting
    private fun formatCurrency(amount: Double): String {
        return try {
            currencyFormatter.format(amount)
        } catch (e: Exception) {
            "Ksh. $amount" // Fallback format
        }
    }

    // RecyclerView Required Methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInputLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(inputs[position])
    }

    override fun getItemCount() = inputs.size
}