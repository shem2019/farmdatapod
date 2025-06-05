package com.example.farmdatapod.userregistration.customers

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.example.farmdatapod.R
import com.example.farmdatapod.adapter.CustomersAdapter
import com.example.farmdatapod.adapter.CustomerType
import android.widget.ImageView

class CustomersLandingPageFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomersAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customers_landing_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.optionsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        val customerTypes = listOf(
            CustomerType(R.drawable.producer, "Individual Customer"),
            CustomerType(R.drawable.hub, "Organizational Customer")
        )

        adapter = CustomersAdapter(customerTypes) { customerType ->
            when (customerType.title) {
                "Individual Customer" -> navigateToIndividualCustomerFlow()
                "Organizational Customer" -> navigateToOrganizationalCustomerFlow()
            }
        }

        recyclerView.adapter = adapter

        val backButton: ImageView = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun navigateToIndividualCustomerFlow() {
        findNavController().navigate(R.id.action_customersLandingPageFragment_to_individualCustomerFragment)
    }

    private fun navigateToOrganizationalCustomerFlow() {
        findNavController().navigate(R.id.action_customersLandingPageFragment_to_organisationCustomerFragment)
    }
}