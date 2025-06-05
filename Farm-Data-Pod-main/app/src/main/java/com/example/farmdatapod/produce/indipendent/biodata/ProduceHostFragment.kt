package com.example.farmdatapod.produce.indipendent.biodata

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.farmdatapod.R
import com.example.farmdatapod.databinding.FragmentProduceHostBinding
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.produce.indipendent.biodata.BasicInformationFragment
import com.example.farmdatapod.utils.NetworkUtils

class ProduceFragment : Fragment() {

    private var _binding: FragmentProduceHostBinding? = null
    private val binding get() = _binding!!
    private lateinit var networkViewModel: NetworkViewModel
    private val args: IndependentProducerLandingPageFragmentArgs by navArgs()

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val fragments = listOf(
        ProducerBioDataFragment(),
        BasicInformationFragment(),
        BaselineInformationFragment(),
        LabourAndChallengesFragment(),
        LivestockInformationFragment(),
        InfrastructureInformationFragment(),
        ProduceInformationFragment()
    )

    private var currentFragmentIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProduceHostBinding.inflate(inflater, container, false)
        networkViewModel = ViewModelProvider(this).get(NetworkViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repository
        sharedViewModel.initRepository(requireContext())

        if (savedInstanceState == null) {
            showFragment(0)
        }

        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        binding.previousButton.setOnClickListener {
            if (currentFragmentIndex > 0) {
                showFragment(currentFragmentIndex - 1)
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        binding.nextButton.setOnClickListener {
            if (currentFragmentIndex < fragments.lastIndex) {
                if (validateCurrentFragment()) {
                    // handleSubmit()
                    saveCurrentFragmentData()
                    showFragment(currentFragmentIndex + 1)
                }
            } else {
                binding.submitButton.visibility = View.VISIBLE
            }
        }

        binding.submitButton.setOnClickListener {
            if (currentFragmentIndex == fragments.lastIndex && validateCurrentFragment()) {
                // Save data for the last fragment and then handle submit
                saveCurrentFragmentData()
                handleSubmit()
            }
        }

        updateButtonVisibility()
    }

    private fun showFragment(index: Int) {
        childFragmentManager.commit {
            replace(R.id.produce_fragment_container, fragments[index])
        }
        currentFragmentIndex = index
        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        binding.previousButton.visibility = if (currentFragmentIndex > 0) View.VISIBLE else View.GONE
        binding.nextButton.visibility = if (currentFragmentIndex == fragments.lastIndex) View.GONE else View.VISIBLE
        binding.submitButton.visibility = if (currentFragmentIndex == fragments.lastIndex) View.VISIBLE else View.GONE
    }

    private fun validateCurrentFragment(): Boolean {
        val fragment = fragments[currentFragmentIndex]
        return when (fragment) {
            is ProducerBioDataFragment -> fragment.validateForm()
            is BasicInformationFragment -> fragment.validateForm()
            is BaselineInformationFragment -> fragment.validateForm()
            is LabourAndChallengesFragment -> fragment.validateForm()
            is LivestockInformationFragment -> fragment.validateForm()
            is InfrastructureInformationFragment -> fragment.validateForm()
            is ProduceInformationFragment -> fragment.validateForm()
            else -> true
        }
    }

    private fun saveCurrentFragmentData() {
        val fragment = fragments[currentFragmentIndex]
        when (fragment) {
            is ProducerBioDataFragment -> fragment.saveData()
            is BasicInformationFragment -> fragment.saveData()
            is BaselineInformationFragment -> fragment.saveData()
            is LabourAndChallengesFragment -> fragment.saveData()
            is LivestockInformationFragment -> fragment.saveData()
            is InfrastructureInformationFragment -> fragment.saveData()
            is ProduceInformationFragment -> fragment.saveData()
        }
    }

    private fun handleSubmit() {
        val producerType = args.producerType

        // Initialize repository first
        sharedViewModel.initRepository(requireContext())

        // Save data locally first
        sharedViewModel.submitProducerData(requireContext(), producerType)

        // Check network status and sync data if online
        val isNetworkAvailable = NetworkUtils.isNetworkAvailable(requireContext())
        if (isNetworkAvailable) {
            sharedViewModel.syncData(requireContext())
        } else {
            Toast.makeText(requireContext(), "No network connection. Data will be synced when network is available.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}