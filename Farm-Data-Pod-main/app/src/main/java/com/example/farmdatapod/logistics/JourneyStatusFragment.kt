package com.example.farmdatapod.logistics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.JourneyScheduleAdapter
import com.example.farmdatapod.databinding.FragmentJourneyStatusBinding
import com.example.farmdatapod.logistics.planJourney.data.JourneyViewModel
import com.example.farmdatapod.logistics.planJourney.data.JourneyWithStopPoints
import com.example.farmdatapod.models.JourneyModel
import com.example.farmdatapod.models.StopPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JourneyStatusFragment : Fragment() {
    private var _binding: FragmentJourneyStatusBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<JourneyViewModel> {
        JourneyViewModel.factory(requireActivity().application)
    }
    private lateinit var journeyAdapter: JourneyScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJourneyStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupBackButton()
        observeViewModel()
    }

    private fun setupBackButton() {
        binding.toolbar.findViewById<View>(R.id.backButton)?.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        journeyAdapter = JourneyScheduleAdapter()
        binding.journeysRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = journeyAdapter
        }

        journeyAdapter.setOnItemClickListener { journey ->
            // Handle journey item click
            // You can pass the entire JourneyModel to the next screen if needed
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.green)
            setProgressBackgroundColorSchemeResource(android.R.color.white)
            setOnRefreshListener {
                performSync()
            }
        }

        // Observe sync state changes
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncState.collectLatest { state ->
                    withContext(Dispatchers.Main) {
                        updateUiForSyncState(state)
                    }
                }
            }
        }
    }

    private fun performSync() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.syncWithServer()
            } catch (e: Exception) {
                // Always ensure the refresh indicator is stopped on error
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            journeysRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun formatDateTime(dateTime: String): String {
        // Add date formatting logic here
        return dateTime
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Using observe since journeys is LiveData
                viewModel.journeys.observe(viewLifecycleOwner) { journeysWithStops ->
                    updateJourneysList(journeysWithStops)
                }
            }
        }
    }

    private fun updateJourneysList(journeysWithStops: List<JourneyWithStopPoints>) {
        Log.d("JourneyStatusFragment", "Received journeys count: ${journeysWithStops.size}")

        for (journeyWithStops in journeysWithStops) {
            Log.d("JourneyStatusFragment", "Journey details:")
            Log.d("JourneyStatusFragment", "ID: ${journeyWithStops.journey.id}")
            Log.d("JourneyStatusFragment", "Driver: ${journeyWithStops.journey.driver}")
            Log.d("JourneyStatusFragment", "Date and Time: ${journeyWithStops.journey.date_and_time}")
            Log.d("JourneyStatusFragment", "Truck: ${journeyWithStops.journey.truck}")
            Log.d("JourneyStatusFragment", "Stop Points Count: ${journeyWithStops.stopPoints.size}")
        }

        val journeys = journeysWithStops.map { journeyWithStops ->
            JourneyModel(
                id = journeyWithStops.journey.id,
                date_and_time = formatDateTime(journeyWithStops.journey.date_and_time),
                driver = journeyWithStops.journey.driver,
                logistician_status = journeyWithStops.journey.logistician_status,
                route_id = journeyWithStops.journey.route_id,
                truck = journeyWithStops.journey.truck,
                user_id = journeyWithStops.journey.user_id,
                stop_points = journeyWithStops.stopPoints.map { stopPointEntity ->
                    StopPoint(
                        description = stopPointEntity.description,
                        purpose = stopPointEntity.purpose,
                        stop_point = stopPointEntity.stop_point,
                        time = stopPointEntity.time
                    )
                }
            )
        }

        Log.d("JourneyStatusFragment", "Converted journeys count: ${journeys.size}")

        journeyAdapter.updateJourneySchedule(journeys)
        updateEmptyState(journeys.isEmpty())
    }

    private fun updateUiForSyncState(state: JourneyViewModel.SyncState) {
        when (state) {
            is JourneyViewModel.SyncState.Syncing -> {
                // Show swipe refresh loading
                binding.swipeRefreshLayout.isRefreshing = true
            }

            is JourneyViewModel.SyncState.Success -> {
                // Hide swipe refresh loading
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }

            is JourneyViewModel.SyncState.Error -> {
                // Hide swipe refresh loading
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
            }

            is JourneyViewModel.SyncState.Idle -> {
                // Hide swipe refresh loading
                binding.swipeRefreshLayout.isRefreshing = false
            }

            else -> {
                // Hide swipe refresh loading
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}