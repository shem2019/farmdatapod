package com.example.farmdatapod.cropmanagement.nursery.data


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.models.NurseryManagementActivity
import com.example.farmdatapod.season.nursery.data.InputEntity
import com.example.farmdatapod.season.nursery.data.ManagementActivityEntity
import com.example.farmdatapod.season.nursery.data.NurseryPlanEntity
import kotlinx.coroutines.launch
import java.util.Date

class NurseryManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NurseryManagementRepository(application)

    fun saveNurseryPlan(
        producer: String,
        season: String,
        dateString: String, // Single date or date range string
        cropCycle: Int,
        crop: String,
        variety: String,
        seedBatch: String,
        trayType: String,
        numberOfTrays: Int,
        seasonPlanningId: Long,
        comments: String?,
        managementActivities: List<NurseryManagementActivity>,
        isOnline: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val nurseryPlan = NurseryPlanEntity(
                    producer = producer,
                    season = season,
                    dateOfEstablishment = dateString, // This will be either single date or date range
                    cropCycleWeeks = cropCycle,
                    crop = crop,
                    variety = variety,
                    seedBatchNumber = seedBatch,
                    typeOfTrays = trayType,
                    numberOfTrays = numberOfTrays,
                    comments = comments,
                    seasonPlanningId = seasonPlanningId,
                    createdAt = Date(),
                    isUploaded = false
                )

                val activities = managementActivities.map { activity ->
                    val managementActivity = ManagementActivityEntity(
                        managementActivity = activity.management_activity,
                        frequency = activity.frequency,
                        manDays = activity.man_days,
                        unitCostOfLabor = activity.unit_cost_of_labor,
                        nurseryPlanId = 0
                    )

                    val inputs = activity.input.map { input ->
                        InputEntity(
                            input = input.input,
                            inputCost = input.input_cost,
                            managementActivityId = 0
                        )
                    }

                    Pair(managementActivity, inputs)
                }

                repository.saveNurseryPlan(nurseryPlan, activities, isOnline)
                    .onSuccess {
                        onSuccess()
                    }
                    .onFailure { error ->
                        onError(error.message ?: "Unknown error occurred")
                    }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }
}