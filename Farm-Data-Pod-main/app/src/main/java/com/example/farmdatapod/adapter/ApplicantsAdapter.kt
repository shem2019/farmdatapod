package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemApplicantBinding
import com.example.farmdatapod.models.NameOfApplicants
import java.lang.ref.WeakReference

class ApplicantsAdapter : RecyclerView.Adapter<ApplicantsAdapter.ApplicantViewHolder>() {

    private val applicants = mutableListOf<NameOfApplicants>()
    private val textWatchers = mutableMapOf<Int, List<TextWatcher>>()

    inner class ApplicantViewHolder(
        private val binding: ItemApplicantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val nameWatcher = createTextWatcher { newText ->
            updateApplicant(adapterPosition) { it.copy(name = newText) }
        }

        private val ppeWatcher = createTextWatcher { newText ->
            updateApplicant(adapterPosition) { it.copy(ppes_used = newText) }
        }

        private val equipmentWatcher = createTextWatcher { newText ->
            updateApplicant(adapterPosition) { it.copy(equipment_used = newText) }
        }

        fun bind(applicant: NameOfApplicants) {
            // Remove previous text watchers if they exist
            removeTextWatchers()

            // Set values
            binding.nameInput.setText(applicant.name)
            binding.ppeInput.setText(applicant.ppes_used)
            binding.equipmentInput.setText(applicant.equipment_used)

            // Add text watchers
            binding.nameInput.addTextChangedListener(nameWatcher)
            binding.ppeInput.addTextChangedListener(ppeWatcher)
            binding.equipmentInput.addTextChangedListener(equipmentWatcher)

            // Store text watchers for this position
            textWatchers[adapterPosition] = listOf(nameWatcher, ppeWatcher, equipmentWatcher)

            // Set up remove button
            binding.removeButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    removeApplicant(position)
                }
            }
        }

        private fun removeTextWatchers() {
            textWatchers[adapterPosition]?.forEach { watcher ->
                binding.nameInput.removeTextChangedListener(watcher)
                binding.ppeInput.removeTextChangedListener(watcher)
                binding.equipmentInput.removeTextChangedListener(watcher)
            }
            textWatchers.remove(adapterPosition)
        }

        private fun createTextWatcher(onTextChanged: (String) -> Unit): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onTextChanged(s?.toString() ?: "")
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        val binding = ItemApplicantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ApplicantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        holder.bind(applicants[position])
    }

    override fun getItemCount(): Int = applicants.size

    override fun onViewRecycled(holder: ApplicantViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            // Remove the text watchers for this position
            textWatchers.remove(position)
        }
    }

    // Public methods for managing applicants
    fun addItem(applicant: NameOfApplicants) {
        applicants.add(applicant)
        notifyItemInserted(applicants.lastIndex)
    }

    fun removeApplicant(position: Int) {
        if (position in applicants.indices) {
            applicants.removeAt(position)
            textWatchers.remove(position)
            notifyItemRemoved(position)
            // Update positions for remaining items
            for (i in position until applicants.size) {
                textWatchers[i]?.let { watchers ->
                    textWatchers.remove(i)
                    textWatchers[i + 1] = watchers
                }
            }
            notifyItemRangeChanged(position, applicants.size)
        }
    }

    private fun updateApplicant(position: Int, update: (NameOfApplicants) -> NameOfApplicants) {
        if (position in applicants.indices) {
            applicants[position] = update(applicants[position])
        }
    }

    fun getItems(): List<NameOfApplicants> = applicants.toList()

    fun clearItems() {
        val size = applicants.size
        applicants.clear()
        textWatchers.clear()
        notifyItemRangeRemoved(0, size)
    }
}