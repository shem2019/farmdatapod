package com.example.farmdatapod.network


import com.example.farmdatapod.Attendance
import com.example.farmdatapod.dbmodels.BuyingCenterResponse
import com.example.farmdatapod.dbmodels.FarmerFieldRegistrationResponse
import com.example.farmdatapod.logistics.equipments.models.EquipmentModel
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.model.InboundLoadingModel
import com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.model.InboundOffloadingModel
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.model.DispatchModel
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.model.LoadedEquipmentModel
import com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.models.LoadingInputModel
import com.example.farmdatapod.logistics.inputTransfer.model.InputTransferModel
import com.example.farmdatapod.models.BaitModel
import com.example.farmdatapod.models.BuyingCentreRequest
import com.example.farmdatapod.models.CIGCreateRequest
import com.example.farmdatapod.models.CIGRegistrationItem
import com.example.farmdatapod.models.CIGServerResponse
import com.example.farmdatapod.models.CropManagementModel
import com.example.farmdatapod.models.CropNutritionModel
import com.example.farmdatapod.models.CropProtectionModel
import com.example.farmdatapod.models.CustomUserRequestModel
import com.example.farmdatapod.models.CustomerPriceRequestModel
import com.example.farmdatapod.models.FarmerFieldRegistrationRequest
import com.example.farmdatapod.models.FarmerPriceRequestModel
import com.example.farmdatapod.models.ForecastYieldModel
import com.example.farmdatapod.models.GerminationModel
import com.example.farmdatapod.models.HQUsersRequest
import com.example.farmdatapod.models.HubResponse
import com.example.farmdatapod.models.HubUserResponse
import com.example.farmdatapod.models.IndividualCustomerRequestModel
import com.example.farmdatapod.models.IndividualLogisticianRequestModel
import com.example.farmdatapod.models.InputAllocationModel
import com.example.farmdatapod.models.JourneyModel
import com.example.farmdatapod.models.LandPreparationModel
import com.example.farmdatapod.models.LoginRequest
import com.example.farmdatapod.models.LoginResponse
import com.example.farmdatapod.models.OrganisationCustomerRequestModel
import com.example.farmdatapod.models.OrganisationLogisticianRequest
import com.example.farmdatapod.models.PlanHarvestingModel
import com.example.farmdatapod.models.PlanNurseryModel
import com.example.farmdatapod.models.PlanPlantingModel
import com.example.farmdatapod.models.ProducerBiodataRequest
import com.example.farmdatapod.models.RegisterRequest
import com.example.farmdatapod.models.RegisterResponse
import com.example.farmdatapod.models.RouteModel
import com.example.farmdatapod.models.RuralWorkerRequest
import com.example.farmdatapod.models.STKPushRequest
import com.example.farmdatapod.models.STKPushResponse
import com.example.farmdatapod.models.SeasonPlanningRequestModel
import com.example.farmdatapod.models.SeasonRequestModel
import com.example.farmdatapod.models.SeasonResponse
import com.example.farmdatapod.models.SellingRequestModel
import com.example.farmdatapod.models.TrainingRequestModel
import com.example.farmdatapod.models.UploadResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @POST("login")
    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>

    @Multipart
    @POST("upload-to-cloudinary")
    fun uploadToCloudinary(@Part file: MultipartBody.Part): Call<UploadResponse>

    @POST("hubs")
    fun registerHubs(
        @Body registerRequest: RegisterRequest
    ): Call<RegisterResponse>


    // initial get requests that populate the local sqlite db
    @GET("users")
    fun getUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("hubs")
    fun getHubs(@Header("Authorization") authToken: String?): Call<HubResponse>


    @GET("buying-centers")
    fun getBuyingCenters(@Header("Authorization") authToken: String?): Call<BuyingCenterResponse>?

    @GET("cigs")
    fun getCigs(@Header("Authorization") authToken: String?): Call<ResponseBody>

    @GET("custom-users")
    fun getCustomUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("hub-users")
    fun getHubUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("hq-users")
    fun getHQUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("processing-users")
    fun getProcessingUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("individual-logistician-users")
    fun getIndividualLogisticianUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("organisation-logistician-users")
    fun getOrganisationLogisticianUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("individual-customer-users")
    fun getIndividualCustomerUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("organisation-customer-users")
    fun getOrganisationCustomerUsers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("producers-biodata")
    fun getProducersBiodata(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

//    @GET("cig-producers-biodata")
//    fun getCIGProducersBiodata(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("farmers-field-registrations")
    fun getFarmerFieldRegistrations(): Call<List<FarmerFieldRegistrationResponse>>  // Changed to List

//    @GET("cig-farmers-field-registrations")
//    fun getCIGFarmerFieldRegistrations(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

//    @GET("seasons-planning")
//    fun getSeasonsPlanning(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

//    @GET("extension-services")
//    fun getExtensionServices(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("trainings")
    fun getTrainings(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("attendance")
    fun getAttendance(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("farmer-price-distributions")
    fun getFarmerPriceDistributions(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("customer-price-distributions")
    fun getCustomerPriceDistributions(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

//    @GET("buying")
//    fun getBuying(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("quarantine")
    fun getQuarantine(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("selling")
    fun getSelling(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("make-payment")
    fun getMakePayment(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("receive-payment")
    fun getReceivePayment(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

//    @GET("plan-journey")
//    fun getPlanJourney(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

//    @GET("loading")
//    fun getLoading(@Header("Authorization") authToken: String?): Call<ResponseBody?>?
//
//    @GET("offloading")
//    fun getOffoading(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @GET("rural-workers")
    fun getRuralWorkers(@Header("Authorization") authToken: String?): Call<ResponseBody?>?

    @POST("producers-biodata")
    fun postProducerBiodata(@Body biodata: ProducerBiodataRequest): Call<Unit>

//    @POST("cig-producers-biodata")
//    fun postCIGProducerBiodata(@Body biodata: ProducerBiodataRequest): Call<Unit>

    @POST("/farmers-field-registrations")
    suspend fun registerFarmerField(@Body request: FarmerFieldRegistrationRequest): Response<Void>

//    @POST("/cig-farmers-field-registrations")
//    suspend fun cigRegisterFarmerField(@Body request: FarmerFieldRegistrationRequest): Response<Void>

    @POST("buying-centers")
    fun registerBuyingCentre(
        @Body buyingCentreRequest: BuyingCentreRequest
    ): Call<BuyingCentreRequest>

    @POST("/cigs")
    suspend fun registerCIG(@Body cigCreateRequest: CIGCreateRequest): Response<CIGServerResponse>

    @POST("/attendance")
    fun postAttendance(@Body attendance: Attendance): Call<Void>

    @POST("hub-users")
    fun registerHubAttendant(
        @Body hubUserResponse: HubUserResponse
    ): Call<HubUserResponse>

    @POST("hq-users")
    fun registerHQUser(
        @Body hqUsersRequest: HQUsersRequest
    ): Call<HQUsersRequest>

    @POST("individual-logistician-users")
    fun registerIndividualLogistician(
        @Body individualLogisticianRequestModel: IndividualLogisticianRequestModel
    ): Call<IndividualLogisticianRequestModel>

    @POST("organisation-logistician-users")
    fun registerOrganisationalLogistician(
        @Body organisationLogisticianRequest: OrganisationLogisticianRequest
    ): Call<OrganisationLogisticianRequest>

    @POST("rural-workers")
    fun registerRuralWorker(
        @Body ruralWorkerRequest: RuralWorkerRequest
    ): Call<RuralWorkerRequest>

    @POST("trainings")
    fun registerTraining(
        @Body trainingRequestModel: TrainingRequestModel
    ): Call<TrainingRequestModel>

    @POST("seasons-planning")
    fun registerSeasonsPlanning(
        @Body seasonPlanningRequestModel: SeasonPlanningRequestModel
    ): Call<SeasonPlanningRequestModel>

    @POST("farmer-price-distributions")
    fun registerFarmerPrice(
        @Body farmerPriceRequestModel: FarmerPriceRequestModel
    ): Call<FarmerPriceRequestModel>

    @POST("customer-price-distributions")
    fun registerCustomerPrice(
        @Body customerPriceRequestModel: CustomerPriceRequestModel
    ): Call<CustomerPriceRequestModel>

//    @POST("extension-services")
//    fun registerExtensionService(
//        @Body extensionServiceRequestModel: ExtensionServiceRequestModel
//    ): Call<ExtensionServiceRequestModel>

    @POST("selling")
    fun registerSelling(
        @Body sellingRequestModel: SellingRequestModel
    ): Call<SellingRequestModel>

//    @POST("buying")
//    fun registerBuying(
//        @Body buyingRequestModel: BuyingRequestModel
//    ): Call<BuyingRequestModel>

//    @POST("offloading")
//    fun submitOffloading(
//        @Body offloadingRequestModel: OffloadingRequestModel
//    ): Call<OffloadingRequestModel>

//    @POST("loading")
//    fun submitLoading(
//        @Body loadingRequestModel: LoadingRequestModel
//    ): Call<LoadingRequestModel>

//    @POST("plan-journey")
//    fun submitPlanJourney(
//        @Body planJourneyRequestModel: PlanJourneyRequestModel
//    ): Call<PlanJourneyRequestModel>

    @POST("custom-users")
    fun registerCustomUser(
        @Body customUserRequestModel: CustomUserRequestModel
    ): Call<CustomUserRequestModel>

    @POST("organisation-customer-users")
    fun registerOrganisationCustomer(
        @Body organisationCustomerRequestModel: OrganisationCustomerRequestModel
    ): Call<OrganisationCustomerRequestModel>


    @POST("individual-customer-users")
    fun registerIndividualCustomer(
        @Body individualCustomerRequestModel: IndividualCustomerRequestModel
    ): Call<IndividualCustomerRequestModel>

//    @POST("mpesa/stkpush/v1/processrequest")
//    suspend fun initiateSTKPush(@Body request: STKPushRequest): STKPushResponse

    @POST("stkpush")
    suspend fun initiateSTKPush(@Body request: STKPushRequest): STKPushResponse

    @POST("season-planning")
    fun registerSeasonPlanning(
        @Body seasonRequestModel: SeasonRequestModel
    ): Call<SeasonResponse>

    @GET("season-planning")
    fun getAllSeasons(): Call<List<SeasonResponse>>

    @POST("plan-forecast-yields")
    fun planForecastYields(
        @Body forecastYieldModel: ForecastYieldModel
    ): Call<ForecastYieldModel>

    @GET("plan-forecast-yields")
    fun getForecastYields(): Call<List<ForecastYieldModel>>


    @POST("plan-scouting-station")
    fun planScoutingStation(
        @Body baitModel: BaitModel
    ): Call<BaitModel>

    @GET("plan-scouting-station")
    fun getScoutingStations(): Call<List<BaitModel>>

    @POST("plan-planting")
    fun planPlanting(
        @Body planPlantingModel: PlanPlantingModel
    ): Call<PlanPlantingModel>

    @GET("plan-planting")
    fun getPlantingPlans(): Call<List<PlanPlantingModel>>

    @POST("plan-nursery")
    fun planNursery(
        @Body planNurseryModel: PlanNurseryModel
    ): Call<PlanNurseryModel>

    @GET("plan-nursery")
    fun getNurseryPlans(): Call<List<PlanNurseryModel>>

    @POST("plan-land-preparation")
    fun planLandPreparation(
        @Body landPreparationModel: LandPreparationModel
    ): Call<LandPreparationModel>

    @GET("plan-land-preparation")
    fun getLandPreparationPlans(): Call<List<LandPreparationModel>>


    @POST("plan-crop-management-activities")
    fun planCropManagementActivities(
        @Body cropManagementModel: CropManagementModel
    ): Call<CropManagementModel>

    @GET("plan-crop-management-activities")
    fun getCropManagementActivities(): Call<List<CropManagementModel>>

    @POST("plan-crop-nutrition")
    fun planCropNutrition(
        @Body cropNutritionModel: CropNutritionModel
    ): Call<CropNutritionModel>

    @GET("plan-crop-nutrition")
    fun getCropNutrition(): Call<List<CropNutritionModel>>

    @POST("plan-crop-protection")
    fun planCropProtection(
        @Body cropProtectionModel: CropProtectionModel
    ): Call<CropProtectionModel>

    @GET("plan-crop-protection")
    fun getCropProtection(): Call<List<CropProtectionModel>>

    @POST("plan-harvesting")
    fun planHarvesting(
        @Body planHarvestingModel: PlanHarvestingModel
    ): Call<PlanHarvestingModel>

    @GET("plan-harvesting")
    fun getHarvestingPlans(): Call<List<PlanHarvestingModel>>

    @POST("nursery")
    fun nurseryManagement(
        @Body planNurseryModel: PlanNurseryModel
    ): Call<PlanNurseryModel>

    @GET("nursery")
    fun getNurseryManagement(): Call<List<PlanNurseryModel>>

    @POST("land-preparation")
    fun landPreparationManagement(
        @Body landPreparationModel: LandPreparationModel
    ): Call<LandPreparationModel>

    @GET("land-preparation")
    fun getLandPreparationManagement(): Call<List<LandPreparationModel>>


    @POST("planting")
    fun plantingManagement(
        @Body planPlantingModel: PlanPlantingModel
    ): Call<PlanPlantingModel>

    @GET("planting")
    fun getPlantingManagement(): Call<List<PlanPlantingModel>>


    @POST("plan-germination")
    fun postGermination(
        @Body germinationModel: GerminationModel
    ): Call<GerminationModel>

    @GET("plan-germination")
    fun getGermination(): Call<List<GerminationModel>>


    @POST("germination")
    fun postGerminationManagement(
        @Body germinationModel: GerminationModel
    ): Call<GerminationModel>

    @GET("germination")
    fun getGerminationManagement(): Call<List<GerminationModel>>


    @POST("forecast-yields")
    fun forecastYieldsManagement(
        @Body forecastYieldModel: ForecastYieldModel
    ): Call<ForecastYieldModel>

    @GET("forecast-yields")
    fun getForecastYieldsManagement(): Call<List<ForecastYieldModel>>

    @POST("crop-management-activities")
    fun cropManagementActivities(
        @Body cropManagementModel: CropManagementModel
    ): Call<CropManagementModel>

    @GET("crop-management-activities")
    fun getCropManagement(): Call<List<CropManagementModel>>

    @POST("crop-nutrition")
    fun CropNutritionManagement(
        @Body cropNutritionModel: CropNutritionModel
    ): Call<CropNutritionModel>

    @GET("crop-nutrition")
    fun getCropNutritionManagement(): Call<List<CropNutritionModel>>

    @POST("crop-protection")
    fun cropProtectionManagement(
        @Body cropProtectionModel: CropProtectionModel
    ): Call<CropProtectionModel>

    @GET("crop-protection")
    fun getCropProtectionManagement(): Call<List<CropProtectionModel>>


    @POST("harvesting")
    fun harvestingManagement(
        @Body planHarvestingModel: PlanHarvestingModel
    ): Call<PlanHarvestingModel>

    @GET("harvesting")
    fun getHarvestingManagement(): Call<List<PlanHarvestingModel>>


    @POST("scouting-station")
    fun scoutingStationManagement(
        @Body baitModel: BaitModel
    ): Call<BaitModel>

    @GET("scouting-station")
    fun getScoutingStationsManagement(): Call<List<BaitModel>>

    @POST("routes")
    fun createRoute(@Body routeModel: RouteModel): Call<RouteModel>

    @GET("routes")
    fun getRoutes(): Call<List<RouteModel>>

    @POST("plan-journey")
    fun planJourney(@Body journeyModel: JourneyModel): Call<JourneyModel>

    @GET("plan-journey")
    fun getJourneys(): Call<List<JourneyModel>>


    @POST("plan-journey-inputs")
    fun planJourneyInputs(@Body inputAllocationModel: InputAllocationModel): Call<InputAllocationModel>

    @GET("plan-journey-inputs")
    fun getJourneyInputs(): Call<List<InputAllocationModel>>

    @POST("input-transfer-request")
    fun inputTransferRequest(@Body inputTransferModel: InputTransferModel): Call<InputTransferModel>

    @GET("input-transfer-request")
    fun getInputTransferRequests(): Call<List<InputTransferModel>>

    @POST("loaded-inputs")
    fun loadedInputs(@Body loadingInputModel: LoadingInputModel): Call<LoadingInputModel>

    @GET("loaded-inputs")
    fun getLoadedInputs(): Call<List<LoadingInputModel>>

    @POST("plan-journey-equipment")
    fun planJourneyEquipment(@Body equipmentModel: EquipmentModel): Call<EquipmentModel>

    @GET("plan-journey-equipment")
    fun getJourneyEquipment(): Call<List<EquipmentModel>>

    @POST("loaded-equipment")
    fun loadedEquipment(@Body loadedEquipmentModel: LoadedEquipmentModel): Call<LoadedEquipmentModel>

    @GET("loaded-equipment")
    fun getLoadedEquipment(): Call<List<LoadedEquipmentModel>>

    @POST("dispatch")
    fun dispatch(@Body dispatchModel: DispatchModel): Call<DispatchModel>

    @GET("dispatch")
    fun getDispatches(): Call<List<DispatchModel>>

    @POST("loading")
    fun loading(@Body inboudLoadingModel: InboundLoadingModel): Call<InboundLoadingModel>

    @GET("loading")
    fun getLoadings(): Call<List<InboundLoadingModel>>

    @POST("offloadings")
    fun offloading(@Body inboundOffloadingModel: InboundOffloadingModel): Call<InboundOffloadingModel>

    @GET("offloadings")
    fun getOffloadings(): Call<List<InboundOffloadingModel>>

}