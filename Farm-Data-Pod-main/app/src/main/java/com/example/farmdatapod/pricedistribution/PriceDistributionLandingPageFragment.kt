package com.example.farmdatapod.pricedistribution

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.PriceDistributionAdapter

class PriceDistributionLandingPageFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PriceDistributionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_price_distribution_landing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.optionsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        adapter = PriceDistributionAdapter { option ->
            onOptionSelected(option)
        }
        recyclerView.adapter = adapter
    }

    private fun onOptionSelected(option: String) {
        when (option) {
            "Farmer" -> {
                findNavController().navigate(R.id.action_priceDistributionLandingPageFragment_to_farmerPriceDistributionFragment)
                // Navigate to Farmer screen or show Farmer-specific information
                Toast.makeText(context, "Farmer option selected", Toast.LENGTH_SHORT).show()
            }
            "Customer" -> {
                findNavController().navigate(R.id.action_priceDistributionLandingPageFragment_to_customerPriceDistributionFragment)
                // Navigate to Customer screen or show Customer-specific information
                Toast.makeText(context, "Customer option selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}