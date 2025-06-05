package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.farmdatapod.ExtScoutingStation

class AddExtScoutingAdapter : RecyclerView.Adapter<AddExtScoutingAdapter.FormViewHolder>() {

    private val forms: MutableList<ExtScoutingStation> = mutableListOf()

    inner class FormViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val baitStationInput: TextInputEditText = itemView.findViewById(R.id.etBaitStation)
        val pestDiseaseInput: AutoCompleteTextView = itemView.findViewById(R.id.actvSelectPest)
        val scoutingMethodInput: TextInputEditText = itemView.findViewById(R.id.etScoutingMethod)
        val managementInput: TextInputEditText = itemView.findViewById(R.id.etManagement)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ext_scouting_form, parent, false)
        return FormViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormViewHolder, position: Int) {
        val form = forms[position]

        holder.baitStationInput.setText(form.bait_station)
        holder.pestDiseaseInput.setText(form.pest_or_disease)
        holder.scoutingMethodInput.setText(form.scouting_method)
        holder.managementInput.setText(form.management)

        // Set up pest/disease dropdown
        val pestDiseases = arrayOf("Aphids", "Fungal Disease", "Weeds", "Other")
        val pestAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_dropdown_item_1line, pestDiseases)
        holder.pestDiseaseInput.setAdapter(pestAdapter)

        // Add TextWatchers to update the form data in real-time
        holder.baitStationInput.addTextChangedListener { form.bait_station = it.toString() }
        holder.pestDiseaseInput.addTextChangedListener { form.pest_or_disease = it.toString() }
        holder.scoutingMethodInput.addTextChangedListener { form.scouting_method = it.toString() }
        holder.managementInput.addTextChangedListener { form.management = it.toString() }

        // Validate fields on focus change
        val validateField = { input: TextInputEditText, layout: TextInputLayout ->
            input.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    if (input.text.isNullOrBlank()) {
                        layout.error = "This field is required"
                    } else {
                        layout.error = null
                    }
                }
            }
        }

        validateField(holder.baitStationInput, holder.itemView.findViewById(R.id.tilBaitStation))
        validateField(holder.scoutingMethodInput, holder.itemView.findViewById(R.id.tilScoutingMethod))
        validateField(holder.managementInput, holder.itemView.findViewById(R.id.tilManagement))
    }

    override fun getItemCount(): Int = forms.size

    fun addForm() {
        forms.add(ExtScoutingStation())
        notifyItemInserted(forms.size - 1)
    }

    fun validateForms(): Boolean {
        var isValid = true
        for (form in forms) {
            if (form.bait_station.isNullOrBlank() || form.pest_or_disease.isNullOrBlank() ||
                form.scouting_method.isNullOrBlank() || form.management.isNullOrBlank()) {
                isValid = false
                break
            }
        }
        return isValid
    }

    fun getForms(): List<ExtScoutingStation> {
        return forms
    }
}