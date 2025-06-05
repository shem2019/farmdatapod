package com.example.farmdatapod.season.nursery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NurseryPlanDao {
    @Transaction
    @Query("SELECT * FROM nursery_plans ORDER BY createdAt DESC")
    fun getAllNurseryPlans(): Flow<List<NurseryPlanWithRelations>>

    @Transaction
    @Query("SELECT * FROM nursery_plans WHERE id = :planId")
    suspend fun getNurseryPlanById(planId: Long): NurseryPlanWithRelations?

    @Query("SELECT * FROM nursery_plans WHERE isUploaded = 0")
    suspend fun getUnsyncedPlans(): List<NurseryPlanWithRelations>

    @Query("SELECT * FROM nursery_plans WHERE producer = :producer AND season = :season AND crop = :crop LIMIT 1")
    suspend fun getNurseryPlanByDetails(producer: String, season: String, crop: String): NurseryPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNurseryPlan(nurseryPlan: NurseryPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManagementActivity(activity: ManagementActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInput(input: InputEntity): Long

    @Update
    suspend fun updateNurseryPlan(nurseryPlan: NurseryPlanEntity)

    @Delete
    suspend fun deleteNurseryPlan(nurseryPlan: NurseryPlanEntity)

    @Query("UPDATE nursery_plans SET isUploaded = 1 WHERE id = :planId")
    suspend fun markAsUploaded(planId: Long)

    @Query("UPDATE nursery_plans SET isUploaded = 0 WHERE id = :planId")
    suspend fun markForSync(planId: Int)

    @Transaction
    suspend fun insertFullNurseryPlan(
        nurseryPlan: NurseryPlanEntity,
        activities: List<Pair<ManagementActivityEntity, List<InputEntity>>>
    ): Long {
        val nurseryPlanId = insertNurseryPlan(nurseryPlan)

        activities.forEach { (activity, inputs) ->
            val activityWithPlanId = activity.copy(nurseryPlanId = nurseryPlanId)
            val activityId = insertManagementActivity(activityWithPlanId)

            inputs.forEach { input ->
                insertInput(input.copy(managementActivityId = activityId))
            }
        }

        return nurseryPlanId
    }

    @Query("DELETE FROM management_activities WHERE nurseryPlanId = :nurseryPlanId")
    suspend fun deleteActivitiesForNurseryPlan(nurseryPlanId: Long)

    @Query("DELETE FROM inputs WHERE managementActivityId IN (SELECT id FROM management_activities WHERE nurseryPlanId = :nurseryPlanId)")
    suspend fun deleteInputsForNurseryPlan(nurseryPlanId: Long)

    @Transaction
    suspend fun updateFullNurseryPlan(
        nurseryPlan: NurseryPlanEntity,
        activities: List<Pair<ManagementActivityEntity, List<InputEntity>>>
    ) {
        // Delete existing related data
        deleteInputsForNurseryPlan(nurseryPlan.id)
        deleteActivitiesForNurseryPlan(nurseryPlan.id)

        // Update nursery plan
        updateNurseryPlan(nurseryPlan)

        // Insert new related data
        activities.forEach { (activity, inputs) ->
            val activityWithPlanId = activity.copy(nurseryPlanId = nurseryPlan.id)
            val activityId = insertManagementActivity(activityWithPlanId)

            inputs.forEach { input ->
                insertInput(input.copy(managementActivityId = activityId))
            }
        }
    }

    @Transaction
    suspend fun deleteFullNurseryPlan(nurseryPlan: NurseryPlanEntity) {
        deleteInputsForNurseryPlan(nurseryPlan.id)
        deleteActivitiesForNurseryPlan(nurseryPlan.id)
        deleteNurseryPlan(nurseryPlan)
    }
}