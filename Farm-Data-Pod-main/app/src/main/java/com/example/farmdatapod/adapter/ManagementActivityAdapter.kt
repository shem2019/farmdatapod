package com.example.farmdatapod.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.ItemManagementActivityBinding
import com.example.farmdatapod.models.Input
import com.example.farmdatapod.models.NurseryManagementActivity
private val managementActivities = listOf(
    "Nursery Establishment",
    "Watering",
    "Feeding",
    "Spraying",
    "Weeding",
    "Hardening"
)
class ManagementActivityAdapter(
    private var activities: MutableList<NurseryManagementActivity> = mutableListOf(),
    private val onDeleteActivity: (Int) -> Unit,
    private val onActivityUpdated: (Int, NurseryManagementActivity) -> Unit
) : RecyclerView.Adapter<ManagementActivityAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "ManagementActivityAdapter"
    }

    init {
        Log.d(TAG, "Initializing ManagementActivityAdapter with ${activities.size} activities")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "Creating new ViewHolder")
        val binding = ItemManagementActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // Add this logging
        Log.d(TAG, "ViewHolder created with dimensions: width=${binding.root.width}, height=${binding.root.height}")
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "Binding ViewHolder at position $position")
        Log.d(TAG, "Activity being bound: ${activities[position]}")
        // Add visibility check
        Log.d(TAG, "ViewHolder visibility: ${holder.itemView.visibility}")
        holder.bind(activities[position])
    }

    override fun getItemCount(): Int = activities.size

    // Public methods for managing activities
    fun getActivities(): List<NurseryManagementActivity> = activities.toList()

    fun updateActivities(newActivities: List<NurseryManagementActivity>) {
        Log.d(TAG, "Updating activities list. Old size: ${activities.size}, New size: ${newActivities.size}")
        activities.clear()
        activities.addAll(newActivities)
        notifyDataSetChanged()
    }

    fun addActivity(activity: NurseryManagementActivity) {
        Log.d(TAG, "Adding new activity to adapter")
        val position = activities.size
        activities.add(activity)
        notifyItemInserted(position)
        notifyItemRangeChanged(position, activities.size)
        Log.d(TAG, "Current number of activities: ${activities.size}")
    }

    inner class ViewHolder(
        private val binding: ItemManagementActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val inputsAdapter = InputAdapter(
            inputs = mutableListOf(),
            onDeleteInput = { position ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Deleting input at position $position")
                    val activity = activities[adapterPosition]
                    (activity.input as MutableList).removeAt(position)
                    onActivityUpdated(adapterPosition, activity)
                }
            },
            onInputUpdated = { position, input ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Updating input at position $position with $input")
                    val activity = activities[adapterPosition]
                    (activity.input as MutableList)[position] = input
                    onActivityUpdated(adapterPosition, activity)
                }
            }
        )

        init {
            Log.d(TAG, "Initializing ViewHolder")
            setupRecyclerView()
            setupClickListeners()
            setupManagementActivityDropdown() // Add this line

            setupFocusListeners()
        }
        private fun setupManagementActivityDropdown() {
            val adapter = ArrayAdapter(
                binding.root.context,
                R.layout.dropdown_item,
                managementActivities
            )
            binding.managementActivityDropdown.setAdapter(adapter)
            binding.managementActivityDropdown.setText("Select", false)
        }

        private fun setupRecyclerView() {
            binding.inputsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = inputsAdapter
            }
        }

        private fun setupClickListeners() {
            // Delete Activity Button
            binding.deleteActivityButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Deleting activity at position $position")
                    onDeleteActivity(position)
                }
            }

            // Add Input Button
            binding.addInputButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Adding new input to activity at position $position")
                    val activity = activities[position]
                    (activity.input as MutableList).add(Input("", 0.0))
                    onActivityUpdated(position, activity)
                    inputsAdapter.notifyItemInserted(activity.input.size - 1)
                }
            }

            // Management Activity Selection
            binding.managementActivityDropdown.setOnItemClickListener { _, _, _, _ ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Updating management activity at position $position")
                    val activity = activities[position].copy(
                        management_activity = binding.managementActivityDropdown.text.toString()
                    )
                    onActivityUpdated(position, activity)
                }
            }
        }

        private fun setupFocusListeners() {
            // Man Days Input
            binding.manDaysInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && adapterPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Updating man days at position $adapterPosition")
                    val activity = activities[adapterPosition].copy(
                        man_days = binding.manDaysInput.text.toString()
                    )
                    onActivityUpdated(adapterPosition, activity)
                }
            }

            // Unit Cost Input
            binding.unitCostInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && adapterPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Updating unit cost at position $adapterPosition")
                    val activity = activities[adapterPosition].copy(
                        unit_cost_of_labor = binding.unitCostInput.text.toString().toDoubleOrNull() ?: 0.0
                    )
                    onActivityUpdated(adapterPosition, activity)
                }
            }

            // Frequency Input
            binding.frequencyInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && adapterPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Updating frequency at position $adapterPosition")
                    val activity = activities[adapterPosition].copy(
                        frequency = binding.frequencyInput.text.toString()
                    )
                    onActivityUpdated(adapterPosition, activity)
                }
            }
        }

        fun bind(activity: NurseryManagementActivity) {
            Log.d(TAG, "Binding position $adapterPosition with activity: $activity")
            binding.apply {
                managementActivityDropdown.setText(activity.management_activity)
                manDaysInput.setText(activity.man_days)
                unitCostInput.setText(activity.unit_cost_of_labor.toString())
                frequencyInput.setText(activity.frequency)
            }
            inputsAdapter.updateInputs(activity.input)
        }
    }
}