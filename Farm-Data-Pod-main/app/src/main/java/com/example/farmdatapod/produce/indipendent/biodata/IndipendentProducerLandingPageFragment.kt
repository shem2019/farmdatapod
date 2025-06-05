package com.example.farmdatapod.produce.indipendent.biodata

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.HomepageOption
import com.example.farmdatapod.adapter.HomepageOptionsAdapter
import androidx.navigation.fragment.navArgs
import com.example.farmdatapod.databinding.FragmentIndipendentProducerLandingPageBinding

class IndependentProducerLandingPageFragment : Fragment() {

    private var _binding: FragmentIndipendentProducerLandingPageBinding? = null
    private val binding get() = _binding!!
    private val args: IndependentProducerLandingPageFragmentArgs by navArgs()

    // Retrieve the producerType from the arguments
    private val producerType: String by lazy { args.producerType }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIndipendentProducerLandingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        // Update the TextView inside the Toolbar with the producerType
        binding.toolbar.findViewById<TextView>(R.id.toolbarTitle).text = producerType
    }

    private fun setupRecyclerView() {
        val options = listOf(
            HomepageOption("Biodata", R.drawable.user_registration),
            HomepageOption("Field Registration", R.drawable.producer)
        )

        val adapter = HomepageOptionsAdapter(options) { option ->
            when (option.title) {
                "Biodata" -> {
                    val action = IndependentProducerLandingPageFragmentDirections
                        .actionIndependentProducerLandingPageFragmentToProduceFragment(producerType)
                    Log.d("IndependentProducerLandingPage", "Navigating to ProduceFragment with producerType: $producerType")
                    findNavController().navigate(action)
                }
                "Field Registration" -> {
                    Log.d("IndependentProducerLandingPageProducerType", "Navigating to FieldRegistrationFragment with producerType: $producerType")
                    val action = IndependentProducerLandingPageFragmentDirections
                        .actionIndependentProducerLandingPageFragmentToFieldRegistrationFragment(producerType)
                    findNavController().navigate(action)
                }
                else -> {
                    Log.e("IndependentProducerLandingPageProducerType", "Unknown option title: ${option.title}")
                }
            }
        }

        binding.optionsRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.optionsRecyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(producerType: String) = IndependentProducerLandingPageFragment().apply {
            arguments = Bundle().apply {
                putString("producerType", producerType)
            }
        }
    }
}