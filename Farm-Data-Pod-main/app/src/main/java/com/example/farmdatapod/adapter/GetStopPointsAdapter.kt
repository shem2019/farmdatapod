package com.example.farmdatapod.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.GetStopPointsBinding
import com.example.farmdatapod.models.StopPoint

class GetStopPointsAdapter(
    private val stopPointsList: List<StopPoint>
) : RecyclerView.Adapter<GetStopPointsAdapter.StopPointViewHolder>() {

    // ViewHolder class to bind the views
    inner class StopPointViewHolder(
        private val binding: GetStopPointsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Bind data to the views
        fun bind(stopPoint: StopPoint) {
            binding.apply {
                stopPointText.setText("Stop Point: ${stopPoint.stop_point}")
                timeText.setText("Time: ${stopPoint.time}")
                purposeText.setText("Purpose: ${stopPoint.purpose}")
                descriptionText.setText("Description: ${stopPoint.description}")
            }
        }
    }

    // Create the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopPointViewHolder {
        val binding = GetStopPointsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StopPointViewHolder(binding)
    }

    // Bind data to the ViewHolder
    override fun onBindViewHolder(holder: StopPointViewHolder, position: Int) {
        holder.bind(stopPointsList[position])
    }

    // Return the number of items in the list
    override fun getItemCount() = stopPointsList.size
}