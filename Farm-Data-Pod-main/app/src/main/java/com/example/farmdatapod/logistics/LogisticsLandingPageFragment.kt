package com.example.farmdatapod.logistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.LogisticsLandingPageAdapter

class LogisticsLandingPageFragment : Fragment() {

    private val options = listOf(
        "Create Route",
        "Equipment Allocation",
        "Input Allocation",
        "Input Transfer",
        "Journey Status",
        "Plan Journey",
        "Inbound Logistics",
        "Outbound Logistics"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logistics_landing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val optionsRecyclerView = view.findViewById<RecyclerView>(R.id.optionsRecyclerView)
        optionsRecyclerView.layoutManager = GridLayoutManager(context, 2)
        optionsRecyclerView.adapter = LogisticsLandingPageAdapter(options) { option ->
            handleOptionClick(option)
        }
    }

    private fun handleOptionClick(option: String) {
        when (option) {
            "Create Route" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToCreateRouteFragment()
                )
            }
            "Equipment Allocation" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToEquipmentAllocationFragment()
                )
            }
            "Input Allocation" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToInputAllocationFragment()
                )
            }
            "Input Transfer" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToInputTransferFragment()
                )
            }
            "Journey Status" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToJourneyStatusFragment()
                )
            }
            "Plan Journey" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToPlanJourneyFragment()
                )
            }
            "Inbound Logistics" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToInboundLandingPageFragment()
               )
            }
            "Outbound Logistics" -> {
                findNavController().navigate(
                    LogisticsLandingPageFragmentDirections.actionLogisticsLandingPageFragment2ToOutboundLandingPageFragment()
                )
            }
        }
    }
}