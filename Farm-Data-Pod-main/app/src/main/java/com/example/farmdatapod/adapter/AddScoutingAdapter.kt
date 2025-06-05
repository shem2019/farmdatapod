package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemScoutingFormBinding
import com.example.farmdatapod.dbmodels.ScoutingStation // Corrected import

class AddScoutingAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<AddScoutingAdapter.ScoutingViewHolder>() {

    private val forms = mutableListOf<ScoutingStation>()
    private val baitOptions = listOf("Bait 1", "Bait 2", "Bait 3", "Bait 4")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoutingViewHolder {
        val binding = ItemScoutingFormBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScoutingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScoutingViewHolder, position: Int) {
        holder.bind(forms[position])
    }

    override fun getItemCount(): Int {
        return forms.size
    }

    fun addForm() {
        forms.add(ScoutingStation().apply {
            bait_station = ""
            type_of_bait_provided = ""
            frequency = ""
        })
        notifyItemInserted(forms.size - 1)
    }

    fun getForms(): List<ScoutingStation> {
        return forms
    }

    fun validateForms(): Boolean {
        return forms.all { station ->
            !station.type_of_bait_provided.isNullOrBlank() &&
                    !station.frequency.isNullOrBlank() &&
                    !station.bait_station.isNullOrBlank()
        }
    }

    inner class ScoutingViewHolder(private val binding: ItemScoutingFormBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(scoutingStation: ScoutingStation) {
            val context = binding.root.context
            val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, baitOptions)
            binding.baitTypeAutoComplete.setAdapter(adapter)

            binding.baitTypeAutoComplete.setText(scoutingStation.type_of_bait_provided, false)
            binding.frequencyEditText.setText(scoutingStation.frequency)
            binding.baitStationEditText.setText(scoutingStation.bait_station)

            // Add listeners for real-time updates
            binding.baitTypeAutoComplete.setOnItemClickListener { _, _, position, _ ->
                scoutingStation.type_of_bait_provided = baitOptions[position]
            }

            binding.frequencyEditText.addTextChangedListener {
                scoutingStation.frequency = it.toString()
            }

            binding.baitStationEditText.addTextChangedListener {
                scoutingStation.bait_station = it.toString()
            }
        }
    }
}