package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemForecastYieldBinding
import com.example.farmdatapod.ForecastYield

class ForecastAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    private val forecastList = mutableListOf<ForecastYield>()
    private val forecastQualityOptions = listOf("excellent", "good", "moderate", "poor", "very poor")

    inner class ForecastViewHolder(private val binding: ItemForecastYieldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(forecastYield: ForecastYield) {
            binding.apply {
                // Set data to views
                etCropPopulation.setText(forecastYield.crop_population_pc)
                etTAComment.setText(forecastYield.ta_comments)
                etYieldForecast.setText(forecastYield.yield_forecast_pc)
                actvForecastQuality.setText(forecastYield.forecast_quality)

                // Setup adapter for the forecast quality dropdown
                val adapter = ArrayAdapter(binding.root.context, android.R.layout.simple_dropdown_item_1line, forecastQualityOptions)
                actvForecastQuality.setAdapter(adapter)

                // Add TextWatchers to update the form data in real-time
                etCropPopulation.addTextChangedListener { forecastYield.crop_population_pc = it.toString() }
                etTAComment.addTextChangedListener { forecastYield.ta_comments = it.toString() }
                etYieldForecast.addTextChangedListener { forecastYield.yield_forecast_pc = it.toString() }
                actvForecastQuality.addTextChangedListener { forecastYield.forecast_quality = it.toString() }

                // Validation on focus change
                etCropPopulation.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        validateCropPopulation()
                    }
                }

                etYieldForecast.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        validateYieldForecast()
                    }
                }

                etTAComment.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        validateTAComment()
                    }
                }

                actvForecastQuality.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        validateForecastQuality()
                    }
                }
            }
        }

        private fun validateCropPopulation(): Boolean {
            val cropPopulation = binding.etCropPopulation.text.toString().toDoubleOrNull()
            return if (cropPopulation == null || cropPopulation < 0) {
                binding.etCropPopulation.error = "Invalid crop population"
                false
            } else {
                binding.etCropPopulation.error = null
                true
            }
        }

        private fun validateYieldForecast(): Boolean {
            val yieldForecast = binding.etYieldForecast.text.toString().toDoubleOrNull()
            return if (yieldForecast == null || yieldForecast < 0) {
                binding.etYieldForecast.error = "Invalid yield forecast"
                false
            } else {
                binding.etYieldForecast.error = null
                true
            }
        }

        private fun validateTAComment(): Boolean {
            val taComment = binding.etTAComment.text.toString()
            return if (taComment.isBlank()) {
                binding.etTAComment.error = "TA Comment cannot be empty"
                false
            } else {
                binding.etTAComment.error = null
                true
            }
        }

        private fun validateForecastQuality(): Boolean {
            val forecastQuality = binding.actvForecastQuality.text.toString()
            return if (forecastQuality.isBlank()) {
                binding.actvForecastQuality.error = "Forecast Quality cannot be empty"
                false
            } else {
                binding.actvForecastQuality.error = null
                true
            }
        }

        fun validateAllFields(): Boolean {
            return validateCropPopulation() &&
                    validateYieldForecast() &&
                    validateTAComment() &&
                    validateForecastQuality()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val binding = ItemForecastYieldBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(forecastList[position])
    }

    override fun getItemCount(): Int = forecastList.size

    fun addForecastYield() {
        forecastList.add(ForecastYield().apply {
            crop_population_pc = ""
            yield_forecast_pc = ""
            forecast_quality = ""
            ta_comments = ""
        })
        notifyItemInserted(forecastList.size - 1)
    }

    fun validateForms(): Boolean {
        return (0 until itemCount).all { position ->
            (recyclerView.findViewHolderForAdapterPosition(position) as? ForecastViewHolder)?.validateAllFields() == true
        }
    }

    fun getForms(): List<ForecastYield> {
        return forecastList.toList()
    }
}