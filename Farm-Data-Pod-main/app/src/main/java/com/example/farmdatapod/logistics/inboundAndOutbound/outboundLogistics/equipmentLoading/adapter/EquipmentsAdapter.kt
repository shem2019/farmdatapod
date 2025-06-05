package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemEquipmentBinding
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingState

class EquipmentsAdapter : ListAdapter<EquipmentLoadingState, EquipmentsAdapter.EquipmentViewHolder>(EQUIPMENT_COMPARATOR) {

    interface EquipmentInteractionListener {
        fun onEquipmentSelected(equipmentState: EquipmentLoadingState)
        fun onQuantityChanged(equipmentState: EquipmentLoadingState, newQuantity: Int)
        fun onAuthorizationChanged(equipmentState: EquipmentLoadingState, isAuthorized: Boolean)
    }

    private var listener: EquipmentInteractionListener? = null
    private var selectedEquipment: EquipmentLoadingState? = null

    fun setListener(listener: EquipmentInteractionListener) {
        this.listener = listener
    }

    fun getSelectedEquipment() = selectedEquipment

    fun clearSelection() {
        val previous = selectedEquipment
        selectedEquipment = null
        previous?.let { notifyItemChanged(currentList.indexOf(it)) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentViewHolder {
        return EquipmentViewHolder(
            ItemEquipmentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EquipmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EquipmentViewHolder(
        private val binding: ItemEquipmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: EquipmentLoadingState) {
            binding.apply {
                // Basic Equipment Info
                equipmentNameText.text = state.equipment.equipment
                deliveryNoteText.text = "DN: ${state.equipment.dn_number}"

                // Quantity Section
                quantityAvailableText.text = state.equipment.number_of_units.toString()
                setupQuantityInput(state)

                // Authorization Section
                setupAuthorizationChip(state)

                // Journey Info
                journeyText.text = "Journey ID: ${state.equipment.journey_id}"
                stopPointText.text = "Stop Point ID: ${state.equipment.stop_point_id}"

                // Selection State
                setupSelectionState(state)
            }
        }

        private fun setupQuantityInput(state: EquipmentLoadingState) {
            binding.quantityLoadedInput.apply {
                if (text.toString() != state.quantityToLoad.toString()) {
                    setText(state.quantityToLoad.toString())
                }

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        handleQuantityInput(state)
                        true
                    } else false
                }
            }
        }

        private fun handleQuantityInput(state: EquipmentLoadingState) {
            val quantity = binding.quantityLoadedInput.text.toString().toIntOrNull() ?: 0
            if (quantity <= state.equipment.number_of_units) {
                listener?.onQuantityChanged(state, quantity)
            } else {
                binding.quantityLoadedInput.error = "Cannot exceed available quantity"
            }
        }

        private fun setupAuthorizationChip(state: EquipmentLoadingState) {
            binding.authStatusChip.apply {
                isChecked = state.isAuthorized
                text = if (state.isAuthorized) "Authorized" else "Pending"
                setOnCheckedChangeListener { _, isChecked ->
                    listener?.onAuthorizationChanged(state, isChecked)
                }
            }
        }

        private fun setupSelectionState(state: EquipmentLoadingState) {
            val isSelected = state == selectedEquipment
            binding.apply {
                root.isChecked = isSelected
                selectButton.text = if (isSelected) "Selected" else "Select"

                selectButton.setOnClickListener { handleSelection(state) }
                root.setOnClickListener { handleSelection(state) }
            }
        }

        private fun handleSelection(state: EquipmentLoadingState) {
            val previous = selectedEquipment
            selectedEquipment = if (selectedEquipment == state) null else state

            previous?.let { notifyItemChanged(currentList.indexOf(it)) }
            notifyItemChanged(currentList.indexOf(state))
            listener?.onEquipmentSelected(state)
        }
    }

    companion object {
        private val EQUIPMENT_COMPARATOR = object : DiffUtil.ItemCallback<EquipmentLoadingState>() {
            override fun areItemsTheSame(oldItem: EquipmentLoadingState, newItem: EquipmentLoadingState): Boolean {
                return oldItem.equipment.id == newItem.equipment.id
            }

            override fun areContentsTheSame(oldItem: EquipmentLoadingState, newItem: EquipmentLoadingState): Boolean {
                return oldItem == newItem
            }
        }
    }
}