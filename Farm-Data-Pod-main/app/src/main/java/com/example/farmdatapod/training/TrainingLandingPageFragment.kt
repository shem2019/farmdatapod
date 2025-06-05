package com.example.farmdatapod.training

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.TrainingAdapter
import com.example.farmdatapod.adapter.TrainingOption

class TrainingLandingPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_training_landing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.optionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val trainingOptions = listOf(
            TrainingOption("Training", R.drawable.training, R.id.action_trainingLandingPageFragment_to_planTrainingFragment),
            TrainingOption("View Trainings", R.drawable.buying, R.id.action_trainingLandingPageFragment_to_viewTrainingFragment)
        )

        recyclerView.adapter = TrainingAdapter(trainingOptions) { destinationId ->
            findNavController().navigate(destinationId)
        }
    }
}