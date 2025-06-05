package com.example.farmdatapod.produce

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.ProducerAdapter
import com.example.farmdatapod.adapter.ProducerOption
import com.example.farmdatapod.databinding.FragmentProduceLandingPageBinding
import com.google.android.material.snackbar.Snackbar

class ProduceLandingPageFragment : Fragment() {

    private var _binding: FragmentProduceLandingPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProduceLandingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.optionsRecyclerView.adapter = ProducerAdapter(getProducerOptions()) { option ->
            when (option.title) {
                "Independent Producer" -> navigateToIndependentProducer("Independent Producer")
                "CIG Producer" -> navigateToIndependentProducer("CIG Producer")
                else -> {
                    Log.e("ProduceLandingPage", "Unknown producer option: ${option.title}")
                }
            }
        }
    }

    private fun getProducerOptions(): List<ProducerOption> {
        return listOf(
            ProducerOption("Independent Producer", R.drawable.producer),
            ProducerOption("CIG Producer", R.drawable.producer)
        )
    }

    private fun navigateToIndependentProducer(producerType: String) {
        try {
            val action = ProduceLandingPageFragmentDirections
                .actionProduceLandingPageFragmentToIndependentProducerLandingPageFragment(producerType)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("ProduceLandingPage", "Error navigating to IndependentProducerLandingPageFragment", e)
            Snackbar.make(binding.root, "Error navigating to Independent Producer page", Snackbar.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}