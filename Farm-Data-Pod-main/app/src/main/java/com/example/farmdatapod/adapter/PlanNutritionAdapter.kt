// PlanNutritionAdapter.kt
package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R
import com.google.android.material.textfield.TextInputLayout

class PlanNutritionAdapter(private val items: List<NutritionPlanItem>) : RecyclerView.Adapter<PlanNutritionAdapter.ViewHolder>() {

    private val validMethodsOfApplication = listOf("Basal", "Foliage", "Drenching")
    private val validProducts = listOf("Compost Manure Fertiliser", "Plant Tea", "Vermi Compost", "Organic Folia", "Convectional Foliage")

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productInputLayout: TextInputLayout = view.findViewById(R.id.productInputLayout)
        val productAutoComplete: AutoCompleteTextView = view.findViewById(R.id.productAutoComplete)
        val methodOfApplicationInputLayout: TextInputLayout = view.findViewById(R.id.methodOfApplicationInputLayout)
        val methodOfApplicationAutoComplete: AutoCompleteTextView = view.findViewById(R.id.methodOfApplicationAutoComplete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nutrition_plan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Set up product dropdown
        val productAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_dropdown_item_1line, validProducts)
        holder.productAutoComplete.setAdapter(productAdapter)
        holder.productAutoComplete.setText(item.product, false)

        // Set up method of application dropdown
        val methodAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_dropdown_item_1line, validMethodsOfApplication)
        holder.methodOfApplicationAutoComplete.setAdapter(methodAdapter)
        holder.methodOfApplicationAutoComplete.setText(item.methodOfApplication, false)

        // Validate product
        if (!validProducts.contains(item.product)) {
            holder.productInputLayout.error = "Invalid product"
        } else {
            holder.productInputLayout.error = null
        }

        // Validate method of application
        if (!validMethodsOfApplication.contains(item.methodOfApplication)) {
            holder.methodOfApplicationInputLayout.error = "Invalid method of application"
        } else {
            holder.methodOfApplicationInputLayout.error = null
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun areAllFieldsValid(): Boolean {
        for (item in items) {
            if (!validProducts.contains(item.product) || !validMethodsOfApplication.contains(item.methodOfApplication)) {
                return false
            }
        }
        return true
    }
}

data class NutritionPlanItem(
    val product: String,
    val methodOfApplication: String
)