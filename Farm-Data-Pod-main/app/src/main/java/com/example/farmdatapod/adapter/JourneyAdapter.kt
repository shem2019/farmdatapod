package com.example.farmdatapod.adapter

import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemJourneyBinding
import com.example.farmdatapod.models.StopPoint
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class JourneyAdapter : RecyclerView.Adapter<JourneyAdapter.JourneyViewHolder>() {
    private val items = mutableListOf<StopPoint>()
    private var onItemRemoved: ((Int) -> Unit)? = null

    inner class JourneyViewHolder(
        private val binding: ItemJourneyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            setupRemoveButton()
            setupTimePickerListener()
        }

        private fun setupRemoveButton() {
            binding.removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    onItemRemoved?.invoke(position)
                }
            }
        }

        private fun setupTimePickerListener() {
            binding.timeInput.setOnClickListener {
                showTimePicker(binding.timeInput)
            }
        }

        private fun showTimePicker(timeInput: TextInputEditText) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                binding.root.context,
                { _, selectedHour, selectedMinute ->
                    // Create a new Calendar instance for the selected time
                    val timeCalendar = Calendar.getInstance()
                    timeCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    timeCalendar.set(Calendar.MINUTE, selectedMinute)

                    // Format for display (HH:mm)
                    val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val displayTime = displayFormat.format(timeCalendar.time)

                    // Format for API (yyyy-MM-dd'T'HH:mm:ss)
                    val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val apiTime = apiFormat.format(timeCalendar.time)

                    timeInput.setText(displayTime)

                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val item = items[adapterPosition]
                        items[adapterPosition] = item.copy(time = apiTime)
                    }
                },
                hour,
                minute,
                true
            ).show()
        }

        fun bind(item: StopPoint) {
            with(binding) {
                descriptionInput.setText(item.description)
                purposeInput.setText(item.purpose)
                stopPointInput.setText(item.stop_point)
                timeInput.setText(item.time)

                descriptionInput.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        items[adapterPosition] = items[adapterPosition].copy(description = text.toString())
                    }
                }

                purposeInput.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        items[adapterPosition] = items[adapterPosition].copy(purpose = text.toString())
                    }
                }

                stopPointInput.doOnTextChanged { text, _, _, _ ->
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        items[adapterPosition] = items[adapterPosition].copy(stop_point = text.toString())
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JourneyViewHolder {
        val binding = ItemJourneyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JourneyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JourneyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun setItems(newItems: List<StopPoint>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: StopPoint) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    fun setOnItemRemovedListener(listener: (Int) -> Unit) {
        onItemRemoved = listener
    }

    fun clearItems() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getItems(): List<StopPoint> = items.toList()
}