package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.HomepageOption
import com.example.farmdatapod.adapter.HomepageOptionsAdapter

class OutboundLandingPageFragment : Fragment() {
    private var backButton: ImageView? = null
    private var optionsRecyclerView: RecyclerView? = null
    private lateinit var adapter: HomepageOptionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Handle any arguments if needed
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_outbound_landing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        backButton = view.findViewById(R.id.backButton)
        optionsRecyclerView = view.findViewById(R.id.optionsRecyclerView)

        // Set up back button
        backButton?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Set up RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val options = listOf(
            HomepageOption(
                title = "Dispatch",
                iconResId = R.drawable.hub // Make sure you have this icon in your drawable resources
            ),
            HomepageOption(
                title = "Equipment Loading",
                iconResId = R.drawable.price_distribution // Make sure you have this icon
            ),
            HomepageOption(
                title = "Input Loading",
                iconResId = R.drawable.producer // Make sure you have this icon
            )
        )

        adapter = HomepageOptionsAdapter(options) { option ->
            when (option.title) {
                "Dispatch" -> findNavController().navigate(
                    R.id.action_outboundLandingPageFragment_to_dispatchFragment
                )
                "Equipment Loading" -> findNavController().navigate(
                    R.id.action_outboundLandingPageFragment_to_equipmentLoadingFragment
                )
                "Input Loading" -> findNavController().navigate(
                    R.id.action_outboundLandingPageFragment_to_inputLoadingFragment
                )
            }
        }

        optionsRecyclerView?.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up references to avoid memory leaks
        backButton = null
        optionsRecyclerView = null
    }
}