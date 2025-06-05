package com.example.farmdatapod.adapter

import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.PlanNutrition
import com.example.farmdatapod.R
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class NutritionPlanAdapter(private val nutritionPlans: MutableList<PlanNutrition>) :
    RecyclerView.Adapter<NutritionPlanAdapter.ViewHolder>() {

    private val productOptions = arrayOf("compost", "manure", "fertiliser", "plant tea", "vermi compost", "organic folia", "convectional foliage")
    private val methodOptions = arrayOf("basal", "foliage", "drenching")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productAutoComplete: AutoCompleteTextView = view.findViewById(R.id.productAutoComplete)
        val productNameEditText: TextInputEditText = view.findViewById(R.id.productNameEditText)
        val unitEditText: TextInputEditText = view.findViewById(R.id.unitEditText)
        val applicationRateEditText: TextInputEditText = view.findViewById(R.id.applicationRateEditText)
        val costPerUnitEditText: TextInputEditText = view.findViewById(R.id.costPerUnitEditText)
        val timeOfApplicationEditText: TextInputEditText = view.findViewById(R.id.timeOfApplicationEditText)
        val methodOfApplicationAutoComplete: AutoCompleteTextView = view.findViewById(R.id.methodOfApplicationAutoComplete)
        val productFormulationEditText: TextInputEditText = view.findViewById(R.id.productFormulationEditText)
        val dateOfApplicationEditText: TextInputEditText = view.findViewById(R.id.dateOfApplicationEditText)
        val mixingRatioEditText: TextInputEditText = view.findViewById(R.id.mixingRatioEditText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nutrition_plan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = nutritionPlans.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plan = nutritionPlans[position]

        holder.apply {
            // Set up ArrayAdapter for productAutoComplete
            val productAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_dropdown_item_1line, productOptions)
            productAutoComplete.setAdapter(productAdapter)
            productAutoComplete.setText(plan.product, false)

            // Set up ArrayAdapter for methodOfApplicationAutoComplete
            val methodAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_dropdown_item_1line, methodOptions)
            methodOfApplicationAutoComplete.setAdapter(methodAdapter)
            methodOfApplicationAutoComplete.setText(plan.method_of_application, false)

            productNameEditText.setText(plan.product_name)
            unitEditText.setText(plan.unit)
            applicationRateEditText.setText(plan.application_rate)
            costPerUnitEditText.setText(plan.cost_per_unit)
            timeOfApplicationEditText.setText(plan.time_of_application)
            productFormulationEditText.setText(plan.product_formulation)
            dateOfApplicationEditText.setText(plan.date_of_application)
            mixingRatioEditText.setText(plan.total_mixing_ratio)

            productAutoComplete.setOnItemClickListener { _, _, _, _ ->
                plan.product = productAutoComplete.text.toString()
            }

            methodOfApplicationAutoComplete.setOnItemClickListener { _, _, _, _ ->
                plan.method_of_application = methodOfApplicationAutoComplete.text.toString()
            }

            setupTextWatcher(productNameEditText) { nutritionPlans[position].product_name = it }
            setupTextWatcher(unitEditText) { nutritionPlans[position].unit = it }
            setupTextWatcher(applicationRateEditText) { nutritionPlans[position].application_rate = it }
            setupTextWatcher(costPerUnitEditText) { nutritionPlans[position].cost_per_unit = it }
            setupTextWatcher(timeOfApplicationEditText) { nutritionPlans[position].time_of_application = it }
            setupTextWatcher(productFormulationEditText) { nutritionPlans[position].product_formulation = it }
            setupTextWatcher(dateOfApplicationEditText) { nutritionPlans[position].date_of_application = it }
            setupTextWatcher(mixingRatioEditText) { nutritionPlans[position].total_mixing_ratio = it }

            dateOfApplicationEditText.setOnClickListener {
                showDatePicker(holder.itemView.context, dateOfApplicationEditText, plan)
            }
        }
    }

    private fun setupTextWatcher(editText: TextInputEditText, onTextChanged: (String) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s.toString())
            }
        })
    }

    private fun showDatePicker(context: Context, editText: TextInputEditText, plan: PlanNutrition) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)
                editText.setText(selectedDate)
                plan.date_of_application = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun getPlanNutritions(): List<PlanNutrition> {
        println("NutritionPlanAdapter: Getting ${nutritionPlans.size} nutrition plans")
        return nutritionPlans.toList()
    }

    fun areAllFieldsValid(): Boolean {
        val isValid = nutritionPlans.all { plan ->
            !plan.product.isNullOrBlank() &&
                    !plan.product_name.isNullOrBlank() &&
                    !plan.unit.isNullOrBlank() &&
                    !plan.application_rate.isNullOrBlank() &&
                    !plan.cost_per_unit.isNullOrBlank() &&
                    !plan.time_of_application.isNullOrBlank() &&
                    !plan.method_of_application.isNullOrBlank() &&
                    !plan.product_formulation.isNullOrBlank() &&
                    !plan.date_of_application.isNullOrBlank() &&
                    !plan.total_mixing_ratio.isNullOrBlank()
        }
        println("NutritionPlanAdapter: All fields valid: $isValid")
        return isValid
    }

    fun addPlanNutrition(plan: PlanNutrition) {
        nutritionPlans.add(plan)
        println("NutritionPlanAdapter: Added new plan. Total plans: ${nutritionPlans.size}")
        notifyItemInserted(nutritionPlans.size - 1)
    }
}