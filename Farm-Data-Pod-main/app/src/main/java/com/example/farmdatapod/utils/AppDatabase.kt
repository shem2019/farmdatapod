package com.example.farmdatapod.utils

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.farmdatapod.hub.hubAggregation.buyingCenter.data.BuyingCenterDao
import com.example.farmdatapod.hub.hubAggregation.buyingCenter.data.BuyingCenterEntity
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIG
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIGDAO
import com.example.farmdatapod.hub.hubRegistration.data.Hub
import com.example.farmdatapod.hub.hubRegistration.data.HubDao
import com.example.farmdatapod.logistics.createRoute.data.RouteDao
import com.example.farmdatapod.logistics.createRoute.data.RouteEntity
import com.example.farmdatapod.logistics.createRoute.data.StopPointConverter
import com.example.farmdatapod.logistics.equipments.data.EquipmentDao
import com.example.farmdatapod.logistics.equipments.data.EquipmentEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.data.InboundLoadingDao
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.data.InboundLoadingEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.data.InboundOffloadingDao
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.data.InboundOffloadingEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data.DispatchDao
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data.DispatchEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingDao
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data.EquipmentLoadingEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data.LoadingInputDao
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data.LoadingInputEntity
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.models.LoadingInputModel
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsDao
import com.example.farmdatapod.logistics.inputAllocation.data.PlanJourneyInputsEntity
import com.example.farmdatapod.logistics.inputTransfer.data.InputTransferDao
import com.example.farmdatapod.logistics.inputTransfer.data.InputTransferEntity
import com.example.farmdatapod.logistics.planJourney.data.JourneyDao
import com.example.farmdatapod.logistics.planJourney.data.JourneyEntity
import com.example.farmdatapod.logistics.planJourney.data.StopPointEntity
import com.example.farmdatapod.produce.data.ProducerConverters
import com.example.farmdatapod.produce.data.ProducerDao
import com.example.farmdatapod.produce.data.ProducerEntity
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.CropEntity
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationDao
import com.example.farmdatapod.produce.indipendent.fieldregistration.data.FieldRegistrationEntity
import com.example.farmdatapod.season.cropManagement.data.CropManagementDao
import com.example.farmdatapod.season.cropManagement.data.CropManagementEntity
import com.example.farmdatapod.season.cropManagement.data.GappingActivityEntity
import com.example.farmdatapod.season.cropManagement.data.PruningActivityEntity
import com.example.farmdatapod.season.cropManagement.data.StakingActivityEntity
import com.example.farmdatapod.season.cropManagement.data.ThinningActivityEntity
import com.example.farmdatapod.season.cropManagement.data.WateringActivityEntity
import com.example.farmdatapod.season.cropManagement.data.WeedingActivityEntity
import com.example.farmdatapod.season.cropProtection.data.CropProtectionApplicantEntity
import com.example.farmdatapod.season.cropProtection.data.CropProtectionDao
import com.example.farmdatapod.season.cropProtection.data.CropProtectionEntity
import com.example.farmdatapod.season.forecastYields.data.YieldForecast
import com.example.farmdatapod.season.forecastYields.data.YieldForecastDao
import com.example.farmdatapod.season.germination.data.GerminationDao
import com.example.farmdatapod.season.germination.data.GerminationEntity
import com.example.farmdatapod.season.harvest.data.HarvestPlanning
import com.example.farmdatapod.season.harvest.data.HarvestPlanningBuyer
import com.example.farmdatapod.season.harvest.data.HarvestPlanningDao
import com.example.farmdatapod.season.landPreparation.data.CoverCropEntity
import com.example.farmdatapod.season.landPreparation.data.LandPreparationDao
import com.example.farmdatapod.season.landPreparation.data.LandPreparationEntity
import com.example.farmdatapod.season.landPreparation.data.MulchingEntity
import com.example.farmdatapod.season.landPreparation.data.SoilAnalysisEntity
import com.example.farmdatapod.season.nursery.data.InputEntity
import com.example.farmdatapod.season.nursery.data.ManagementActivityEntity
import com.example.farmdatapod.season.nursery.data.NurseryPlanConverters
import com.example.farmdatapod.season.nursery.data.NurseryPlanDao
import com.example.farmdatapod.season.nursery.data.NurseryPlanEntity
import com.example.farmdatapod.season.nutrition.data.ApplicantEntity
import com.example.farmdatapod.season.nutrition.data.CropNutritionDao
import com.example.farmdatapod.season.nutrition.data.CropNutritionEntity
import com.example.farmdatapod.season.planting.data.PlantingMaterialEntity
import com.example.farmdatapod.season.planting.data.PlantingMethodEntity
import com.example.farmdatapod.season.planting.data.PlantingPlanDao
import com.example.farmdatapod.season.planting.data.PlantingPlanEntity
import com.example.farmdatapod.season.register.registerSeasonData.Season
import com.example.farmdatapod.season.register.registerSeasonData.SeasonDao
import com.example.farmdatapod.season.scouting.data.BaitScoutingDao
import com.example.farmdatapod.season.scouting.data.BaitScoutingEntity

@Database(
    entities = [
        Season::class,
        YieldForecast::class,
        GerminationEntity::class,
        BaitScoutingEntity::class,
        PlantingMaterialEntity::class,
        PlantingMethodEntity::class,
        PlantingPlanEntity::class,
        Hub::class,
        BuyingCenterEntity::class,
        CIG::class,
        ProducerEntity::class,
        CropEntity::class,
        FieldRegistrationEntity::class,
        InputEntity::class,
        ManagementActivityEntity::class,
        NurseryPlanEntity::class,
        CoverCropEntity::class,
        LandPreparationEntity::class,
        MulchingEntity::class,
        SoilAnalysisEntity::class,
        GappingActivityEntity::class,
        CropManagementEntity::class,
        PruningActivityEntity::class,
        StakingActivityEntity::class,
        ThinningActivityEntity::class,
        WateringActivityEntity::class,
        WeedingActivityEntity::class,
        ApplicantEntity::class,
        CropNutritionEntity::class,
        CropProtectionEntity::class,
        CropProtectionApplicantEntity::class,
        HarvestPlanning::class,
        HarvestPlanningBuyer::class,
        RouteEntity::class,
        JourneyEntity::class,
        StopPointEntity::class,
        EquipmentEntity::class,
        PlanJourneyInputsEntity::class,
        InputTransferEntity::class,
        LoadingInputEntity::class,
        EquipmentLoadingEntity::class,
        DispatchEntity::class,
        InboundOffloadingEntity::class,
        InboundLoadingEntity::class


    ],
    version = 47,
    exportSchema = false
)
@TypeConverters(
    ProducerConverters::class,
    NurseryPlanConverters::class,
    StopPointConverter::class
//    HarvestPlanningConverters::class
// Updated name here
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun seasonDao(): SeasonDao
    abstract fun yieldForecastDao(): YieldForecastDao
    abstract fun germinationDao(): GerminationDao
    abstract fun baitScoutingDao(): BaitScoutingDao
    abstract fun plantingPlanDao(): PlantingPlanDao
    abstract fun hubDao(): HubDao
    abstract fun buyingCenterDao(): BuyingCenterDao
    abstract fun cigDao(): CIGDAO
    abstract fun producerDao(): ProducerDao
    abstract fun fieldRegistrationDao(): FieldRegistrationDao
    abstract fun nurseryPlanDao(): NurseryPlanDao
    abstract fun landPreparationDao(): LandPreparationDao
    abstract fun cropManagementDao(): CropManagementDao
    abstract fun cropNutritionDao(): CropNutritionDao
    abstract fun cropProtectionDao(): CropProtectionDao
    abstract fun harvestPlanningDao(): HarvestPlanningDao
    abstract fun routeDao(): RouteDao
    abstract fun journeyDao(): JourneyDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun planJourneyInputsDao(): PlanJourneyInputsDao
    abstract fun inputTransferDao(): InputTransferDao
    abstract fun loadingInputDao(): LoadingInputDao
    abstract fun equipmentLoadingDao(): EquipmentLoadingDao
    abstract fun dispatchDao(): DispatchDao
    abstract fun inboundLoadingDao(): InboundLoadingDao
    abstract fun inboundOffloadingDao(): InboundOffloadingDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "farm_data_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}