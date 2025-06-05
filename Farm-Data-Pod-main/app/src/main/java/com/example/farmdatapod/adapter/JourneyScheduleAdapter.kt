package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemJourneyScheduleBinding
import com.example.farmdatapod.models.JourneyModel
import com.example.farmdatapod.models.StopPoint

class JourneyScheduleAdapter : RecyclerView.Adapter<JourneyScheduleAdapter.JourneyViewHolder>() {

    private var journeyList = mutableListOf<JourneyModel>()

    inner class JourneyViewHolder(private val binding: ItemJourneyScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(journey: JourneyModel) {
            binding.apply {
                // Set journey details
                driverText.setText("Driver: ${journey.driver}")
                dateTimeText.setText("Date and Time: ${journey.date_and_time}")
                truckText.setText("Truck: ${journey.truck}")
                logisticianStatusText.setText("Logistician Status: ${journey.logistician_status}")
                routeIdText.setText("Route ID: ${journey.route_id}")

                // Set up stop points RecyclerView
                val stopPointsAdapter = GetStopPointsAdapter(journey.stop_points)
                stopPointsRecyclerView.layoutManager = LinearLayoutManager(root.context)
                stopPointsRecyclerView.adapter = stopPointsAdapter

                // Optional: Set up item click listener
                root.setOnClickListener {
                    onItemClickListener?.invoke(journey)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JourneyViewHolder {
        val binding = ItemJourneyScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JourneyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JourneyViewHolder, position: Int) {
        holder.bind(journeyList[position])
    }

    override fun getItemCount(): Int = journeyList.size

    fun updateJourneySchedule(newJourneys: List<JourneyModel>) {
        journeyList.clear()
        journeyList.addAll(newJourneys)
        notifyDataSetChanged()
    }

    private var onItemClickListener: ((JourneyModel) -> Unit)? = null

    fun setOnItemClickListener(listener: (JourneyModel) -> Unit) {
        onItemClickListener = listener
    }
}