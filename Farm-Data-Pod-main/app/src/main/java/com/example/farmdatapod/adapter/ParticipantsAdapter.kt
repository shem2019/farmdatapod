package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.google.android.material.textfield.TextInputEditText


data class Participants(
    val participantNames: List<String>
)

class ParticipantsAdapter(private val participants: List<String>) :
    RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(participants[position])
    }

    override fun getItemCount(): Int = participants.size

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val participantInput: TextInputEditText = itemView.findViewById(R.id.participant_input)

        fun bind(participantName: String) {
            participantInput.setText(participantName)
        }
    }
}