package com.example.farmdatapod.userregistration.hq

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

class DepartmentsFragment : Fragment() {

    private lateinit var departmentsRecyclerView: RecyclerView
    private lateinit var departmentsAdapter: DepartmentsAdapter
    private lateinit var backButton: ImageView
    private val departmentsList = listOf(
        Department("HR", "Human Resources Department"),
        Department("IT", "Information Technology Department")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_departments, container, false)

        departmentsRecyclerView = view.findViewById(R.id.optionsRecyclerView)
        backButton = view.findViewById(R.id.backButton) // Ensure you have this ID in your layout

        setupRecyclerView()
        setupBackButton()

        return view
    }

    private fun setupRecyclerView() {
        departmentsRecyclerView.layoutManager = LinearLayoutManager(context)
        departmentsAdapter = DepartmentsAdapter(departmentsList) { department ->
            // Handle department click
            val action = DepartmentsFragmentDirections.actionDepartmentsFragmentToDepartmentUserRegistrationFragment()
            findNavController().navigate(action)
        }
        departmentsRecyclerView.adapter = departmentsAdapter
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}