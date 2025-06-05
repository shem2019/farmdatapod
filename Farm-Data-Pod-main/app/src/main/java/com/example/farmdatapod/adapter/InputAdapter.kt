package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemInputBinding
import com.example.farmdatapod.models.Input

class InputAdapter(
    private var inputs: MutableList<Input> = mutableListOf(),
    private val onDeleteInput: (Int) -> Unit,
    private val onInputUpdated: (Int, Input) -> Unit
) : RecyclerView.Adapter<InputAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemInputBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.deleteInputButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteInput(position)
                }
            }

            binding.inputNameEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val updatedInput = Input(
                            input = binding.inputNameEditText.text.toString(),
                            input_cost = inputs[position].input_cost
                        )
                        onInputUpdated(position, updatedInput)
                    }
                }
            }

            binding.inputCostEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val updatedInput = Input(
                            input = inputs[position].input,
                            input_cost = binding.inputCostEditText.text.toString().toDoubleOrNull() ?: 0.0
                        )
                        onInputUpdated(position, updatedInput)
                    }
                }
            }
        }

        fun bind(input: Input) {
            binding.apply {
                inputNameEditText.setText(input.input)
                inputCostEditText.setText(input.input_cost.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInputBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(inputs[position])
    }

    override fun getItemCount(): Int = inputs.size

    fun updateInputs(newInputs: List<Input>) {
        inputs.clear()
        inputs.addAll(newInputs)
        notifyDataSetChanged()
    }
}