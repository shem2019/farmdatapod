// ProcessingLandingPageFragment.kt
package com.example.farmdatapod.userregistration.processing

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.ProcessingAdapter
import com.example.farmdatapod.adapter.ProcessingPlant

class ProcessingLandingPageFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProcessingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_processing_landing_page, container, false)

        recyclerView = view.findViewById(R.id.optionsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val plants = listOf(
            ProcessingPlant("Processing Plant A", R.drawable.producer),
            ProcessingPlant("Processing Plant B", R.drawable.producer),
            ProcessingPlant("Processing Plant C", R.drawable.producer),
            ProcessingPlant("Processing Plant D", R.drawable.producer),
            ProcessingPlant("Processing Plant E", R.drawable.producer)
        )

        adapter = ProcessingAdapter(plants) { plant ->
            // Navigate to ProcessingPlantRegistrationFragment
            findNavController().navigate(R.id.action_processingLandingPageFragment_to_processingPlantRegistrationFragment)
        }
        recyclerView.adapter = adapter

        return view
    }
}