package com.example.farmdatapod.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.HomepageOption
import com.example.farmdatapod.adapter.HomepageOptionsAdapter
import com.example.farmdatapod.databinding.FragmentHomepageBinding
import com.example.farmdatapod.utils.SharedPrefs

class HomepageFragment : Fragment() {

    private var _binding: FragmentHomepageBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPrefs: SharedPrefs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomepageBinding.inflate(inflater, container, false)
        sharedPrefs = SharedPrefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupLogoutButton()
    }

    private fun setupLogoutButton() {
        binding.logoutIcon.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        // Clear all authentication data
        sharedPrefs.clearAllAuthData()

        // Create intent for LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            // Clear the back stack so user can't go back
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        // Finish the current activity
        requireActivity().finish()
    }

    private fun setupRecyclerView() {
        val options = listOf(
            HomepageOption("Hub", R.drawable.hub),
            HomepageOption("User Registration", R.drawable.user_registration),
            HomepageOption("Producer", R.drawable.producer),
            HomepageOption("Season Planning", R.drawable.season),
            HomepageOption("Crop Management", R.drawable.extension),
//            HomepageOption("Extension Services", R.drawable.extension),
            HomepageOption("Training", R.drawable.training),
            HomepageOption("Price Distribution", R.drawable.price_distribution),
            HomepageOption("Logistics", R.drawable.logistics),
//            HomepageOption("Buying", R.drawable.buying),
            HomepageOption("Selling", R.drawable.selling),
            HomepageOption("Make Payments", R.drawable.make_payments),
            HomepageOption("Receive Payments", R.drawable.receive_payments),
            HomepageOption("Rural Worker", R.drawable.rural_worker),
        )

        val adapter = HomepageOptionsAdapter(options) { option ->
            navigateBasedOnOption(option.title)
        }

        binding.optionsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            this.adapter = adapter
        }
    }

    private fun navigateBasedOnOption(optionTitle: String) {
        val navigationAction = when (optionTitle) {
            "Hub" -> R.id.action_homePageFragment_to_hubLandingPageFragment
            "User Registration" -> R.id.action_homePageFragment_to_categoryFragment
            "Producer" -> R.id.action_homePageFragment_to_produceLandingPageFragment
            "Season Planning" -> R.id.action_homePageFragment_to_seasonsLandingPageFragment
            "Crop Management" -> R.id.action_homePageFragment_to_cropManagementLandingPageFragment
//            "Extension Services" -> R.id.action_homePageFragment_to_extensionsFragment
            "Training" -> R.id.action_homePageFragment_to_trainingLandingPageFragment
            "Price Distribution" -> R.id.action_homePageFragment_to_priceDistributionLandingPageFragment
//            "Buying" -> R.id.action_homePageFragment_to_buyingInformationFragment
            "Selling" -> R.id.action_homePageFragment_to_sellingFragment
            "Make Payments" -> R.id.action_homePageFragment_to_makePaymentFragment
            "Receive Payments" -> R.id.action_homePageFragment_to_receivePaymentFragment
            "Logistics" -> R.id.action_homePageFragment_to_logisticsLandingPageFragment2
            "Rural Worker" -> R.id.action_homePageFragment_to_ruralWorkerInformationFragment
            else -> null
        }

        navigationAction?.let {
            findNavController().navigate(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}