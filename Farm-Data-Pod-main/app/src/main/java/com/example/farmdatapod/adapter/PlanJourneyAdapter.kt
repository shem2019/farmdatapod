package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.PlanJourney

class PlannedJourneyAdapter()
//    private var journeyList: List<PlanJourney>,
//    private val onViewDetailsClicked: (PlanJourney) -> Unit
//) : RecyclerView.Adapter<PlannedJourneyAdapter.JourneyViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JourneyViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_planned_journey, parent, false)
//        return JourneyViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: JourneyViewHolder, position: Int) {
//        val journey = journeyList[position]
//
//        // Bind data to views
//        holder.truckTextView.text = journey.truck
//        holder.startLocationTextView.text = journey.start_location
//        holder.finalDestinationTextView.text = journey.final_destination
//
//        // Set up three-dot menu (vertical ellipsis)
//        holder.menuImageView.setOnClickListener {
//            showPopupMenu(it, journey)
//        }
//    }
//
//    override fun getItemCount(): Int = journeyList.size
//
//    fun updateJourneys(newJourneys: List<PlanJourney>) {
//        journeyList = newJourneys
//        notifyDataSetChanged()
//    }
//
//    private fun showPopupMenu(view: View, journey: PlanJourney) {
//        val popupMenu = PopupMenu(view.context, view)
//        popupMenu.inflate(R.menu.journey_popup_menu)  // Create this menu XML
//        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
//            when (menuItem.itemId) {
//                R.id.menu_view_details -> {
//                    onViewDetailsClicked(journey)
//                    true
//                }
//                else -> false
//            }
//        }
//        popupMenu.show()
//    }
//
//    class JourneyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val truckTextView: TextView = itemView.findViewById(R.id.truckTextView)
//        val startLocationTextView: TextView = itemView.findViewById(R.id.startLocationTextView)
//        val finalDestinationTextView: TextView = itemView.findViewById(R.id.finalDestinationTextView)
//        val menuImageView: ImageView = itemView.findViewById(R.id.menuImageView)
//    }

