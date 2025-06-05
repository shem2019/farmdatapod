package com.example.farmdatapod.userregistration

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.CategoryOption
import com.example.farmdatapod.adapter.CategoryOptionsAdapter

class CategoryFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // If you need to handle any arguments, do it here
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        val options = listOf(
            CategoryOption("HQ", R.drawable.buying),
            CategoryOption("Hub Users", R.drawable.user_registration),
            CategoryOption("Logistics", R.drawable.logistics),
            CategoryOption("Customers", R.drawable.producer),
            CategoryOption("Processing", R.drawable.price_distribution),
            CategoryOption("Custom", R.drawable.hub)  // Added the new "Custom" category
        )

        val adapter = CategoryOptionsAdapter(options) { option ->
            // Handle option click
            when (option.title) {
                "HQ" -> findNavController().navigate(R.id.action_categoryFragment_to_departmentsFragment)
                "Hub Users" -> findNavController().navigate(R.id.action_categoryFragment_to_hubUserRegistrationFragment)
                "Logistics" -> findNavController().navigate(R.id.action_categoryFragment_to_logisticsLandingPageFragment)
                "Customers" -> findNavController().navigate(R.id.action_categoryFragment_to_customersLandingPageFragment)
                "Processing" -> findNavController().navigate(R.id.action_categoryFragment_to_processingLandingPageFragment)
                "Custom" -> findNavController().navigate(R.id.action_categoryFragment_to_customUsersFragment)
            }
        }

        val recyclerView: RecyclerView = view.findViewById(R.id.optionsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = adapter

        val backButton: ImageView = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        return view
    }
}