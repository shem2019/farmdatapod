
package com.example.farmdatapod

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.farmdatapod.dbmodels.ScoutingStation
import com.example.farmdatapod.dbmodels.User
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

class DBHandler (context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create Users Table
        val createUsersTable = ("CREATE TABLE " + usersTableName + " ("
                + idCol + " TEXT PRIMARY KEY, "
                + lastNameCol + " TEXT NOT NULL, "
                + otherNameCol + " TEXT NOT NULL, "
                + userTypeCol + " TEXT NOT NULL, "
                + emailCol + " TEXT NOT NULL, "
                + roleCol + " TEXT NOT NULL, "
                + emailVerifiedCol + " INTEGER DEFAULT 0, "
                + passwordCol + " TEXT NOT NULL, "
                + verificationTokenCol + " TEXT, "
                + createdAtCol + " TEXT DEFAULT CURRENT_TIMESTAMP, "
                + updatedAtCol + " TEXT)")

        // Create Hubs Table
        val createHubsTable = ("CREATE TABLE " + HUB_TABLE_NAME + " ("
                + HUB_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + regionCol + " TEXT NOT NULL, "
                + hubNameCol + " TEXT NOT NULL, "
                + hubCodeCol + " TEXT NOT NULL, "
                + addressCol + " TEXT NOT NULL, "
                + yearEstablishedCol + " DATETIME, "
                + ownershipCol + " TEXT NOT NULL, "
                + floorSizeCol + " TEXT NOT NULL, "
                + facilitiesCol + " TEXT NOT NULL, "
                + inputCenterCol + " TEXT NOT NULL, "
                + typeOfBuildingCol + " TEXT NOT NULL, "
                + longitudeCol + " TEXT NOT NULL, "
                + latitudeCol + " TEXT NOT NULL, "
                + hubUserIdCol + " TEXT NOT NULL, " // Changed from INTEGER
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + hubUserIdCol + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create KeyContacts Table
        val createKeyContactsTable = ("CREATE TABLE " + KEY_CONTACTS_TABLE_NAME + " ("
                + KEY_CONTACT_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_CONTACT_OTHER_NAME_COL + " TEXT NOT NULL, "
                + KEY_CONTACT_LAST_NAME_COL + " TEXT NOT NULL, "
                + ID_NUMBER_COL + " INTEGER, "
                + GENDER_COL + " TEXT NOT NULL, "
                + KEY_CONTACT_ROLE_COL + " TEXT NOT NULL, "
                + DATE_OF_BIRTH_COL + " DATETIME, "
                + KEY_CONTACT_EMAIL_COL + " TEXT NOT NULL, "
                + PHONE_NUMBER_COL + " INTEGER, "
                + HUB_ID_FK_COL + " INTEGER, "
                + BUYING_CENTER_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + HUB_ID_FK_COL + ") REFERENCES " + HUB_TABLE_NAME + "(" + HUB_ID_COL + "), "
                + "FOREIGN KEY(" + BUYING_CENTER_ID_FK_COL + ") REFERENCES " + BUYING_CENTER_TABLE_NAME + "(" + BUYING_CENTER_ID_COL + "))")

        // Create BuyingCenter Table
        val createBuyingCenterTable = ("CREATE TABLE " + BUYING_CENTER_TABLE_NAME + " ("
                + BUYING_CENTER_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HUB_COL + " TEXT NOT NULL, "
                + COUNTY_COL + " TEXT NOT NULL, "
                + SUB_COUNTY_COL + " TEXT NOT NULL, "
                + WARD_COL + " TEXT NOT NULL, "
                + VILLAGE_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_NAME_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_CODE_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_ADDRESS_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_YEAR_ESTABLISHED_COL + " DATETIME, "
                + BUYING_CENTER_OWNERSHIP_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_FLOOR_SIZE_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_FACILITIES_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_INPUT_CENTER_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_TYPE_OF_BUILDING_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_LOCATION_COL + " TEXT NOT NULL, "
                + BUYING_CENTER_USER_ID_COL + " INTEGER NOT NULL, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + BUYING_CENTER_USER_ID_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        //Create cig table
        val createCIGTable = ("CREATE TABLE " + CIG_TABLE_NAME + " ("
                + CIG_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + CIG_HUB_COL + " TEXT NOT NULL, "
                + CIG_NAME_COL + " TEXT NOT NULL, "
                + NO_OF_MEMBERS_COL + " INTEGER, "
                + DATE_ESTABLISHED_COL + " DATETIME, "
                + CONSTITUTION_COL + " TEXT NOT NULL, "
                + REGISTRATION_COL + " TEXT NOT NULL, "
                + ELECTIONS_HELD_COL + " TEXT NOT NULL, "
                + DATE_OF_LAST_ELECTIONS_COL + " DATETIME, "
                + MEETING_VENUE_COL + " TEXT NOT NULL, "
                + FREQUENCY_COL + " TEXT NOT NULL, "
                + SCHEDULED_MEETING_DAY_COL + " TEXT NOT NULL, "
                + SCHEDULED_MEETING_TIME_COL + " TEXT NOT NULL, "
                + CIG_USER_ID_COL + " TEXT NOT NULL, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + CIG_USER_ID_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        //create members table
        val createMembersTable = ("CREATE TABLE " + MEMBERS_TABLE_NAME + " ("
                + MEMBER_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MEMBER_OTHER_NAME_COL + " TEXT NOT NULL, "
                + MEMBER_LAST_NAME_COL + " TEXT NOT NULL, "
                + MEMBER_GENDER_COL + " TEXT NOT NULL, "
                + MEMBER_DATE_OF_BIRTH_COL + " DATETIME, "
                + MEMBER_EMAIL_COL + " TEXT NOT NULL, "
                + MEMBER_PHONE_NUMBER_COL + " INTEGER, "
                + MEMBER_ID_NUMBER_COL + " INTEGER, "
                + PRODUCT_INVOLVED_COL + " TEXT NOT NULL, "
                + HECTORAGE_REGISTERED_UNDER_CIG_COL + " TEXT NOT NULL, "
                + MEMBER_CIG_ID_COL + " INTEGER NOT NULL, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + MEMBER_CIG_ID_COL + ") REFERENCES " + CIG_TABLE_NAME + "(" + CIG_ID_COL + "))")

        // create custom users table
        val createCustomUserTable = ("CREATE TABLE " + CUSTOM_USER_TABLE_NAME + " ("
                + CUSTOM_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + CUSTOM_USER_OTHER_NAME_COL + " TEXT NOT NULL, "
                + CUSTOM_USER_LAST_NAME_COL + " TEXT NOT NULL, "
                + CUSTOM_USER_STAFF_CODE_COL + " TEXT, "
                + CUSTOM_USER_ID_NUMBER_COL + " TEXT, "
                + CUSTOM_USER_GENDER_COL + " TEXT, "
                + CUSTOM_USER_DATE_OF_BIRTH_COL + " TEXT, "
                + CUSTOM_USER_EMAIL_COL + " TEXT, "
                + CUSTOM_USER_PHONE_NUMBER_COL + " TEXT, "
                + CUSTOM_USER_EDUCATION_LEVEL_COL + " TEXT, "
                + CUSTOM_USER_ROLE_COL + " TEXT, "
                + CUSTOM_USER_REPORTING_TO_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + CUSTOM_USER_USER_ID_COL + " TEXT)")

        //create hub users table
        val createHubUserTable = ("CREATE TABLE " + HUB_USER_TABLE_NAME + " ("
                + HUB_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + HUB_USER_OTHER_NAME_COL + " TEXT NOT NULL, "
                + HUB_USER_LAST_NAME_COL + " TEXT NOT NULL, "
                + HUB_USER_CODE_COL + " TEXT, "
                + HUB_USER_ROLE_COL + " TEXT, "
                + HUB_USER_ID_NUMBER_COL + " TEXT, "
                + HUB_USER_GENDER_COL + " TEXT, "
                + HUB_USER_DATE_OF_BIRTH_COL + " TEXT, "
                + HUB_USER_EMAIL_COL + " TEXT, "
                + HUB_USER_PHONE_NUMBER_COL + " TEXT, "
                + HUB_USER_EDUCATION_LEVEL_COL + " TEXT, "
                + HUB_USER_HUB_COL + " TEXT, "
                + HUB_USER_BUYING_CENTER_COL + " TEXT, "
                + HUB_USER_COUNTY_COL + " TEXT, "
                + HUB_USER_SUB_COUNTY_COL + " TEXT, "
                + HUB_USER_WARD_COL + " TEXT, "
                + HUB_USER_VILLAGE_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + HUB_USER_USER_ID_COL + " TEXT)")

        // create hq user table
        val createHQUserTable = ("CREATE TABLE " + HQ_USER_TABLE_NAME + " ("
                + HQ_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + HQ_USER_OTHER_NAME_COL + " TEXT NOT NULL, "
                + HQ_USER_LAST_NAME_COL + " TEXT NOT NULL, "
                + HQ_USER_STAFF_CODE_COL + " TEXT, "
                + HQ_USER_DEPARTMENT_COL + " TEXT, "
                + HQ_USER_ID_NUMBER_COL + " TEXT, "
                + HQ_USER_GENDER_COL + " TEXT, "
                + HQ_USER_DATE_OF_BIRTH_COL + " TEXT, "
                + HQ_USER_EMAIL_COL + " TEXT, "
                + HQ_USER_PHONE_NUMBER_COL + " TEXT, "
                + HQ_USER_EDUCATION_LEVEL_COL + " TEXT, "
                + HQ_USER_ROLE_COL + " TEXT, "
                + HQ_USER_REPORTING_TO_COL + " TEXT, "
                + HQ_USER_RELATED_ROLES_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + HQ_USER_USER_ID_COL + " TEXT)")

        //create processing user table
        val createProcessingUserTable = ("CREATE TABLE " + PROCESSING_USER_TABLE_NAME + " ("
                + PROCESSING_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + PROCESSING_USER_OTHER_NAME_COL + " TEXT NOT NULL, "
                + PROCESSING_USER_LAST_NAME_COL + " TEXT NOT NULL, "
                + PROCESSING_USER_PROCESSOR_CODE_COL + " TEXT, "
                + PROCESSING_USER_PROCESSING_PLANT_COL + " TEXT, "
                + PROCESSING_USER_ID_NUMBER_COL + " TEXT, "
                + PROCESSING_USER_GENDER_COL + " TEXT, "
                + PROCESSING_USER_DATE_OF_BIRTH_COL + " TEXT, "
                + PROCESSING_USER_EMAIL_COL + " TEXT, "
                + PROCESSING_USER_PHONE_NUMBER_COL + " TEXT, "
                + PROCESSING_USER_EDUCATION_LEVEL_COL + " TEXT, "
                + PROCESSING_USER_HUB_COL + " TEXT, "
                + PROCESSING_USER_BUYING_CENTER_COL + " TEXT, "
                + PROCESSING_USER_COUNTY_COL + " TEXT, "
                + PROCESSING_USER_SUB_COUNTY_COL + " TEXT, "
                + PROCESSING_USER_WARD_COL + " TEXT, "
                + PROCESSING_USER_VILLAGE_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + PROCESSING_USER_USER_ID_COL + " TEXT)")

        // create individual logistician table
        val createIndividualLogisticianUserTable = ("CREATE TABLE " + INDIVIDUAL_LOGISTICIAN_USER_TABLE_NAME + " ("
                + INDIVIDUAL_LOGISTICIAN_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + INDIVIDUAL_LOGISTICIAN_USER_OTHER_NAME_COL + " TEXT NOT NULL, "
                + INDIVIDUAL_LOGISTICIAN_USER_LAST_NAME_COL + " TEXT NOT NULL, "
                + LOGISTICIAN_CODE_COL + " TEXT, "
                + INDIVIDUAL_LOGISTICIAN_USER_ID_NUMBER_COL + " TEXT, "
                + INDIVIDUAL_LOGISTICIAN_USER_DATE_OF_BIRTH_COL + " TEXT, "
                + INDIVIDUAL_LOGISTICIAN_USER_EMAIL_COL + " TEXT, "
                + INDIVIDUAL_LOGISTICIAN_USER_PHONE_NUMBER_COL + " TEXT, "
                + INDIVIDUAL_LOGISTICIAN_USER_ADDRESS_COL + " TEXT, "
                + INDIVIDUAL_LOGISTICIAN_USER_HUB_COL + " TEXT, "
                + INDIVIDUAL_LOGISTICIAN_USER_REGION_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + INDIVIDUAL_LOGISTICIAN_USER_USER_ID_COL + " TEXT)")

        // create organisational user table
        val createOrganisationLogisticianUserTable = ("CREATE TABLE " + ORGANISATION_LOGISTICIAN_USER_TABLE_NAME + " ("
                + ORGANISATION_LOGISTICIAN_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + ORGANISATION_LOGISTICIAN_USER_NAME_COL + " TEXT NOT NULL, "
                + ORGANISATION_LOGISTICIAN_USER_LOGISTICIAN_CODE_COL + " TEXT, "
                + ORGANISATION_LOGISTICIAN_USER_REGISTRATION_NUMBER_COL + " TEXT, "
                + ORGANISATION_LOGISTICIAN_USER_DATE_OF_REGISTRATION_COL + " TEXT, "
                + ORGANISATION_LOGISTICIAN_USER_EMAIL_COL + " TEXT, "
                + ORGANISATION_LOGISTICIAN_USER_PHONE_NUMBER_COL + " TEXT, "
                + ORGANISATION_LOGISTICIAN_USER_ADDRESS_COL + " TEXT, "
                + ORGANISATION_LOGISTICIAN_USER_HUB_COL + " TEXT, "
                + ORGANISATION_LOGISTICIAN_USER_REGION_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + ORGANISATION_LOGISTICIAN_USER_USER_ID_COL + " TEXT)")

        // create cars table
        val createCarTable = ("CREATE TABLE " + CAR_TABLE_NAME + " ("
                + CAR_ID_COL + " INTEGER PRIMARY KEY, "
                + CAR_BODY_TYPE_COL + " TEXT NOT NULL, "
                + CAR_MODEL_COL + " TEXT NOT NULL, "
                + CAR_NUMBER_PLATE_COL + " TEXT, "
                + CAR_DRIVER1_NAME_COL + " TEXT, "
                + CAR_DRIVER2_NAME_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + INDIVIDUAL_LOGISTICIAN_ID_FK_COL + " INTEGER, "
                + ORGANISATION_LOGISTICIAN_ID_FK_COL + " INTEGER)")

        //create individual customer table
        val createIndividualCustomerUserTable = ("CREATE TABLE " + INDIVIDUAL_CUSTOMER_USER_TABLE_NAME + " ("
                + INDIVIDUAL_CUSTOMER_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + INDIVIDUAL_CUSTOMER_USER_OTHER_NAME_COL + " TEXT NOT NULL, "
                + INDIVIDUAL_CUSTOMER_USER_LAST_NAME_COL + " TEXT NOT NULL, "
                + INDIVIDUAL_CUSTOMER_USER_CUSTOMER_CODE_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_ID_NUMBER_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_GENDER_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_DATE_OF_BIRTH_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_EMAIL_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_PHONE_NUMBER_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_COUNTY_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_SUB_COUNTY_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_WARD_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_VILLAGE_COL + " TEXT, "
                + INDIVIDUAL_CUSTOMER_USER_USER_AUTHORISED_COL + " INTEGER DEFAULT 0, "
                + INDIVIDUAL_CUSTOMER_USER_AUTHORISATION_TOKEN_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + INDIVIDUAL_CUSTOMER_USER_USER_ID_COL + " TEXT)")

        // create organisational customer table
        val createOrganisationCustomerUserTable = ("CREATE TABLE " + ORGANISATION_CUSTOMER_USER_TABLE_NAME + " ("
                + ORGANISATION_CUSTOMER_USER_ID_COL + " INTEGER PRIMARY KEY, "
                + ORGANISATION_CUSTOMER_USER_COMPANY_NAME_COL + " TEXT NOT NULL, "
                + ORGANISATION_CUSTOMER_USER_CUSTOMER_CODE_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_REGISTRATION_NUMBER_COL + " INTEGER, "
                + ORGANISATION_CUSTOMER_USER_SECTOR_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_DATE_OF_REGISTRATION_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_EMAIL_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_PHONE_NUMBER_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_COUNTY_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_SUB_COUNTY_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_WARD_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_VILLAGE_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_OTHER_NAME1_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_LAST_NAME1_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_ID_NUMBER1_COL + " INTEGER, "
                + ORGANISATION_CUSTOMER_USER_GENDER1_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH1_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_EMAIL1_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_PHONE_NUMBER1_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_OTHER_NAME2_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_LAST_NAME2_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_ID_NUMBER2_COL + " INTEGER, "
                + ORGANISATION_CUSTOMER_USER_GENDER2_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH2_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_EMAIL2_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_PHONE_NUMBER2_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_OTHER_NAME3_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_LAST_NAME3_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_ID_NUMBER3_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_GENDER3_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH3_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_EMAIL3_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_PHONE_NUMBER3_COL + " TEXT, "
                + ORGANISATION_CUSTOMER_USER_USER_AUTHORISED_COL + " INTEGER DEFAULT 0, "
                + ORGANISATION_CUSTOMER_USER_AUTHORISATION_TOKEN_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + ORGANISATION_CUSTOMER_USER_USER_ID_COL + " TEXT)")

        //create product table
        val createProductTable = ("CREATE TABLE " + PRODUCT_TABLE_NAME + " ("
                + PRODUCT_ID_COL + " INTEGER PRIMARY KEY, "
                + PRODUCT_CATEGORY_COL + " TEXT NOT NULL, "
                + PRODUCT_PRODUCTS_INTERESTED_IN_COL + " TEXT, "
                + PRODUCT_VOLUME_IN_KGS_COL + " TEXT, "
                + PRODUCT_PACKAGING_COL + " TEXT, "
                + PRODUCT_QUALITY_COL + " TEXT, "
                + PRODUCT_FREQUENCY_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + INDIVIDUAL_CUSTOMER_ID_FK_COL + " INTEGER, "
                + ORGANISATION_CUSTOMER_ID_FK_COL + " INTEGER)")

        // Create ProducerBiodata Table
//        val createProducerBiodataTable = ("CREATE TABLE " + PRODUCER_BIODATA_TABLE_NAME + " ("
//                + PRODUCER_BIODATA_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                + PRODUCER_OTHER_NAME_COL + " TEXT NOT NULL, "
//                + PRODUCER_LAST_NAME_COL + " TEXT NOT NULL, "
//                + PRODUCER_FARMER_CODE_COL + " TEXT NOT NULL, "
//                + PRODUCER_ID_NUMBER_COL + " INTEGER, "
//                + PRODUCER_GENDER_COL + " TEXT NOT NULL, "
//                + PRODUCER_DATE_OF_BIRTH_COL + " DATETIME, "
//                + PRODUCER_EMAIL_COL + " TEXT NOT NULL, "
//                + PRODUCER_PHONE_NUMBER_COL + " INTEGER, "
//                + PRODUCER_HUB_COL + " TEXT NOT NULL, "
//                + PRODUCER_BUYING_CENTER_COL + " TEXT NOT NULL, "
//                + PRODUCER_EDUCATION_LEVEL_COL + " TEXT NOT NULL, "
//                + PRODUCER_COUNTY_COL + " TEXT NOT NULL, "
//                + PRODUCER_SUB_COUNTY_COL + " TEXT NOT NULL, "
//                + PRODUCER_WARD_COL + " TEXT NOT NULL, "
//                + PRODUCER_VILLAGE_COL + " TEXT NOT NULL, "
//                + PRODUCER_PRIMARY_PRODUCER_COL + " TEXT NOT NULL, "
//                + PRODUCER_TOTAL_LAND_SIZE_COL + " TEXT NOT NULL, "
//                + PRODUCER_CULTIVATE_LAND_SIZE_COL + " TEXT NOT NULL, "
//                + PRODUCER_HOMESTEAD_SIZE_COL + " TEXT NOT NULL, "
//                + PRODUCER_UNCULTIVATED_LAND_SIZE_COL + " TEXT NOT NULL, "
//                + PRODUCER_FARM_ACCESSIBILITY_COL + " TEXT NOT NULL, "
//                + PRODUCER_NUMBER_OF_FAMILY_WORKERS_COL + " TEXT NOT NULL, "
//                + PRODUCER_NUMBER_OF_HIRED_WORKERS_COL + " TEXT NOT NULL, "
//                + PRODUCER_ACCESS_TO_IRRIGATION_COL + " TEXT NOT NULL, "
//                + PRODUCER_CROP_LIST_COL + " TEXT NOT NULL, "
//                + PRODUCER_FARMER_INTEREST_IN_EXTENSION_COL + " TEXT NOT NULL, "
//                + PRODUCER_KNOWLEDGE_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_SOIL_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_COMPOST_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_NUTRITION_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_PESTS_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_DISEASE_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_QUALITY_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_MARKET_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_FOOD_LOSS_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_FINANCE_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_WEATHER_RELATED_COL + " TEXT NOT NULL, "
//                + PRODUCER_DAIRY_CATTLE_COL + " TEXT NOT NULL, "
//                + PRODUCER_BEEF_CATTLE_COL + " TEXT NOT NULL, "
//                + PRODUCER_SHEEP_COL + " TEXT NOT NULL, "
//                + PRODUCER_POULTRY_COL + " TEXT NOT NULL, "
//                + PRODUCER_PIGS_COL + " TEXT NOT NULL, "
//                + PRODUCER_RABBITS_COL + " TEXT NOT NULL, "
//                + PRODUCER_BEEHIVES_COL + " TEXT NOT NULL, "
//                + PRODUCER_DONKEYS_COL + " TEXT NOT NULL, "
//                + PRODUCER_GOATS_COL + " TEXT NOT NULL, "
//                + PRODUCER_CAMELS_COL + " TEXT NOT NULL, "
//                + PRODUCER_AQUACULTURE_COL + " TEXT NOT NULL, "
//                + PRODUCER_HOUSING_TYPE_COL + " TEXT NOT NULL, "
//                + PRODUCER_HOUSING_FLOOR_COL + " TEXT NOT NULL, "
//                + PRODUCER_HOUSING_ROOF_COL + " TEXT NOT NULL, "
//                + PRODUCER_LIGHTING_FUEL_COL + " TEXT NOT NULL, "
//                + PRODUCER_COOKING_FUEL_COL + " TEXT NOT NULL, "
//                + PRODUCER_WATER_FILTER_COL + " TEXT NOT NULL, "
//                + PRODUCER_WATER_TANK_GREATER_THAN_5000LTS_COL + " TEXT NOT NULL, "
//                + PRODUCER_HAND_WASHING_FACILITIES_COL + " TEXT NOT NULL, "
//                + PRODUCER_PPES_COL + " TEXT NOT NULL, "
//                + PRODUCER_WATER_WELL_OR_WEIR_COL + " TEXT NOT NULL, "
//                + PRODUCER_IRRIGATION_PUMP_COL + " TEXT NOT NULL, "
//                + PRODUCER_HARVESTING_EQUIPMENT_COL + " TEXT NOT NULL, "
//                + PRODUCER_TRANSPORTATION_TYPE_COL + " TEXT NOT NULL, "
//                + PRODUCER_TOILET_FLOOR_COL + " TEXT NOT NULL, "
//                + PRODUCER_USER_APPROVED_COL + " INTEGER DEFAULT 0, "
//                + PRODUCER_TA_COL + " TEXT, "
//                + PRODUCER_USER_ID_COL + " TEXT, "
//                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
//                + "FOREIGN KEY(" + PRODUCER_USER_ID_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create CommercialProduce Table
        val createCommercialProduceTable = ("CREATE TABLE " + COMMERCIAL_PRODUCE_TABLE_NAME + " ("
                + COMMERCIAL_PRODUCE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COMMERCIAL_PRODUCE_PRODUCT_COL + " TEXT NOT NULL, "
                + COMMERCIAL_PRODUCE_PRODUCT_CATEGORY_COL + " TEXT NOT NULL, "
                + COMMERCIAL_PRODUCE_ACERAGE_COL + " TEXT NOT NULL, "
                + COMMERCIAL_PRODUCER_BIODATA_ID_FK_COL + " INTEGER, "
                + COMMERCIAL_CIG_PRODUCER_BIODATA_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + COMMERCIAL_PRODUCER_BIODATA_ID_FK_COL + ") REFERENCES " + PRODUCER_BIODATA_TABLE_NAME + "(" + PRODUCER_BIODATA_ID_COL + "), "
                + "FOREIGN KEY(" + COMMERCIAL_CIG_PRODUCER_BIODATA_ID_FK_COL + ") REFERENCES " + PRODUCER_BIODATA_TABLE_NAME + "(" + PRODUCER_BIODATA_ID_COL + "))")

        // Create DomesticProduce Table
        val createDomesticProduceTable = ("CREATE TABLE " + DOMESTIC_PRODUCE_TABLE_NAME + " ("
                + DOMESTIC_PRODUCE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DOMESTIC_PRODUCE_PRODUCT_COL + " TEXT NOT NULL, "
                + DOMESTIC_PRODUCE_PRODUCT_CATEGORY_COL + " TEXT NOT NULL, "
                + DOMESTIC_PRODUCE_ACERAGE_COL + " TEXT NOT NULL, "
                + DOMESTIC_PRODUCER_BIODATA_ID_FK_COL + " INTEGER, "
                + DOMESTIC_CIG_PRODUCER_BIODATA_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + DOMESTIC_PRODUCER_BIODATA_ID_FK_COL + ") REFERENCES " + PRODUCER_BIODATA_TABLE_NAME + "(" + PRODUCER_BIODATA_ID_COL + "), "
                + "FOREIGN KEY(" + DOMESTIC_CIG_PRODUCER_BIODATA_ID_FK_COL + ") REFERENCES " + PRODUCER_BIODATA_TABLE_NAME + "(" + PRODUCER_BIODATA_ID_COL + "))")

//        // Create FarmerFieldRegistration Table
//        val createFarmerFieldRegistrationTable = ("CREATE TABLE " + FARMER_FIELD_REGISTRATION_TABLE_NAME + " ("
//                + FARMER_FIELD_REGISTRATION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                + FARMER_FIELD_PRODUCER_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_FIELD_NUMBER_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_FIELD_SIZE_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_CROP1_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_CROP_VARIETY1_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_DATE_PLANTED1_COL + " DATETIME, "
//                + FARMER_FIELD_DATE_OF_HARVEST1_COL + " DATETIME, "
//                + FARMER_FIELD_POPULATION1_COL + " INTEGER NOT NULL, "
//                + FARMER_FIELD_BASELINE_YIELD_LAST_SEASON1_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_BASELINE_INCOME_LAST_SEASON1_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON1_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_CROP2_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_CROP_VARIETY2_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_DATE_PLANTED2_COL + " DATETIME, "
//                + FARMER_FIELD_DATE_OF_HARVEST2_COL + " DATETIME, "
//                + FARMER_FIELD_POPULATION2_COL + " INTEGER NOT NULL, "
//                + FARMER_FIELD_BASELINE_YIELD_LAST_SEASON2_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_BASELINE_INCOME_LAST_SEASON2_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON2_COL + " TEXT NOT NULL, "
//                + FARMER_FIELD_PRODUCER_BIODATA_ID_FK_COL + " INTEGER, "
//                + FARMER_FIELD_USER_ID_FK_COL + " TEXT, "
//                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
//                + "FOREIGN KEY(" + FARMER_FIELD_PRODUCER_BIODATA_ID_FK_COL + ") REFERENCES " + PRODUCER_BIODATA_TABLE_NAME + "(" + PRODUCER_BIODATA_ID_COL + "), "
//                + "FOREIGN KEY(" + FARMER_FIELD_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create CigProducerBiodata Table
//        val createCigProducerBiodataTable = ("CREATE TABLE " + CIG_PRODUCER_BIODATA_TABLE_NAME + " ("
//                + CIG_PRODUCER_BIODATA_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                + CIG_PRODUCER_OTHER_NAME_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_LAST_NAME_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_FARMER_CODE_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_ID_NUMBER_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_GENDER_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_DATE_OF_BIRTH_COL + " DATETIME, "
//                + CIG_PRODUCER_EMAIL_COL + " TEXT, "
//                + CIG_PRODUCER_PHONE_NUMBER_COL + " TEXT, "
//                + CIG_PRODUCER_HUB_COL + " TEXT, "
//                + CIG_PRODUCER_BUYING_CENTER_COL + " TEXT, "
//                + CIG_PRODUCER_EDUCATION_LEVEL_COL + " TEXT, "
//                + CIG_PRODUCER_COUNTY_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_SUB_COUNTY_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_WARD_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_VILLAGE_COL + " TEXT NOT NULL, "
//                + CIG_PRODUCER_PRIMARY_PRODUCER_COL + " TEXT, "
//                + CIG_PRODUCER_TOTAL_LAND_SIZE_COL + " TEXT, "
//                + CIG_PRODUCER_CULTIVATE_LAND_SIZE_COL + " TEXT, "
//                + CIG_PRODUCER_HOMESTEAD_SIZE_COL + " TEXT, "
//                + CIG_PRODUCER_UNCULTIVATED_LAND_SIZE_COL + " TEXT, "
//                + CIG_PRODUCER_FARM_ACCESSIBILITY_COL + " TEXT, "
//                + CIG_PRODUCER_NUMBER_OF_FAMILY_WORKERS_COL + " INTEGER, "
//                + CIG_PRODUCER_NUMBER_OF_HIRED_WORKERS_COL + " INTEGER, "
//                + CIG_PRODUCER_ACCESS_TO_IRRIGATION_COL + " TEXT, "
//                + CIG_PRODUCER_CROP_LIST_COL + " TEXT, "
//                + CIG_PRODUCER_FARMER_INTEREST_IN_EXTENSION_COL + " TEXT, "
//                + CIG_PRODUCER_KNOWLEDGE_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_SOIL_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_COMPOST_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_NUTRITION_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_PESTS_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_DISEASE_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_QUALITY_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_MARKET_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_FOOD_LOSS_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_FINANCE_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_WEATHER_RELATED_COL + " TEXT, "
//                + CIG_PRODUCER_DAIRY_CATTLE_COL + " INTEGER, "
//                + CIG_PRODUCER_BEEF_CATTLE_COL + " INTEGER, "
//                + CIG_PRODUCER_SHEEP_COL + " INTEGER, "
//                + CIG_PRODUCER_POULTRY_COL + " INTEGER, "
//                + CIG_PRODUCER_PIGS_COL + " INTEGER, "
//                + CIG_PRODUCER_RABBITS_COL + " INTEGER, "
//                + CIG_PRODUCER_BEEHIVES_COL + " INTEGER, "
//                + CIG_PRODUCER_DONKEYS_COL + " INTEGER, "
//                + CIG_PRODUCER_GOATS_COL + " INTEGER, "
//                + CIG_PRODUCER_CAMELS_COL + " INTEGER, "
//                + CIG_PRODUCER_AQUACULTURE_COL + " INTEGER, "
//                + CIG_PRODUCER_HOUSING_TYPE_COL + " TEXT, "
//                + CIG_PRODUCER_HOUSING_FLOOR_COL + " TEXT, "
//                + CIG_PRODUCER_HOUSING_ROOF_COL + " TEXT, "
//                + CIG_PRODUCER_LIGHTING_FUEL_COL + " TEXT, "
//                + CIG_PRODUCER_COOKING_FUEL_COL + " TEXT, "
//                + CIG_PRODUCER_WATER_FILTER_COL + " TEXT, "
//                + CIG_PRODUCER_WATER_TANK_GREATER_THAN_5000LTS_COL + " TEXT, "
//                + CIG_PRODUCER_HAND_WASHING_FACILITIES_COL + " TEXT, "
//                + CIG_PRODUCER_PPES_COL + " TEXT, "
//                + CIG_PRODUCER_WATER_WELL_OR_WEIR_COL + " TEXT, "
//                + CIG_PRODUCER_IRRIGATION_PUMP_COL + " TEXT, "
//                + CIG_PRODUCER_HARVESTING_EQUIPMENT_COL + " TEXT, "
//                + CIG_PRODUCER_TRANSPORTATION_TYPE_COL + " TEXT, "
//                + CIG_PRODUCER_TOILET_FLOOR_COL + " TEXT, "
//                + CIG_PRODUCER_USER_APPROVED_COL + " TEXT, "
//                + CIG_PRODUCER_TA_COL + " TEXT, "
//                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
//                + CIG_PRODUCER_USER_ID_COL + " TEXT)")

//        // Create CigFarmerFieldRegistration Table
//        val createCigFarmerFieldRegistrationTable = ("CREATE TABLE " + CIG_FARMER_FIELD_REGISTRATION_TABLE_NAME + " ("
//                + CIG_FARMER_FIELD_REGISTRATION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//                + CIG_FARMER_FIELD_PRODUCER_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_FIELD_NUMBER_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_FIELD_SIZE_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_CROP1_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_CROP_VARIETY1_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_DATE_PLANTED1_COL + " DATETIME, "
//                + CIG_FARMER_FIELD_DATE_OF_HARVEST1_COL + " DATETIME, "
//                + CIG_FARMER_FIELD_POPULATION1_COL + " INTEGER NOT NULL, "
//                + CIG_FARMER_FIELD_BASELINE_YIELD_LAST_SEASON1_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_BASELINE_INCOME_LAST_SEASON1_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON1_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_CROP2_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_CROP_VARIETY2_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_DATE_PLANTED2_COL + " DATETIME, "
//                + CIG_FARMER_FIELD_DATE_OF_HARVEST2_COL + " DATETIME, "
//                + CIG_FARMER_FIELD_POPULATION2_COL + " INTEGER NOT NULL, "
//                + CIG_FARMER_FIELD_BASELINE_YIELD_LAST_SEASON2_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_BASELINE_INCOME_LAST_SEASON2_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON2_COL + " TEXT NOT NULL, "
//                + CIG_FARMER_FIELD_PRODUCER_BIODATA_ID_FK_COL + " INTEGER, "
//                + CIG_FARMER_FIELD_USER_ID_FK_COL + " TEXT, "
//                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
//                + "FOREIGN KEY(" + CIG_FARMER_FIELD_PRODUCER_BIODATA_ID_FK_COL + ") REFERENCES " + CIG_PRODUCER_BIODATA_TABLE_NAME + "(" + CIG_PRODUCER_BIODATA_ID_COL + "), "
//                + "FOREIGN KEY(" + CIG_FARMER_FIELD_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create SeasonPlanning Table
        val createSeasonPlanningTable = ("CREATE TABLE " + SEASON_PLANNING_TABLE_NAME + " ("
                + SEASON_PLANNING_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SEASON_PLANNING_PRODUCER_COL + " TEXT NOT NULL, "
                + SEASON_PLANNING_FIELD_COL + " TEXT NOT NULL, "
                + SEASON_PLANNING_PLANNED_DATE_OF_PLANTING_COL + " DATETIME, "
                + SEASON_PLANNING_WEEK_NUMBER_COL + " INTEGER NOT NULL, "
                + SEASON_PLANNING_NURSERY_COL + " TEXT, "
                + SEASON_PLANNING_GAPPING_COL + " TEXT, "
                + SEASON_PLANNING_SOIL_ANALYSIS_COL + " TEXT, "
                + SEASON_PLANNING_LIMING_COL + " TEXT, "
                + SEASON_PLANNING_TRANSPLANTING_COL + " TEXT, "
                + SEASON_PLANNING_WEEDING_COL + " TEXT, "
                + SEASON_PLANNING_PRUNNING_THINNING_DESUCKERING_COL + " TEXT, "
                + SEASON_PLANNING_MULCHING_COL + " TEXT, "
                + SEASON_PLANNING_HARVESTING_COL + " TEXT, "
                + SEASON_PLANNING_USER_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + SEASON_PLANNING_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create MarketProduce Table
        val createMarketProduceTable = ("CREATE TABLE " + MARKET_PRODUCE_TABLE_NAME + " ("
                + MARKET_PRODUCE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MARKET_PRODUCE_PRODUCT_COL + " TEXT, "
                + MARKET_PRODUCE_PRODUCT_CATEGORY_COL + " TEXT, "
                + MARKET_PRODUCE_ACERAGE_COL + " TEXT, "
                + MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL + " INTEGER, "
                + MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL + ") REFERENCES " + SEASON_PLANNING_TABLE_NAME + "(" + SEASON_PLANNING_ID_COL + "), "
                + "FOREIGN KEY(" + MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL + ") REFERENCES " + EXTENSION_SERVICE_TABLE_NAME + "(" + EXTENSION_SERVICE_ID_COL + "))")

        // Create PlanNutrition Table
        val createPlanNutritionTable = ("CREATE TABLE " + PLAN_NUTRITION_TABLE_NAME + " ("
                + PLAN_NUTRITION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PLAN_NUTRITION_PRODUCT_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_PRODUCT_NAME_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_UNIT_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_COST_PER_UNIT_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_APPLICATION_RATE_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_TIME_OF_APPLICATION_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_METHOD_OF_APPLICATION_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_PRODUCT_FORMULATION_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_DATE_OF_APPLICATION_COL + " DATETIME, "
                + PLAN_NUTRITION_TOTAL_MIXING_RATIO_COL + " TEXT NOT NULL, "
                + PLAN_NUTRITION_SEASON_PLANNING_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + PLAN_NUTRITION_SEASON_PLANNING_ID_FK_COL + ") REFERENCES " + SEASON_PLANNING_TABLE_NAME + "(" + SEASON_PLANNING_ID_COL + "))")

        // Create ScoutingStation Table
        val createScoutingStationTable = ("CREATE TABLE " + SCOUTING_STATION_TABLE_NAME + " ("
                + SCOUTING_STATION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SCOUTING_STATION_BAIT_STATION_COL + " TEXT NOT NULL, "
                + SCOUTING_STATION_TYPE_OF_BAIT_PROVIDED_COL + " TEXT NOT NULL, "
                + SCOUTING_STATION_FREQUENCY_COL + " TEXT NOT NULL, "
                + SCOUTING_STATION_SEASON_PLANNING_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + SCOUTING_STATION_SEASON_PLANNING_ID_FK_COL + ") REFERENCES " + SEASON_PLANNING_TABLE_NAME + "(" + SEASON_PLANNING_ID_COL + "))")

        // Create PreventativeDisease Table
        val createPreventativeDiseaseTable = ("CREATE TABLE " + PREVENTATIVE_DISEASE_TABLE_NAME + " ("
                + PREVENTATIVE_DISEASE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PREVENTATIVE_DISEASE_DISEASE_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_PRODUCT_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_CATEGORY_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_FORMULATION_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_DOSAGE_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_UNIT_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_COST_PER_UNIT_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_VOLUME_OF_WATER_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_FREQUENCY_OF_APPLICATION_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_TOTAL_COST_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_DISEASE_SEASON_PLANNING_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + PREVENTATIVE_DISEASE_SEASON_PLANNING_ID_FK_COL + ") REFERENCES " + SEASON_PLANNING_TABLE_NAME + "(" + SEASON_PLANNING_ID_COL + "))")

        // Create PreventativePest Table
        val createPreventativePestTable = ("CREATE TABLE " + PREVENTATIVE_PEST_TABLE_NAME + " ("
                + PREVENTATIVE_PEST_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PREVENTATIVE_PEST_PEST_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_PRODUCT_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_CATEGORY_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_FORMULATION_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_DOSAGE_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_UNIT_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_COST_PER_UNIT_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_VOLUME_OF_WATER_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_FREQUENCY_OF_APPLICATION_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_TOTAL_COST_COL + " TEXT NOT NULL, "
                + PREVENTATIVE_PEST_SEASON_PLANNING_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + PREVENTATIVE_PEST_SEASON_PLANNING_ID_FK_COL + ") REFERENCES " + SEASON_PLANNING_TABLE_NAME + "(" + SEASON_PLANNING_ID_COL + "))")

        // Create PlanIrrigation Table
        val createPlanIrrigationTable = ("CREATE TABLE " + PLAN_IRRIGATION_TABLE_NAME + " ("
                + PLAN_IRRIGATION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PLAN_IRRIGATION_TYPE_OF_IRRIGATION_COL + " TEXT NOT NULL, "
                + PLAN_IRRIGATION_DISCHARGE_HOURS_COL + " TEXT NOT NULL, "
                + PLAN_IRRIGATION_FREQUENCY_COL + " TEXT NOT NULL, "
                + PLAN_IRRIGATION_COST_OF_FUEL_COL + " TEXT NOT NULL, "
                + PLAN_IRRIGATION_UNIT_COST_COL + " TEXT NOT NULL, "
                + PLAN_IRRIGATION_SEASON_PLANNING_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + PLAN_IRRIGATION_SEASON_PLANNING_ID_FK_COL + ") REFERENCES " + SEASON_PLANNING_TABLE_NAME + "(" + SEASON_PLANNING_ID_COL + "))")

        // Create ExtensionService Table
        val createExtensionServiceTable = ("CREATE TABLE " + EXTENSION_SERVICE_TABLE_NAME + " ("
                + EXTENSION_SERVICE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + EXTENSION_SERVICE_PRODUCER_COL + " TEXT NOT NULL, "
                + EXTENSION_SERVICE_FIELD_COL + " TEXT NOT NULL, "
                + EXTENSION_SERVICE_PLANNED_DATE_OF_PLANTING_COL + " DATETIME, "
                + EXTENSION_SERVICE_WEEK_NUMBER_COL + " INTEGER, "
                + EXTENSION_SERVICE_NURSERY_COL + " TEXT, "
                + EXTENSION_SERVICE_GAPPING_COL + " TEXT, "
                + EXTENSION_SERVICE_SOIL_ANALYSIS_COL + " TEXT, "
                + EXTENSION_SERVICE_LIMING_COL + " TEXT, "
                + EXTENSION_SERVICE_TRANSPLANTING_COL + " TEXT, "
                + EXTENSION_SERVICE_WEEDING_COL + " TEXT, "
                + EXTENSION_SERVICE_PRUNNING_THINNING_DESUCKERING_COL + " TEXT, "
                + EXTENSION_SERVICE_MULCHING_COL + " TEXT, "
                + EXTENSION_SERVICE_HARVESTING_COL + " TEXT, "
                + EXTENSION_SERVICE_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + EXTENSION_SERVICE_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create ExtScoutingStation Table
        val createExtScoutingStationTable = ("CREATE TABLE " + EXT_SCOUTING_STATION_TABLE_NAME + " ("
                + EXT_SCOUTING_STATION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + EXT_SCOUTING_STATION_SCOUTING_METHOD_COL + " TEXT, "
                + EXT_SCOUTING_STATION_BAIT_STATION_COL + " TEXT, "
                + EXT_SCOUTING_STATION_PEST_OR_DISEASE_COL + " TEXT, "
                + EXT_SCOUTING_STATION_MANAGEMENT_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + EXT_SCOUTING_STATION_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + " INTEGER, "
                + "FOREIGN KEY(" + EXT_SCOUTING_STATION_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + ") REFERENCES " + EXTENSION_SERVICE_TABLE_NAME + "(" + EXTENSION_SERVICE_ID_COL + "))")

        // Create PesticideUsed Table
        val createPesticideUsedTable = ("CREATE TABLE " + PESTICIDE_USED_TABLE_NAME + " ("
                + PESTICIDE_USED_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PESTICIDE_USED_REGISTER_COL + " TEXT, "
                + PESTICIDE_USED_PRODUCT_COL + " TEXT, "
                + PESTICIDE_USED_CATEGORY_COL + " TEXT, "
                + PESTICIDE_USED_FORMULATION_COL + " TEXT, "
                + PESTICIDE_USED_DOSAGE_COL + " TEXT, "
                + PESTICIDE_USED_UNIT_COL + " TEXT, "
                + PESTICIDE_USED_COST_PER_UNIT_COL + " TEXT, "
                + PESTICIDE_USED_VOLUME_OF_WATER_COL + " TEXT, "
                + PESTICIDE_USED_FREQUENCY_OF_APPLICATION_COL + " TEXT, "
                + PESTICIDE_USED_TOTAL_COST_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + PESTICIDE_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + " INTEGER, "
                + "FOREIGN KEY(" + PESTICIDE_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + ") REFERENCES " + EXTENSION_SERVICE_TABLE_NAME + "(" + EXTENSION_SERVICE_ID_COL + "))")

        // Create FertilizerUsed Table
        val createFertilizerUsedTable = ("CREATE TABLE " + FERTILIZER_USED_TABLE_NAME + " ("
                + FERTILIZER_USED_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FERTILIZER_USED_REGISTER_COL + " TEXT, "
                + FERTILIZER_USED_PRODUCT_COL + " TEXT, "
                + FERTILIZER_USED_CATEGORY_COL + " TEXT, "
                + FERTILIZER_USED_FORMULATION_COL + " TEXT, "
                + FERTILIZER_USED_DOSAGE_COL + " TEXT, "
                + FERTILIZER_USED_UNIT_COL + " TEXT, "
                + FERTILIZER_USED_COST_PER_UNIT_COL + " TEXT, "
                + FERTILIZER_USED_VOLUME_OF_WATER_COL + " TEXT, "
                + FERTILIZER_USED_FREQUENCY_OF_APPLICATION_COL + " TEXT, "
                + FERTILIZER_USED_TOTAL_COST_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + FERTILIZER_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + " INTEGER, "
                + "FOREIGN KEY(" + FERTILIZER_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + ") REFERENCES " + EXTENSION_SERVICE_TABLE_NAME + "(" + EXTENSION_SERVICE_ID_COL + "))")

        // Create ForecastYield Table
        val createForecastYieldTable = ("CREATE TABLE " + FORECAST_YIELD_TABLE_NAME + " ("
                + FORECAST_YIELD_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FORECAST_YIELD_CROP_POPULATION_PC_COL + " TEXT, "
                + FORECAST_YIELD_YIELD_FORECAST_PC_COL + " TEXT, "
                + FORECAST_YIELD_FORECAST_QUALITY_COL + " TEXT, "
                + FORECAST_YIELD_TA_COMMENTS_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + FORECAST_YIELD_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + " INTEGER, "
                + "FOREIGN KEY(" + FORECAST_YIELD_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL + ") REFERENCES " + EXTENSION_SERVICE_TABLE_NAME + "(" + EXTENSION_SERVICE_ID_COL + "))")

        // Create Training Table
        val createTrainingTable = ("CREATE TABLE " + TRAINING_TABLE_NAME + " ("
                + TRAINING_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TRAINING_COURSE_NAME_COL + " TEXT NOT NULL, "
                + TRAINING_TRAINER_NAME_COL + " TEXT NOT NULL, "
                + TRAINING_BUYING_CENTER_COL + " TEXT NOT NULL, "
                + TRAINING_COURSE_DESCRIPTION_COL + " TEXT NOT NULL, "
                + TRAINING_DATE_OF_TRAINING_COL + " DATETIME NOT NULL, "
                + TRAINING_CONTENT_OF_TRAINING_COL + " TEXT NOT NULL, "
                + TRAINING_VENUE_COL + " TEXT NOT NULL, "
                + TRAINING_PARTICIPANTS_COL + " INTEGER NOT NULL, "
                + TRAINING_USER_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + TRAINING_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create Attendance Table
        val createAttendanceTable = ("CREATE TABLE " + ATTENDANCE_TABLE_NAME + " ("
                + ATTENDANCE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ATTENDANCE_ATTENDANCE_COL + " TEXT NOT NULL, "
                + ATTENDANCE_TRAINING_ID_COL + " INTEGER, "
                + ATTENDANCE_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + ATTENDANCE_TRAINING_ID_COL + ") REFERENCES " + TRAINING_TABLE_NAME + "(" + TRAINING_ID_COL + "), "
                + "FOREIGN KEY(" + ATTENDANCE_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create FarmerPriceDistribution Table
        val createFarmerPriceDistributionTable = ("CREATE TABLE " + FARMER_PRICE_DISTRIBUTION_TABLE_NAME + " ("
                + FARMER_PRICE_DISTRIBUTION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FARMER_PRICE_DISTRIBUTION_HUB_COL + " TEXT NOT NULL, "
                + FARMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL + " TEXT NOT NULL, "
                + FARMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL + " REAL NOT NULL, "
                + FARMER_PRICE_DISTRIBUTION_UNIT_COL + " TEXT NOT NULL, "
                + FARMER_PRICE_DISTRIBUTION_DATE_COL + " DATETIME NOT NULL, "
                + FARMER_PRICE_DISTRIBUTION_COMMENTS_COL + " TEXT, "
                + FARMER_PRICE_DISTRIBUTION_SOLD_COL + " INTEGER NOT NULL, "
                + FARMER_PRICE_DISTRIBUTION_USER_ID_FK_COL + " TEXT, "
                + FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + FARMER_PRICE_DISTRIBUTION_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "), "
                + "FOREIGN KEY(" + FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL + ") REFERENCES " + MARKET_PRODUCE_TABLE_NAME + "(" + MARKET_PRODUCE_ID_COL + "))")

        // Create CustomerPriceDistribution Table
        val createCustomerPriceDistributionTable = ("CREATE TABLE " + CUSTOMER_PRICE_DISTRIBUTION_TABLE_NAME + " ("
                + CUSTOMER_PRICE_DISTRIBUTION_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + CUSTOMER_PRICE_DISTRIBUTION_HUB_COL + " TEXT NOT NULL, "
                + CUSTOMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL + " TEXT NOT NULL, "
                + CUSTOMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL + " REAL NOT NULL, "
                + CUSTOMER_PRICE_DISTRIBUTION_UNIT_COL + " TEXT NOT NULL, "
                + CUSTOMER_PRICE_DISTRIBUTION_DATE_COL + " DATETIME NOT NULL, "
                + CUSTOMER_PRICE_DISTRIBUTION_COMMENTS_COL + " TEXT, "
                + CUSTOMER_PRICE_DISTRIBUTION_SOLD_COL + " INTEGER NOT NULL, "
                + CUSTOMER_PRICE_DISTRIBUTION_USER_ID_FK_COL + " TEXT, "
                + CUSTOMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + CUSTOMER_PRICE_DISTRIBUTION_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "), "
                + "FOREIGN KEY(" + CUSTOMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL + ") REFERENCES " + MARKET_PRODUCE_TABLE_NAME + "(" + MARKET_PRODUCE_ID_COL + "))")

        // Create BuyingFarmer Table
        val createBuyingFarmerTable = ("CREATE TABLE " + BUYING_FARMER_TABLE_NAME + " ("
                + BUYING_FARMER_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + BUYING_FARMER_BUYING_CENTER_COL + " TEXT NOT NULL, "
                + BUYING_FARMER_PRODUCER_COL + " TEXT NOT NULL, "
                + BUYING_FARMER_PRODUCE_COL + " TEXT NOT NULL, "
                + BUYING_FARMER_GRN_NUMBER_COL + " TEXT NOT NULL, "
                + BUYING_FARMER_UNIT_COL + " TEXT NOT NULL, "
                + BUYING_FARMER_QUALITY_COL + " TEXT NOT NULL, "
                + "`" + BUYING_FARMER_ACTION_COL + "` TEXT NOT NULL, "
                + BUYING_FARMER_WEIGHT_COL + " REAL NOT NULL, "
                + BUYING_FARMER_LOADED_COL + " INTEGER NOT NULL, "
                + BUYING_FARMER_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + BUYING_FARMER_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create Quarantine Table
        val createQuarantineTable = ("CREATE TABLE " + QUARANTINE_TABLE_NAME + " ("
                + QUARANTINE_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "`" + QUARANTINE_ACTION_COL + "` TEXT NOT NULL, "
                + QUARANTINE_APPROVED_BY_COL + " TEXT , "
                + QUARANTINE_NEW_WEIGHT_IN_COL + " TEXT NOT NULL, "
                + QUARANTINE_NEW_WEIGHT_OUT_COL + " TEXT NOT NULL, "
                + QUARANTINE_BUYING_FARMER_ID_FK_COL + " INTEGER, "
                + QUARANTINE_BUYING_CUSTOMER_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + QUARANTINE_BUYING_FARMER_ID_FK_COL + ") REFERENCES " + BUYING_FARMER_TABLE_NAME + "(" + BUYING_FARMER_ID_COL + "), "
                + "FOREIGN KEY(" + QUARANTINE_BUYING_CUSTOMER_ID_FK_COL + ") REFERENCES " + BUYING_CUSTOMER_TABLE_NAME + "(" + BUYING_CUSTOMER_ID_COL + "))")

        // Create BuyingCustomer Table
        val createBuyingCustomerTable = ("CREATE TABLE " + BUYING_CUSTOMER_TABLE_NAME + " ("
                + BUYING_CUSTOMER_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + BUYING_CUSTOMER_PRODUCE_COL + " TEXT NOT NULL, "
                + BUYING_CUSTOMER_CUSTOMER_COL + " TEXT NOT NULL, "
                + BUYING_CUSTOMER_GRN_NUMBER_COL + " TEXT NOT NULL, "
                + BUYING_CUSTOMER_UNIT_COL + " TEXT NOT NULL, "
                + BUYING_CUSTOMER_QUALITY_COL + " TEXT NOT NULL, "
                + "`" + BUYING_CUSTOMER_ACTION_COL + "` TEXT NOT NULL, "
                + BUYING_CUSTOMER_WEIGHT_COL + " REAL NOT NULL, "
                + BUYING_CUSTOMER_ONLINE_PRICE_COL + " REAL NOT NULL, "
                + BUYING_CUSTOMER_LOADED_COL + " INTEGER NOT NULL, "
                + BUYING_CUSTOMER_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + BUYING_CUSTOMER_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create PaymentFarmer Table
        val createPaymentFarmerTable = ("CREATE TABLE " + PAYMENT_FARMER_TABLE_NAME + " ("
                + PAYMENT_FARMER_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PAYMENT_FARMER_BUYING_CENTER_COL + " TEXT NOT NULL, "
                + PAYMENT_FARMER_CIG_COL + " TEXT NOT NULL, "
                + PAYMENT_FARMER_PRODUCER_COL + " TEXT NOT NULL, "
                + PAYMENT_FARMER_GRN_COL + " TEXT NOT NULL, "
                + PAYMENT_FARMER_NET_BALANCE_COL + " REAL NOT NULL, "
                + PAYMENT_FARMER_PAYMENT_TYPE_COL + " TEXT NOT NULL, "
                + PAYMENT_FARMER_OUTSTANDING_LOAN_AMOUNT_COL + " REAL NOT NULL, "
                + PAYMENT_FARMER_PAYMENT_DUE_COL + " REAL NOT NULL, "
                + PAYMENT_FARMER_SET_LOAN_DEDUCTION_COL + " REAL NOT NULL, "
                + PAYMENT_FARMER_NET_BALANCE_BEFORE_COL + " REAL NOT NULL, "
                + PAYMENT_FARMER_NET_BALANCE_AFTER_LOAN_DEDUCTION_COL + " REAL NOT NULL, "
                + PAYMENT_FARMER_COMMENT_COL + " TEXT, "
                + PAYMENT_FARMER_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + PAYMENT_FARMER_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create PaymentCustomer Table
        val createPaymentCustomerTable = ("CREATE TABLE " + PAYMENT_CUSTOMER_TABLE_NAME + " ("
                + PAYMENT_CUSTOMER_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PAYMENT_CUSTOMER_VILLAGE_OR_ESTATE_COL + " TEXT NOT NULL, "
                + PAYMENT_CUSTOMER_CUSTOMER_COL + " TEXT NOT NULL, "
                + PAYMENT_CUSTOMER_GRN_COL + " TEXT NOT NULL, "
                + PAYMENT_CUSTOMER_AMOUNT_COL + " REAL NOT NULL, "
                + PAYMENT_CUSTOMER_NET_BALANCE_COL + " REAL NOT NULL, "
                + PAYMENT_CUSTOMER_PAYMENT_TYPE_COL + " TEXT NOT NULL, "
                + PAYMENT_CUSTOMER_ENTER_AMOUNT_COL + " REAL NOT NULL, "
                + PAYMENT_CUSTOMER_NET_BALANCE_BEFORE_COL + " REAL NOT NULL, "
                + PAYMENT_CUSTOMER_NET_BALANCE_AFTER_COL + " REAL NOT NULL, "
                + PAYMENT_CUSTOMER_COMMENT_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + PAYMENT_CUSTOMER_USER_ID_FK_COL + " TEXT, "
                + "FOREIGN KEY(" + PAYMENT_CUSTOMER_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")
        // Create PlanJourney Table
        val createPlanJourneyTable = ("CREATE TABLE " + PLAN_JOURNEY_TABLE_NAME + " ("
                + PLAN_JOURNEY_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PLAN_JOURNEY_TRUCK_COL + " TEXT NOT NULL, "
                + PLAN_JOURNEY_DRIVER_COL + " TEXT NOT NULL, "
                + PLAN_JOURNEY_STARTING_MILEAGE_COL + " TEXT NOT NULL, "
                + PLAN_JOURNEY_STARTING_FUEL_COL + " TEXT NOT NULL, "
                + PLAN_JOURNEY_START_LOCATION_COL + " TEXT NOT NULL, "
                + PLAN_JOURNEY_DOCUMENTATION_COL + " TEXT, "
                + PLAN_JOURNEY_STOP_POINTS_COL + " TEXT, "
                + PLAN_JOURNEY_FINAL_DESTINATION_COL + " TEXT NOT NULL, "
                + PLAN_JOURNEY_DATE_AND_TIME_COL + " TEXT NOT NULL, "
                + PLAN_JOURNEY_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + PLAN_JOURNEY_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create DispatchInput Table
        val createDispatchInputTable = ("CREATE TABLE " + DISPATCH_INPUT_TABLE_NAME + " ("
                + DISPATCH_INPUT_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DISPATCH_INPUT_GRN_COL + " TEXT NOT NULL, "
                + DISPATCH_INPUT_INPUT_COL + " TEXT NOT NULL, "
                + DISPATCH_INPUT_DESCRIPTION_COL + " TEXT, "
                + DISPATCH_INPUT_NUMBER_OF_UNITS_COL + " INTEGER NOT NULL, "
                + DISPATCH_INPUT_PLAN_JOURNEY_ID_FK_COL + " INTEGER, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + DISPATCH_INPUT_PLAN_JOURNEY_ID_FK_COL + ") REFERENCES " + PLAN_JOURNEY_TABLE_NAME + "(" + PLAN_JOURNEY_ID_COL + "))")

        // Create Loading Table
        val createLoadingTable = ("CREATE TABLE " + LOADING_TABLE_NAME + " ("
                + LOADING_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + LOADING_GRN_COL + " TEXT NOT NULL, "
                + LOADING_TOTAL_WEIGHT_COL + " TEXT NOT NULL, "
                + LOADING_TRUCK_LOADING_NUMBER_COL + " TEXT NOT NULL, "
                + LOADING_FROM_COL + " TEXT NOT NULL, "
                + "`" + LOADING_TO_COL + "` TEXT NOT NULL, "
                + LOADING_COMMENT_COL + " TEXT, "
                + LOADING_OFFLOADED_COL + " INTEGER NOT NULL, "
                + LOADING_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + LOADING_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create Offloading Table
        val createOffloadingTable = ("CREATE TABLE " + OFFLOADING_TABLE_NAME + " ("
                + OFFLOADING_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + OFFLOADING_OFFLOADED_LOAD_COL + " TEXT NOT NULL, "
                + OFFLOADING_TOTAL_WEIGHT_COL + " REAL NOT NULL, "
                + OFFLOADING_TRUCK_OFFLOADING_NUMBER_COL + " TEXT NOT NULL, "
                + OFFLOADING_COMMENT_COL + " TEXT, "
                + OFFLOADING_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + OFFLOADING_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        // Create RuralWorker Table
        val createRuralWorkerTable = ("CREATE TABLE " + RURAL_WORKER_TABLE_NAME + " ("
                + RURAL_WORKER_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + RURAL_WORKER_OTHER_NAME_COL + " TEXT, "
                + RURAL_WORKER_LAST_NAME_COL + " TEXT NOT NULL, "
                + RURAL_WORKER_RURAL_WORKER_CODE_COL + " TEXT NOT NULL, "
                + RURAL_WORKER_ID_NUMBER_COL + " TEXT NOT NULL, "
                + RURAL_WORKER_GENDER_COL + " TEXT NOT NULL, "
                + RURAL_WORKER_DATE_OF_BIRTH_COL + " TEXT NOT NULL, "
                + RURAL_WORKER_EMAIL_COL + " TEXT, "
                + RURAL_WORKER_PHONE_NUMBER_COL + " TEXT NOT NULL, "
                + RURAL_WORKER_EDUCATION_LEVEL_COL + " TEXT, "
                + RURAL_WORKER_SERVICE_COL + " TEXT, "
                + RURAL_WORKER_OTHER_COL + " TEXT, "
                + RURAL_WORKER_COUNTY_COL + " TEXT, "
                + RURAL_WORKER_SUB_COUNTY_COL + " TEXT, "
                + RURAL_WORKER_WARD_COL + " TEXT, "
                + RURAL_WORKER_VILLAGE_COL + " TEXT, "
                + RURAL_WORKER_USER_ID_FK_COL + " TEXT, "
                + IS_OFFLINE_COL + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + RURAL_WORKER_USER_ID_FK_COL + ") REFERENCES " + usersTableName + "(" + idCol + "))")

        db.execSQL(createCIGTable)
        db.execSQL(createHubsTable)
        db.execSQL(createMembersTable)
        db.execSQL(createUsersTable)
        db.execSQL(createKeyContactsTable)
        db.execSQL(createBuyingCenterTable)
        db.execSQL(createCustomUserTable)
        db.execSQL(createHubUserTable)
        db.execSQL(createHQUserTable)
        db.execSQL(createProcessingUserTable)
        db.execSQL(createIndividualLogisticianUserTable)
        db.execSQL(createOrganisationLogisticianUserTable)
        db.execSQL(createCarTable)
        db.execSQL(createIndividualCustomerUserTable)
        db.execSQL(createOrganisationCustomerUserTable)
        db.execSQL(createProductTable)
        db.execSQL(createCommercialProduceTable)
        db.execSQL(createDomesticProduceTable)
//        db.execSQL(createFarmerFieldRegistrationTable)
//
//        db.execSQL(createCigFarmerFieldRegistrationTable)
        db.execSQL(createSeasonPlanningTable)
        db.execSQL(createMarketProduceTable)
        db.execSQL(createPlanNutritionTable)
        db.execSQL(createScoutingStationTable)
        db.execSQL(createPreventativeDiseaseTable)
        db.execSQL(createPreventativePestTable)
        db.execSQL(createPlanIrrigationTable)
        db.execSQL(createExtensionServiceTable)
        db.execSQL(createExtScoutingStationTable)
        db.execSQL(createPesticideUsedTable)
        db.execSQL(createFertilizerUsedTable)
        db.execSQL(createForecastYieldTable)
        db.execSQL(createTrainingTable)
        db.execSQL(createAttendanceTable)
        db.execSQL(createFarmerPriceDistributionTable)
        db.execSQL(createCustomerPriceDistributionTable)
        db.execSQL(createBuyingFarmerTable)
        db.execSQL(createQuarantineTable)
        db.execSQL(createBuyingCustomerTable)
        db.execSQL(createPaymentFarmerTable)
        db.execSQL(createPaymentCustomerTable)
        db.execSQL(createPlanJourneyTable)
        db.execSQL(createDispatchInputTable)
        db.execSQL(createLoadingTable)
        db.execSQL(createOffloadingTable)
        db.execSQL(createRuralWorkerTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + usersTableName)
        db.execSQL("DROP TABLE IF EXISTS " + HUB_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + KEY_CONTACTS_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + BUYING_CENTER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + CIG_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + MEMBERS_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + CUSTOM_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + HUB_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + HQ_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PROCESSING_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + INDIVIDUAL_LOGISTICIAN_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + ORGANISATION_LOGISTICIAN_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + CAR_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + INDIVIDUAL_CUSTOMER_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + ORGANISATION_CUSTOMER_USER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PRODUCT_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PRODUCER_BIODATA_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + COMMERCIAL_PRODUCE_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + DOMESTIC_PRODUCE_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + FARMER_FIELD_REGISTRATION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + CIG_PRODUCER_BIODATA_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + CIG_FARMER_FIELD_REGISTRATION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + SEASON_PLANNING_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + MARKET_PRODUCE_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PLAN_NUTRITION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + SCOUTING_STATION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PREVENTATIVE_DISEASE_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PREVENTATIVE_PEST_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PLAN_IRRIGATION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + EXTENSION_SERVICE_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + EXT_SCOUTING_STATION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PESTICIDE_USED_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + FERTILIZER_USED_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + FORECAST_YIELD_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + TRAINING_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + ATTENDANCE_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + FARMER_PRICE_DISTRIBUTION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + CUSTOMER_PRICE_DISTRIBUTION_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + BUYING_FARMER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + QUARANTINE_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + BUYING_CUSTOMER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PAYMENT_FARMER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PAYMENT_CUSTOMER_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + PLAN_JOURNEY_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + DISPATCH_INPUT_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + LOADING_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + OFFLOADING_TABLE_NAME)
        db.execSQL("DROP TABLE IF EXISTS " + RURAL_WORKER_TABLE_NAME)

        onCreate(db)
    }

    // Insert key contacts method
    fun insertKeyContact(keyContact: KeyContact): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(KEY_CONTACT_OTHER_NAME_COL, keyContact.other_name)
        values.put(KEY_CONTACT_LAST_NAME_COL, keyContact.last_name)
        values.put(ID_NUMBER_COL, keyContact.id_number)
        values.put(GENDER_COL, keyContact.gender)
        values.put(KEY_CONTACT_ROLE_COL, keyContact.role)
        values.put(DATE_OF_BIRTH_COL, keyContact.date_of_birth)
        values.put(KEY_CONTACT_EMAIL_COL, keyContact.email)
        values.put(PHONE_NUMBER_COL, keyContact.phone_number)
        values.put(HUB_ID_FK_COL, keyContact.hub_id)
        values.put(BUYING_CENTER_ID_FK_COL, keyContact.buying_center_id)
        values.put(IS_OFFLINE_COL, 1)

        // Log the values being inserted
        Log.d("DBHandler", "Inserting KeyContact with values: $values")

        val result = db.insert(KEY_CONTACTS_TABLE_NAME, null, values)

        // Log the result of the insert operation
        Log.d("DBHandler", "Insert result for KeyContact: $result")

        return result != -1L
    }

    // Insert Member
    fun insertMember(member: Member): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(MEMBER_OTHER_NAME_COL, member.other_name)
        values.put(MEMBER_LAST_NAME_COL, member.last_name)
        values.put(MEMBER_GENDER_COL, member.gender)
        values.put(MEMBER_DATE_OF_BIRTH_COL, member.date_of_birth)
        values.put(MEMBER_EMAIL_COL, member.email)
        values.put(MEMBER_PHONE_NUMBER_COL, member.phone_number)
        values.put(MEMBER_ID_NUMBER_COL, member.id_number)
        values.put(PRODUCT_INVOLVED_COL, member.product_involved)
        values.put(HECTORAGE_REGISTERED_UNDER_CIG_COL, member.hectorage_registered_under_cig)
        values.put(MEMBER_CIG_ID_COL, member.cig_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        // Log the values being inserted
        Log.d("DBHandler", "Inserting Member with values: $values")

        val result = db.insert(MEMBERS_TABLE_NAME, null, values)

        // Log the result of the insert operation
        Log.d("DBHandler", "Insert result for Member: $result")

        return result != -1L
    }

    // Insert HQ Users method
    fun insertHQUser(hqUser: HQUser): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(HQ_USER_OTHER_NAME_COL, hqUser.other_name)
        values.put(HQ_USER_LAST_NAME_COL, hqUser.last_name)
        values.put(HQ_USER_STAFF_CODE_COL, hqUser.staff_code)
        values.put(HQ_USER_DEPARTMENT_COL, hqUser.department)
        values.put(HQ_USER_ID_NUMBER_COL, hqUser.id_number)
        values.put(HQ_USER_GENDER_COL, hqUser.gender)
        values.put(HQ_USER_DATE_OF_BIRTH_COL, hqUser.date_of_birth)
        values.put(HQ_USER_EMAIL_COL, hqUser.email)
        values.put(HQ_USER_PHONE_NUMBER_COL, hqUser.phone_number)
        values.put(HQ_USER_EDUCATION_LEVEL_COL, hqUser.education_level)
        values.put(HQ_USER_ROLE_COL, hqUser.role)
        values.put(HQ_USER_REPORTING_TO_COL, hqUser.reporting_to)
        values.put(HQ_USER_RELATED_ROLES_COL, hqUser.related_roles)
        values.put(IS_OFFLINE_COL, 1)
        values.put(HQ_USER_USER_ID_COL, hqUser.user_id)

        val result = db.insert(HQ_USER_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert Hub users method
    fun insertHubUser(hubUser: HubUser): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(HUB_USER_OTHER_NAME_COL, hubUser.other_name)
        values.put(HUB_USER_LAST_NAME_COL, hubUser.last_name)
        values.put(HUB_USER_CODE_COL, hubUser.code)
        values.put(HUB_USER_ROLE_COL, hubUser.role)
        values.put(HUB_USER_ID_NUMBER_COL, hubUser.id_number)
        values.put(HUB_USER_GENDER_COL, hubUser.gender)
        values.put(HUB_USER_DATE_OF_BIRTH_COL, hubUser.date_of_birth)
        values.put(HUB_USER_EMAIL_COL, hubUser.email)
        values.put(HUB_USER_PHONE_NUMBER_COL, hubUser.phone_number)
        values.put(HUB_USER_EDUCATION_LEVEL_COL, hubUser.education_level)
        values.put(HUB_USER_HUB_COL, hubUser.hub)
        values.put(HUB_USER_BUYING_CENTER_COL, hubUser.buying_center)
        values.put(HUB_USER_COUNTY_COL, hubUser.county)
        values.put(HUB_USER_SUB_COUNTY_COL, hubUser.sub_county)
        values.put(HUB_USER_WARD_COL, hubUser.ward)
        values.put(HUB_USER_VILLAGE_COL, hubUser.village)
        values.put(IS_OFFLINE_COL, 1)
        values.put(HUB_USER_USER_ID_COL, hubUser.user_id)

        val result = db.insert(HUB_USER_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert Individual Logistician User
    fun insertIndividualLogistician(logistician: IndividualLogistician): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(INDIVIDUAL_LOGISTICIAN_USER_OTHER_NAME_COL, logistician.other_name)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_LAST_NAME_COL, logistician.last_name)
        values.put(LOGISTICIAN_CODE_COL, logistician.logistician_code)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_ID_NUMBER_COL, logistician.id_number)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_DATE_OF_BIRTH_COL, logistician.date_of_birth)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_EMAIL_COL, logistician.email)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_PHONE_NUMBER_COL, logistician.phone_number)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_ADDRESS_COL, logistician.address)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_HUB_COL, logistician.hub)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_REGION_COL, logistician.region)
        values.put(IS_OFFLINE_COL, 1)
        values.put(INDIVIDUAL_LOGISTICIAN_USER_USER_ID_COL, logistician.user_id)

        // Insert logistician data
        val logisticianResult = db.insert(INDIVIDUAL_LOGISTICIAN_USER_TABLE_NAME, null, values)
        if (logisticianResult == -1L) {
            return false
        }

        // Insert cars associated with this logistician
        val cars: List<Car>? = logistician.cars
        if (cars != null) {
            for (car in cars)  {
                car.individual_logistician_id = logisticianResult.toInt()
                val carInserted = insertCar(car)
                if (!carInserted) {
                    return false
                }
            }
        }

        return true
    }

    // Insert Car
    fun insertCar(car: Car): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(CAR_BODY_TYPE_COL, car.car_body_type)
        values.put(CAR_MODEL_COL, car.car_model)
        values.put(CAR_NUMBER_PLATE_COL, car.number_plate)
        values.put(CAR_DRIVER1_NAME_COL, car.driver1_name)
        values.put(CAR_DRIVER2_NAME_COL, car.driver2_name)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline
        values.put(INDIVIDUAL_LOGISTICIAN_ID_FK_COL, car.individual_logistician_id)
        values.put(ORGANISATION_LOGISTICIAN_ID_FK_COL, car.organisation_logistician_id)

        val result = db.insert(CAR_TABLE_NAME, null, values)
        db.close() // Ensure the database is closed after the operation
        return result != -1L
    }

    // Insert Organisational Logistician User
    fun insertOrganisationalLogistician(logistician: OrganisationalLogistician): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(ORGANISATION_LOGISTICIAN_USER_NAME_COL, logistician.name)
        values.put(ORGANISATION_LOGISTICIAN_USER_LOGISTICIAN_CODE_COL, logistician.logistician_code)
        values.put(ORGANISATION_LOGISTICIAN_USER_REGISTRATION_NUMBER_COL, logistician.registration_number)
        values.put(ORGANISATION_LOGISTICIAN_USER_DATE_OF_REGISTRATION_COL, logistician.date_of_registration)
        values.put(ORGANISATION_LOGISTICIAN_USER_EMAIL_COL, logistician.email)
        values.put(ORGANISATION_LOGISTICIAN_USER_PHONE_NUMBER_COL, logistician.phone_number)
        values.put(ORGANISATION_LOGISTICIAN_USER_ADDRESS_COL, logistician.address)
        values.put(ORGANISATION_LOGISTICIAN_USER_HUB_COL, logistician.hub)
        values.put(ORGANISATION_LOGISTICIAN_USER_REGION_COL, logistician.region)
        values.put(IS_OFFLINE_COL, 1)
        values.put(ORGANISATION_LOGISTICIAN_USER_USER_ID_COL, logistician.user_id)


        // Insert organisational logistician data
        val logisticianResult = db.insert(ORGANISATION_LOGISTICIAN_USER_TABLE_NAME, null, values)
        if (logisticianResult == -1L) {
            return false
        }

        // Insert cars associated with this logistician
        val cars: List<Car>? = logistician.cars
        if (cars != null) {
            for (car in cars) {
                car.organisation_logistician_id = logisticianResult.toInt()
                val carInserted = insertCar(car)
                if (!carInserted) {
                    return false
                }
            }
        }


        return true
    }

    // Insert Product
    fun insertProduct(product: Product): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(PRODUCT_CATEGORY_COL, product.category)
        values.put(PRODUCT_PRODUCTS_INTERESTED_IN_COL, product.products_interested_in)
        values.put(PRODUCT_VOLUME_IN_KGS_COL, product.volume_in_kgs)
        values.put(PRODUCT_PACKAGING_COL, product.packaging)
        values.put(PRODUCT_QUALITY_COL, product.quality)
        values.put(PRODUCT_FREQUENCY_COL, product.frequency)
        values.put(IS_OFFLINE_COL, 1)
        values.put(INDIVIDUAL_CUSTOMER_ID_FK_COL, product.individual_customer_id)
        values.put(ORGANISATION_CUSTOMER_ID_FK_COL, product.organisation_customer_id)

        val result = db.insert(PRODUCT_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert commercial produce
    fun insertCommercialProduce(produce: CommercialProduce): Boolean {

        val db = this.writableDatabase
        val values = ContentValues()

        values.put(COMMERCIAL_PRODUCE_PRODUCT_COL, produce.product)
        values.put(COMMERCIAL_PRODUCE_PRODUCT_CATEGORY_COL, produce.product_category)
        values.put(COMMERCIAL_PRODUCE_ACERAGE_COL, produce.acerage)
        values.put(COMMERCIAL_PRODUCER_BIODATA_ID_FK_COL, produce.producer_biodata_id)
        values.put(COMMERCIAL_CIG_PRODUCER_BIODATA_ID_FK_COL, produce.cig_producer_biodata_id)
        values.put(IS_OFFLINE_COL, 1)

        val result = db.insert(COMMERCIAL_PRODUCE_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert domestic produce
    fun insertDomesticProduce(produce: DomesticProduce): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(DOMESTIC_PRODUCE_PRODUCT_COL, produce.product)
        values.put(DOMESTIC_PRODUCE_PRODUCT_CATEGORY_COL, produce.product_category)
        values.put(DOMESTIC_PRODUCE_ACERAGE_COL, produce.acerage)
        values.put(DOMESTIC_PRODUCER_BIODATA_ID_FK_COL, produce.producer_biodata_id)
        values.put(DOMESTIC_CIG_PRODUCER_BIODATA_ID_FK_COL, produce.cig_producer_biodata_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(DOMESTIC_PRODUCE_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert CIG Producer biodata
    fun insertCIGProducerBiodata(producer: ProducerBiodata): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        // Insert Producer Biodata
        values.put(CIG_PRODUCER_OTHER_NAME_COL, producer.other_name)
        values.put(CIG_PRODUCER_LAST_NAME_COL, producer.last_name)
        values.put(CIG_PRODUCER_FARMER_CODE_COL, producer.farmer_code)
        values.put(CIG_PRODUCER_ID_NUMBER_COL, producer.id_number)
        values.put(CIG_PRODUCER_DATE_OF_BIRTH_COL, producer.date_of_birth)
        values.put(CIG_PRODUCER_EMAIL_COL, producer.email)
        values.put(CIG_PRODUCER_PHONE_NUMBER_COL, producer.phone_number)
        values.put(CIG_PRODUCER_HUB_COL, producer.hub)
        values.put(CIG_PRODUCER_BUYING_CENTER_COL, producer.buying_center)
        values.put(CIG_PRODUCER_GENDER_COL, producer.gender)
        values.put(CIG_PRODUCER_EDUCATION_LEVEL_COL, producer.education_level)
        values.put(CIG_PRODUCER_COUNTY_COL, producer.county)
        values.put(CIG_PRODUCER_SUB_COUNTY_COL, producer.sub_county)
        values.put(CIG_PRODUCER_WARD_COL, producer.ward)
        values.put(CIG_PRODUCER_VILLAGE_COL, producer.village)

        // Convert primary_producer list to JSON string
        val gson = Gson()
        val primaryProducerJson = gson.toJson(producer.primary_producer)
        values.put(CIG_PRODUCER_PRIMARY_PRODUCER_COL, primaryProducerJson)

        values.put(CIG_PRODUCER_TOTAL_LAND_SIZE_COL, producer.total_land_size)
        values.put(CIG_PRODUCER_CULTIVATE_LAND_SIZE_COL, producer.cultivate_land_size)
        values.put(CIG_PRODUCER_HOMESTEAD_SIZE_COL, producer.homestead_size)
        values.put(CIG_PRODUCER_UNCULTIVATED_LAND_SIZE_COL, producer.uncultivated_land_size)
        values.put(CIG_PRODUCER_FARM_ACCESSIBILITY_COL, producer.farm_accessibility)
        values.put(CIG_PRODUCER_NUMBER_OF_FAMILY_WORKERS_COL, producer.number_of_family_workers)
        values.put(CIG_PRODUCER_NUMBER_OF_HIRED_WORKERS_COL, producer.number_of_hired_workers)
        values.put(CIG_PRODUCER_ACCESS_TO_IRRIGATION_COL, producer.access_to_irrigation)
        values.put(CIG_PRODUCER_CROP_LIST_COL, producer.crop_list)
        values.put(CIG_PRODUCER_FARMER_INTEREST_IN_EXTENSION_COL, producer.farmer_interest_in_extension)
        values.put(CIG_PRODUCER_KNOWLEDGE_RELATED_COL, producer.knowledge_related)
        values.put(CIG_PRODUCER_SOIL_RELATED_COL, producer.soil_related)
        values.put(CIG_PRODUCER_COMPOST_RELATED_COL, producer.compost_related)
        values.put(CIG_PRODUCER_NUTRITION_RELATED_COL, producer.nutrition_related)
        values.put(CIG_PRODUCER_PESTS_RELATED_COL, producer.pests_related)
        values.put(CIG_PRODUCER_DISEASE_RELATED_COL, producer.disease_related)
        values.put(CIG_PRODUCER_QUALITY_RELATED_COL, producer.quality_related)
        values.put(CIG_PRODUCER_MARKET_RELATED_COL, producer.market_related)
        values.put(CIG_PRODUCER_FOOD_LOSS_RELATED_COL, producer.food_loss_related)
        values.put(CIG_PRODUCER_FINANCE_RELATED_COL, producer.finance_related)
        values.put(CIG_PRODUCER_WEATHER_RELATED_COL, producer.weather_related)
        values.put(CIG_PRODUCER_DAIRY_CATTLE_COL, producer.dairy_cattle)
        values.put(CIG_PRODUCER_BEEF_CATTLE_COL, producer.beef_cattle)
        values.put(CIG_PRODUCER_SHEEP_COL, producer.sheep)
        values.put(CIG_PRODUCER_POULTRY_COL, producer.poultry)
        values.put(CIG_PRODUCER_PIGS_COL, producer.pigs)
        values.put(CIG_PRODUCER_RABBITS_COL, producer.rabbits)
        values.put(CIG_PRODUCER_BEEHIVES_COL, producer.beehives)
        values.put(CIG_PRODUCER_DONKEYS_COL, producer.donkeys)
        values.put(CIG_PRODUCER_GOATS_COL, producer.goats)
        values.put(CIG_PRODUCER_CAMELS_COL, producer.camels)
        values.put(CIG_PRODUCER_AQUACULTURE_COL, producer.aquaculture)
        values.put(CIG_PRODUCER_HOUSING_TYPE_COL, producer.housing_type)
        values.put(CIG_PRODUCER_HOUSING_FLOOR_COL, producer.housing_floor)
        values.put(CIG_PRODUCER_HOUSING_ROOF_COL, producer.housing_roof)
        values.put(CIG_PRODUCER_LIGHTING_FUEL_COL, producer.lighting_fuel)
        values.put(CIG_PRODUCER_COOKING_FUEL_COL, producer.cooking_fuel)
        values.put(CIG_PRODUCER_WATER_FILTER_COL, producer.water_filter)
        values.put(CIG_PRODUCER_WATER_TANK_GREATER_THAN_5000LTS_COL, producer.water_tank_greater_than_5000lts)
        values.put(CIG_PRODUCER_HAND_WASHING_FACILITIES_COL, producer.hand_washing_facilities)
        values.put(CIG_PRODUCER_PPES_COL, producer.ppes)
        values.put(CIG_PRODUCER_WATER_WELL_OR_WEIR_COL, producer.water_well_or_weir)
        values.put(CIG_PRODUCER_IRRIGATION_PUMP_COL, producer.irrigation_pump)
        values.put(CIG_PRODUCER_HARVESTING_EQUIPMENT_COL, producer.harvesting_equipment)
        values.put(CIG_PRODUCER_TRANSPORTATION_TYPE_COL, producer.transportation_type)
        values.put(CIG_PRODUCER_TOILET_FLOOR_COL, producer.toilet_floor)
        values.put(CIG_PRODUCER_USER_APPROVED_COL, producer.user_approved)
        values.put(CIG_PRODUCER_TA_COL, producer.ta)
        values.put(CIG_PRODUCER_USER_ID_COL, producer.user_id)
        values.put(IS_OFFLINE_COL, 1)

        // Insert producer biodata
        val producerResult = db.insert(CIG_PRODUCER_BIODATA_TABLE_NAME, null, values)
        if (producerResult == -1L) {
            return false
        }

        // Insert commercial produce if available
        val commercialProduces: List<CommercialProduce>? = producer.commercialProduces
        if (commercialProduces != null) {
            for (produce in commercialProduces) {
                produce.cig_producer_biodata_id = producerResult.toInt()
                val produceInserted = insertCIGCommercialProduce(produce)
                if (!produceInserted) {
                    return false
                }
            }
        }

        // Insert domestic produce if available
        val domesticProduces: List<DomesticProduce>? = producer.domesticProduces
        if (domesticProduces != null) {
            for (produce in domesticProduces) {
                produce.cig_producer_biodata_id = producerResult.toInt()
                val produceInserted = insertCIGDomesticProduce(produce)
                if (!produceInserted) {
                    return false
                }
            }
        }
        return true
    }

    // Insert cig commercial produce
    fun insertCIGCommercialProduce(produce: CommercialProduce): Boolean {

        val db = this.writableDatabase
        val values = ContentValues()

        values.put(COMMERCIAL_PRODUCE_PRODUCT_COL, produce.product)
        values.put(COMMERCIAL_PRODUCE_PRODUCT_CATEGORY_COL, produce.product_category)
        values.put(COMMERCIAL_PRODUCE_ACERAGE_COL, produce.acerage)
        values.put(COMMERCIAL_PRODUCER_BIODATA_ID_FK_COL, produce.producer_biodata_id)
        values.put(COMMERCIAL_CIG_PRODUCER_BIODATA_ID_FK_COL, produce.cig_producer_biodata_id)
        values.put(IS_OFFLINE_COL, 1)

        val result = db.insert(COMMERCIAL_PRODUCE_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert cig domestic produce
    fun insertCIGDomesticProduce(produce: DomesticProduce): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(DOMESTIC_PRODUCE_PRODUCT_COL, produce.product)
        values.put(DOMESTIC_PRODUCE_PRODUCT_CATEGORY_COL, produce.product_category)
        values.put(DOMESTIC_PRODUCE_ACERAGE_COL, produce.acerage)
        values.put(DOMESTIC_PRODUCER_BIODATA_ID_FK_COL, produce.producer_biodata_id)
        values.put(DOMESTIC_CIG_PRODUCER_BIODATA_ID_FK_COL, produce.cig_producer_biodata_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(DOMESTIC_PRODUCE_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert Market Produce Data
    fun insertMarketProduce(marketProduce: MarketProduce): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(MARKET_PRODUCE_PRODUCT_COL, marketProduce.product)
        values.put(MARKET_PRODUCE_PRODUCT_CATEGORY_COL, marketProduce.product_category)
        values.put(MARKET_PRODUCE_ACERAGE_COL, marketProduce.acerage)
        values.put(MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL, marketProduce.season_planning_id)
        values.put(MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL, marketProduce.extension_service_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        Log.d("DatabaseInsertMarketProduce", "Inserting MarketProduce with season_planning_id: ${marketProduce.season_planning_id}")

        val result = db.insert(MARKET_PRODUCE_TABLE_NAME, null, values)
        return result != -1L
    }

    // Insert Plan Nutrition Data
    fun insertPlanNutrition(planNutrition: PlanNutrition): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(PLAN_NUTRITION_PRODUCT_COL, planNutrition.product)
        values.put(PLAN_NUTRITION_PRODUCT_NAME_COL, planNutrition.product_name)
        values.put(PLAN_NUTRITION_UNIT_COL, planNutrition.unit)
        values.put(PLAN_NUTRITION_COST_PER_UNIT_COL, planNutrition.cost_per_unit)
        values.put(PLAN_NUTRITION_APPLICATION_RATE_COL, planNutrition.application_rate)
        values.put(PLAN_NUTRITION_TIME_OF_APPLICATION_COL, planNutrition.time_of_application)
        values.put(PLAN_NUTRITION_METHOD_OF_APPLICATION_COL, planNutrition.method_of_application)
        values.put(PLAN_NUTRITION_PRODUCT_FORMULATION_COL, planNutrition.product_formulation)
        values.put(PLAN_NUTRITION_DATE_OF_APPLICATION_COL, planNutrition.date_of_application)
        values.put(PLAN_NUTRITION_TOTAL_MIXING_RATIO_COL, planNutrition.total_mixing_ratio)
        values.put(PLAN_NUTRITION_SEASON_PLANNING_ID_FK_COL, planNutrition.season_planning_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(PLAN_NUTRITION_TABLE_NAME, null, values)
        return result != -1L
    }

    // Insert Scouting Station Data
    fun insertScoutingStation(scoutingStation: ScoutingStation): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(SCOUTING_STATION_BAIT_STATION_COL, scoutingStation.bait_station)
        values.put(SCOUTING_STATION_TYPE_OF_BAIT_PROVIDED_COL, scoutingStation.type_of_bait_provided)
        values.put(SCOUTING_STATION_FREQUENCY_COL, scoutingStation.frequency)
        values.put(SCOUTING_STATION_SEASON_PLANNING_ID_FK_COL, scoutingStation.season_planning_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(SCOUTING_STATION_TABLE_NAME, null, values)
        return result != -1L
    }

    // Insert Preventative Disease Data
    fun insertPreventativeDisease(preventativeDisease: PreventativeDisease): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(PREVENTATIVE_DISEASE_DISEASE_COL, preventativeDisease.disease)
        values.put(PREVENTATIVE_DISEASE_PRODUCT_COL, preventativeDisease.product)
        values.put(PREVENTATIVE_DISEASE_CATEGORY_COL, preventativeDisease.category)
        values.put(PREVENTATIVE_DISEASE_FORMULATION_COL, preventativeDisease.formulation)
        values.put(PREVENTATIVE_DISEASE_DOSAGE_COL, preventativeDisease.dosage)
        values.put(PREVENTATIVE_DISEASE_UNIT_COL, preventativeDisease.unit)
        values.put(PREVENTATIVE_DISEASE_COST_PER_UNIT_COL, preventativeDisease.cost_per_unit)
        values.put(PREVENTATIVE_DISEASE_VOLUME_OF_WATER_COL, preventativeDisease.volume_of_water)
        values.put(PREVENTATIVE_DISEASE_FREQUENCY_OF_APPLICATION_COL, preventativeDisease.frequency_of_application)
        values.put(PREVENTATIVE_DISEASE_TOTAL_COST_COL, preventativeDisease.total_cost)
        values.put(PREVENTATIVE_DISEASE_SEASON_PLANNING_ID_FK_COL, preventativeDisease.season_planning_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(PREVENTATIVE_DISEASE_TABLE_NAME, null, values)
        return result != -1L
    }

    // Insert Preventative Pest Data
    fun insertPreventativePest(preventativePest: PreventativePest): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(PREVENTATIVE_PEST_PEST_COL, preventativePest.pest)
        values.put(PREVENTATIVE_PEST_PRODUCT_COL, preventativePest.product)
        values.put(PREVENTATIVE_PEST_CATEGORY_COL, preventativePest.category)
        values.put(PREVENTATIVE_PEST_FORMULATION_COL, preventativePest.formulation)
        values.put(PREVENTATIVE_PEST_DOSAGE_COL, preventativePest.dosage)
        values.put(PREVENTATIVE_PEST_UNIT_COL, preventativePest.unit)
        values.put(PREVENTATIVE_PEST_COST_PER_UNIT_COL, preventativePest.cost_per_unit)
        values.put(PREVENTATIVE_PEST_VOLUME_OF_WATER_COL, preventativePest.volume_of_water)
        values.put(PREVENTATIVE_PEST_FREQUENCY_OF_APPLICATION_COL, preventativePest.frequency_of_application)
        values.put(PREVENTATIVE_PEST_TOTAL_COST_COL, preventativePest.total_cost)
        values.put(PREVENTATIVE_PEST_SEASON_PLANNING_ID_FK_COL, preventativePest.season_planning_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(PREVENTATIVE_PEST_TABLE_NAME, null, values)
        return result != -1L
    }

    // Insert Plan Irrigation Data
    fun insertPlanIrrigation(planIrrigation: PlanIrrigation): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(PLAN_IRRIGATION_TYPE_OF_IRRIGATION_COL, planIrrigation.type_of_irrigation)
        values.put(PLAN_IRRIGATION_DISCHARGE_HOURS_COL, planIrrigation.discharge_hours)
        values.put(PLAN_IRRIGATION_FREQUENCY_COL, planIrrigation.frequency)
        values.put(PLAN_IRRIGATION_COST_OF_FUEL_COL, planIrrigation.cost_of_fuel)
        values.put(PLAN_IRRIGATION_UNIT_COST_COL, planIrrigation.unit_cost)
        values.put(PLAN_IRRIGATION_SEASON_PLANNING_ID_FK_COL, planIrrigation.season_planning_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(PLAN_IRRIGATION_TABLE_NAME, null, values)
        return result != -1L
    }

    // Insert Ext Service
    fun insertExtensionService(extensionService: ExtensionService): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        // Populate ContentValues for ExtensionService
        values.put(EXTENSION_SERVICE_PRODUCER_COL, extensionService.producer)
        values.put(EXTENSION_SERVICE_FIELD_COL, extensionService.fieldName)
        values.put(EXTENSION_SERVICE_PLANNED_DATE_OF_PLANTING_COL, extensionService.planned_date_of_planting)
        values.put(EXTENSION_SERVICE_WEEK_NUMBER_COL, extensionService.week_number)

        // JSON fields
        values.put(EXTENSION_SERVICE_NURSERY_COL, Gson().toJson(extensionService.nursery))
        values.put(EXTENSION_SERVICE_SOIL_ANALYSIS_COL, Gson().toJson(extensionService.soil_analysis))
        values.put(EXTENSION_SERVICE_LIMING_COL, Gson().toJson(extensionService.liming))
        values.put(EXTENSION_SERVICE_TRANSPLANTING_COL, Gson().toJson(extensionService.transplanting))
        values.put(EXTENSION_SERVICE_WEEDING_COL, Gson().toJson(extensionService.weeding))
        values.put(EXTENSION_SERVICE_PRUNNING_THINNING_DESUCKERING_COL, Gson().toJson(extensionService.prunning_thinning_desuckering))
        values.put(EXTENSION_SERVICE_MULCHING_COL, Gson().toJson(extensionService.mulching))
        values.put(EXTENSION_SERVICE_GAPPING_COL, Gson().toJson(extensionService.gapping))
        values.put(EXTENSION_SERVICE_HARVESTING_COL, Gson().toJson(extensionService.harvesting))
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        // Insert ExtensionService and get the inserted ID
        val extensionServiceId = db.insert(EXTENSION_SERVICE_TABLE_NAME, null, values)

        if (extensionServiceId == -1L) {
            // Insertion failed
            return false
        }

        // Set the generated ID to the extensionService object
        extensionService.id = extensionServiceId.toInt()

        // Log the ExtensionService data
        val gson = Gson()
        val extensionServiceJson = gson.toJson(extensionService)
        Log.d("ExtensionServiceFragment", "ExtensionService Data: $extensionServiceJson")

        // Insert related data using the proper extensionServiceId
        extensionService.marketProduces.forEach { produce ->
            produce.extension_service_id = extensionService.id
            if (!insertMarketProduce(produce)) return false
        }

        extensionService.forecast_yields.forEach { yield ->
            yield.extension_service_registration_id = extensionService.id
            if (!insertForecastYield(yield)) return false
        }

        extensionService.pesticides_used.forEach { pesticide ->
            pesticide.extension_service_registration_id = extensionService.id
            if (!insertPesticideUsed(pesticide)) return false
        }

        extensionService.fertlizers_used.forEach { fertilizer ->
            fertilizer.extension_service_registration_id = extensionService.id
            if (!insertFertilizerUsed(fertilizer)) return false
        }

        extensionService.ext_scouting_stations.forEach { station ->
            station.extension_service_registration_id = extensionService.id
            if (!insertExtScoutingStation(station)) return false
        }

        return true
    }

    fun insertExtScoutingStation(scoutingStation: ExtScoutingStation): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(EXT_SCOUTING_STATION_SCOUTING_METHOD_COL, scoutingStation.scouting_method)
        values.put(EXT_SCOUTING_STATION_BAIT_STATION_COL, scoutingStation.bait_station)
        values.put(EXT_SCOUTING_STATION_PEST_OR_DISEASE_COL, scoutingStation.pest_or_disease)
        values.put(EXT_SCOUTING_STATION_MANAGEMENT_COL, scoutingStation.management)
        values.put(EXT_SCOUTING_STATION_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL, scoutingStation.extension_service_registration_id)
        values.put(IS_OFFLINE_COL, 1)

        val result = db.insert(EXT_SCOUTING_STATION_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun insertPesticideUsed(pesticideUsed: PesticideUsed): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(PESTICIDE_USED_REGISTER_COL, pesticideUsed.register)
        values.put(PESTICIDE_USED_PRODUCT_COL, pesticideUsed.product)
        values.put(PESTICIDE_USED_CATEGORY_COL, pesticideUsed.category)
        values.put(PESTICIDE_USED_FORMULATION_COL, pesticideUsed.formulation)
        values.put(PESTICIDE_USED_DOSAGE_COL, pesticideUsed.dosage)
        values.put(PESTICIDE_USED_UNIT_COL, pesticideUsed.unit)
        values.put(PESTICIDE_USED_COST_PER_UNIT_COL, pesticideUsed.cost_per_unit)
        values.put(PESTICIDE_USED_VOLUME_OF_WATER_COL, pesticideUsed.volume_of_water)
        values.put(PESTICIDE_USED_FREQUENCY_OF_APPLICATION_COL, pesticideUsed.frequency_of_application)
        values.put(PESTICIDE_USED_TOTAL_COST_COL, pesticideUsed.total_cost)
        values.put(PESTICIDE_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL, pesticideUsed.extension_service_registration_id)
        values.put(IS_OFFLINE_COL, 1)

        val result = db.insert(PESTICIDE_USED_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun insertFertilizerUsed(fertilizerUsed: FertilizerUsed): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(FERTILIZER_USED_REGISTER_COL, fertilizerUsed.register)
        values.put(FERTILIZER_USED_PRODUCT_COL, fertilizerUsed.product)
        values.put(FERTILIZER_USED_CATEGORY_COL, fertilizerUsed.category)
        values.put(FERTILIZER_USED_FORMULATION_COL, fertilizerUsed.formulation)
        values.put(FERTILIZER_USED_DOSAGE_COL, fertilizerUsed.dosage)
        values.put(FERTILIZER_USED_UNIT_COL, fertilizerUsed.unit)
        values.put(FERTILIZER_USED_COST_PER_UNIT_COL, fertilizerUsed.cost_per_unit)
        values.put(FERTILIZER_USED_VOLUME_OF_WATER_COL, fertilizerUsed.volume_of_water)
        values.put(FERTILIZER_USED_FREQUENCY_OF_APPLICATION_COL, fertilizerUsed.frequency_of_application)
        values.put(FERTILIZER_USED_TOTAL_COST_COL, fertilizerUsed.total_cost)
        values.put(FERTILIZER_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL, fertilizerUsed.extension_service_registration_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(FERTILIZER_USED_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun insertForecastYield(forecastYield: ForecastYield): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(FORECAST_YIELD_CROP_POPULATION_PC_COL, forecastYield.crop_population_pc)
        values.put(FORECAST_YIELD_YIELD_FORECAST_PC_COL, forecastYield.yield_forecast_pc)
        values.put(FORECAST_YIELD_FORECAST_QUALITY_COL, forecastYield.forecast_quality)
        values.put(FORECAST_YIELD_TA_COMMENTS_COL, forecastYield.ta_comments)
        values.put(FORECAST_YIELD_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL, forecastYield.extension_service_registration_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(FORECAST_YIELD_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert training data
    fun insertTraining(training: Training): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(TRAINING_COURSE_NAME_COL, training.course_name)
        values.put(TRAINING_TRAINER_NAME_COL, training.trainer_name)
        values.put(TRAINING_BUYING_CENTER_COL, training.buying_center)
        values.put(TRAINING_COURSE_DESCRIPTION_COL, training.course_description)
        values.put(TRAINING_DATE_OF_TRAINING_COL, training.date_of_training)
        values.put(TRAINING_CONTENT_OF_TRAINING_COL, training.content_of_training)
        values.put(TRAINING_VENUE_COL, training.venue)

        val participantsJson = Gson().toJson(training.participants)
        values.put(TRAINING_PARTICIPANTS_COL, participantsJson)

        values.put(TRAINING_USER_ID_FK_COL, training.user_id)
        values.put(IS_OFFLINE_COL, 1)

        val result = db.insert(TRAINING_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun insertAttendance(attendance: Attendance): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(ATTENDANCE_ATTENDANCE_COL, attendance.attendance)
        values.put(ATTENDANCE_TRAINING_ID_COL, attendance.training_id)
        values.put(ATTENDANCE_USER_ID_FK_COL, attendance.user_id)
        values.put(IS_OFFLINE_COL, 1)

        val result = db.insert(ATTENDANCE_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert price distribution data
    fun insertFarmerPriceDistribution(farmerPriceDistribution: FarmerPriceDistribution): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(FARMER_PRICE_DISTRIBUTION_HUB_COL, farmerPriceDistribution.hub)
        values.put(FARMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL, farmerPriceDistribution.buying_center)
        values.put(FARMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL, farmerPriceDistribution.online_price)
        values.put(FARMER_PRICE_DISTRIBUTION_UNIT_COL, farmerPriceDistribution.unit)
        values.put(FARMER_PRICE_DISTRIBUTION_DATE_COL, farmerPriceDistribution.date)
        values.put(FARMER_PRICE_DISTRIBUTION_COMMENTS_COL, farmerPriceDistribution.comments)
        values.put(FARMER_PRICE_DISTRIBUTION_SOLD_COL, farmerPriceDistribution.sold)
        values.put(FARMER_PRICE_DISTRIBUTION_USER_ID_FK_COL, farmerPriceDistribution.user_id)
        values.put(FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL, farmerPriceDistribution.produce_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(FARMER_PRICE_DISTRIBUTION_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun insertCustomerPriceDistribution(customerPriceDistribution: CustomerPriceDistribution): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(CUSTOMER_PRICE_DISTRIBUTION_HUB_COL, customerPriceDistribution.hub)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL, customerPriceDistribution.buying_center)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL, customerPriceDistribution.online_price)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_UNIT_COL, customerPriceDistribution.unit)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_DATE_COL, customerPriceDistribution.date)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_COMMENTS_COL, customerPriceDistribution.comments)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_SOLD_COL, customerPriceDistribution.sold)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_USER_ID_FK_COL, customerPriceDistribution.user_id)
        values.put(CUSTOMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL, customerPriceDistribution.produce_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(CUSTOMER_PRICE_DISTRIBUTION_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    fun insertBuyingCustomer(buyingCustomer: BuyingCustomer): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(BUYING_CUSTOMER_PRODUCE_COL, buyingCustomer.produce)
        values.put(BUYING_CUSTOMER_CUSTOMER_COL, buyingCustomer.customer)
        values.put(BUYING_CUSTOMER_GRN_NUMBER_COL, buyingCustomer.grn_number)
        values.put(BUYING_CUSTOMER_UNIT_COL, buyingCustomer.unit)

        val qualityJson = Gson().toJson(buyingCustomer.quality)
        values.put(BUYING_FARMER_QUALITY_COL, qualityJson)

        values.put(BUYING_CUSTOMER_ACTION_COL, buyingCustomer.action)
        values.put(BUYING_CUSTOMER_WEIGHT_COL, buyingCustomer.weight)
        values.put(BUYING_CUSTOMER_ONLINE_PRICE_COL, buyingCustomer.online_price)
        values.put(BUYING_CUSTOMER_LOADED_COL, buyingCustomer.loaded)
        values.put(BUYING_CUSTOMER_USER_ID_FK_COL, buyingCustomer.user_id)
        values.put(IS_OFFLINE_COL, 1) // Mark as offline

        val result = db.insert(BUYING_CUSTOMER_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // Insert rural worker
    fun insertRuralWorker(ruralWorker: RuralWorker): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        values.put(RURAL_WORKER_OTHER_NAME_COL, ruralWorker.other_name)
        values.put(RURAL_WORKER_LAST_NAME_COL, ruralWorker.last_name)
        values.put(RURAL_WORKER_RURAL_WORKER_CODE_COL, ruralWorker.rural_worker_code)
        values.put(RURAL_WORKER_ID_NUMBER_COL, ruralWorker.id_number)
        values.put(RURAL_WORKER_GENDER_COL, ruralWorker.gender)
        values.put(RURAL_WORKER_DATE_OF_BIRTH_COL, ruralWorker.date_of_birth)
        values.put(RURAL_WORKER_EMAIL_COL, ruralWorker.email)
        values.put(RURAL_WORKER_PHONE_NUMBER_COL, ruralWorker.phone_number)
        values.put(RURAL_WORKER_EDUCATION_LEVEL_COL, ruralWorker.education_level)
        values.put(RURAL_WORKER_SERVICE_COL, ruralWorker.service)
        values.put(RURAL_WORKER_OTHER_COL, ruralWorker.other)
        values.put(RURAL_WORKER_COUNTY_COL, ruralWorker.county)
        values.put(RURAL_WORKER_SUB_COUNTY_COL, ruralWorker.sub_county)
        values.put(RURAL_WORKER_WARD_COL, ruralWorker.ward)
        values.put(RURAL_WORKER_VILLAGE_COL, ruralWorker.village)
        values.put(RURAL_WORKER_USER_ID_FK_COL, ruralWorker.user_id)
        values.put(IS_OFFLINE_COL, 1)

        val result = db.insert(RURAL_WORKER_TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    @get:SuppressLint("Range")
    val offlineHubs: List<Hub>
        get() {
            val hubs: MutableList<Hub> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                HUB_ID_COL,
                regionCol,
                hubNameCol,
                hubCodeCol,
                addressCol,
                yearEstablishedCol,
                ownershipCol,
                floorSizeCol,
                facilitiesCol,
                inputCenterCol,
                typeOfBuildingCol,
                longitudeCol,
                latitudeCol,
                hubUserIdCol
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(HUB_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val hub = Hub()
                    hub.id = cursor.getInt(cursor.getColumnIndex(HUB_ID_COL))
                    hub.region = cursor.getString(cursor.getColumnIndex(regionCol))
                    hub.hub_name = cursor.getString(cursor.getColumnIndex(hubNameCol))
                    hub.hub_code = cursor.getString(cursor.getColumnIndex(hubCodeCol))
                    hub.address = cursor.getString(cursor.getColumnIndex(addressCol))
                    hub.year_established = cursor.getString(cursor.getColumnIndex(yearEstablishedCol))
                    hub.ownership = cursor.getString(cursor.getColumnIndex(ownershipCol))
                    hub.floor_size = cursor.getString(cursor.getColumnIndex(floorSizeCol))
                    hub.facilities = cursor.getString(cursor.getColumnIndex(facilitiesCol))
                    hub.input_center = cursor.getString(cursor.getColumnIndex(inputCenterCol))
                    hub.type_of_building = cursor.getString(cursor.getColumnIndex(typeOfBuildingCol))
                    hub.longitude = cursor.getString(cursor.getColumnIndex(longitudeCol))
                    hub.latitude = cursor.getString(cursor.getColumnIndex(latitudeCol))
                    hub.user_id = cursor.getString(cursor.getColumnIndex(hubUserIdCol))

                    // Add hub to list
                    hubs.add(hub)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return hubs
        }

    @get:SuppressLint("Range")
    val allHubNames: List<String>
        get() {
            val hubsList: MutableList<String> = ArrayList()

            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT " + hubNameCol + " FROM " + HUB_TABLE_NAME, null)
            Log.d("Cursor", "Column count: " + cursor!!.columnCount)
            Log.d("Cursor", "Column names: " + cursor.columnNames.contentToString())

            if (cursor != null && cursor.moveToFirst()) {
                do  {
                    @SuppressLint("Range") val hubName = cursor.getString(cursor.getColumnIndex(
                        hubNameCol
                    ))

                    hubsList.add(hubName)
                } while (cursor.moveToNext())
            } else {
                Log.e("Cursor", "Cursor is null or empty")
            }

            cursor.close()
            return hubsList
        }

    @get:SuppressLint("Range")
    val allBuyingCenterNames: List<String>
        get() {
            val buyingCenterList: MutableList<String> = ArrayList()

            val db = this.readableDatabase
            val cursor = db.rawQuery("SELECT " + BUYING_CENTER_NAME_COL + " FROM " + BUYING_CENTER_TABLE_NAME, null)
            Log.d("Cursor", "Column count: " + cursor!!.columnCount)
            Log.d("Cursor", "Column names: " + cursor.columnNames.contentToString())

            if (cursor != null && cursor.moveToFirst()) {
                do  {
                    val buyingCenterName = cursor.getString(cursor.getColumnIndex(
                        BUYING_CENTER_NAME_COL
                    ))
                    buyingCenterList.add(buyingCenterName)
                } while (cursor.moveToNext())
            } else {
                Log.e("Cursor", "Cursor is null or empty")
            }

            cursor.close()
            return buyingCenterList
        }

    // Producer names
    @SuppressLint("Range")
    fun getProducerNames(): Map<Int, String> {
        val producerMap: MutableMap<Int, String> = HashMap()

        val db = this.readableDatabase
        val query = "SELECT ${PRODUCER_BIODATA_ID_COL}, ${PRODUCER_LAST_NAME_COL}, ${PRODUCER_OTHER_NAME_COL} FROM ${PRODUCER_BIODATA_TABLE_NAME}"
        val cursor = db.rawQuery(query, null)

        Log.d("ProducerData", "Executing query: $query")
        Log.d("ProducerData", "Column count: ${cursor.columnCount}")
        Log.d("ProducerData", "Column names: ${cursor.columnNames.joinToString()}")

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val idIndex = cursor.getColumnIndex(PRODUCER_BIODATA_ID_COL)
                val lastNameIndex = cursor.getColumnIndex(PRODUCER_LAST_NAME_COL)
                val otherNameIndex = cursor.getColumnIndex(PRODUCER_OTHER_NAME_COL)

                val id = cursor.getInt(idIndex)
                val lastName = cursor.getString(lastNameIndex)
                val otherName = cursor.getString(otherNameIndex)
                val fullName = "${lastName} ${otherName}".trim()

                Log.d("ProducerData", "ID: $id, FullName: $fullName")

                producerMap[id] = fullName
            } while (cursor.moveToNext())
        } else {
            Log.e("ProducerData", "Cursor is null or empty")
        }

        cursor.close()
        Log.d("ProducerData", "Producer Names Map: $producerMap")
        return producerMap
    }



    // Get training data
    fun getAllTrainings(): List<Training> {
        val trainings = mutableListOf<Training>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TRAINING_TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val training = Training().apply {
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(TRAINING_ID_COL))
                    course_name = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_COURSE_NAME_COL))
                    trainer_name = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_TRAINER_NAME_COL))
                    buying_center = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_BUYING_CENTER_COL))
                    course_description = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_COURSE_DESCRIPTION_COL))
                    date_of_training = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_DATE_OF_TRAINING_COL))
                    content_of_training = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_CONTENT_OF_TRAINING_COL))
                    venue = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_VENUE_COL))

                    // Fetch participants as JSON string and parse into Map
                    val participantsJson = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_PARTICIPANTS_COL))
                    setParticipantsFromJson(participantsJson)

                    // Handle user_id as String to match the model class
                    user_id = cursor.getString(cursor.getColumnIndexOrThrow(TRAINING_USER_ID_FK_COL))
                }
                trainings.add(training)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return trainings
    }

    // Getting produces for farmer price distributions
    fun getMarketProducesForBuyingCenter(buyingCenter: String): List<MarketProduce> {
        val marketProduces = mutableListOf<MarketProduce>()

        // Step 1: Get Producer IDs for the specified Buying Center
        val producerIds = getProducerIdsByBuyingCenter(buyingCenter)
        Log.d("DatabaseDebug", "Producer IDs for buying center '$buyingCenter': $producerIds")

        if (producerIds.isNotEmpty()) {
            // Step 2: Get Extension Services for the filtered Producers
            val extensionServiceIds = getExtensionServiceIdsByProducerIds(producerIds)
            Log.d("DatabaseDebug", "Extension Service IDs for producers $producerIds: $extensionServiceIds")

            if (extensionServiceIds.isNotEmpty()) {
                // Step 3: Get Market Produces for the filtered Extension Services
                val marketProduceMaps = getMarketProducesByExtensionServiceIds(extensionServiceIds)
                Log.d("DatabaseDebug", "Market produces for extension services $extensionServiceIds: $marketProduceMaps")

                // Convert each map to a MarketProduce object
                marketProduceMaps.forEach { map ->
                    val marketProduce = MarketProduce().apply {
                        id = map[MARKET_PRODUCE_ID_COL] as? Int ?: 0
                        product = map[MARKET_PRODUCE_PRODUCT_COL] as? String
                        product_category = map[MARKET_PRODUCE_PRODUCT_CATEGORY_COL] as? String
                        acerage = map[MARKET_PRODUCE_ACERAGE_COL] as? String
                        season_planning_id = (map[MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL] as? Int)?.takeIf { it != 0 }
                        extension_service_id = (map[MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL] as? Int)?.takeIf { it != 0 }
                    }
                    marketProduces.add(marketProduce)
                }
            }
        }

        return marketProduces
    }

    private fun getProducerIdsByBuyingCenter(buyingCenter: String): List<String> {
        val producerIds = mutableListOf<String>()
        val db = this.readableDatabase
        // Ensure to fetch the correct column (ID) that matches the producer field in extensionServices
        val query = "SELECT $PRODUCER_BIODATA_ID_COL FROM $PRODUCER_BIODATA_TABLE_NAME WHERE $PRODUCER_BUYING_CENTER_COL = ?"
        val cursor = db.rawQuery(query, arrayOf(buyingCenter))

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(PRODUCER_BIODATA_ID_COL)
            while (!cursor.isAfterLast) {
                if (columnIndex >= 0) {
                    val producerId = cursor.getInt(columnIndex).toString() // Convert to string
                    producerIds.add(producerId)

                    // Log each producer ID and its data type
                    Log.d("DatabaseDebug", "Producer ID: $producerId, DataType: ${producerId::class.simpleName}")
                }
                cursor.moveToNext()
            }
        }

        cursor.close()

        // Log all the filtered producer IDs
        Log.d("DatabaseDebug", "Fetched Producer IDs for buying center '$buyingCenter': $producerIds")

        return producerIds
    }

    private fun getExtensionServiceIdsByProducerIds(producerIds: List<String>): List<Int> {
        val extensionServiceIds = mutableListOf<Int>()
        val db = this.readableDatabase

        if (producerIds.isEmpty()) {
            Log.d("DatabaseDebug", "No producer IDs provided. Returning empty list.")
            return extensionServiceIds
        }

        // Prepare placeholders for the IN clause
        val placeholders = producerIds.joinToString(",") { "?" }
        val query = "SELECT $EXTENSION_SERVICE_ID_COL FROM $EXTENSION_SERVICE_TABLE_NAME WHERE $EXTENSION_SERVICE_PRODUCER_COL IN ($placeholders)"

        // Execute the query with producer IDs as strings
        val cursor = db.rawQuery(query, producerIds.map { it.trim() }.toTypedArray())

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(EXTENSION_SERVICE_ID_COL)
            while (!cursor.isAfterLast) {
                if (columnIndex >= 0) {
                    // Fetch the ID and log it
                    val extensionServiceId = cursor.getInt(columnIndex)
                    Log.d("DatabaseDebug", "Fetched Extension Service ID: $extensionServiceId")
                    extensionServiceIds.add(extensionServiceId)
                }
                cursor.moveToNext()
            }
        } else {
            Log.d("DatabaseDebug", "No extension services found for the given producer IDs.")
        }

        cursor.close()
        Log.d("DatabaseDebug", "Fetched Extension Service IDs: $extensionServiceIds")
        return extensionServiceIds
    }

    private fun getMarketProducesByExtensionServiceIds(extensionServiceIds: List<Int>): List<Map<String, Any?>> {
        val marketProduces = mutableListOf<Map<String, Any?>>()
        val db = this.readableDatabase

        if (extensionServiceIds.isEmpty()) return marketProduces

        val placeholders = extensionServiceIds.joinToString(",") { "?" }
        val query = """
        SELECT * FROM $MARKET_PRODUCE_TABLE_NAME mp
        WHERE mp.$MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL IN ($placeholders)
        AND NOT EXISTS (
            SELECT 1 FROM $FARMER_PRICE_DISTRIBUTION_TABLE_NAME fpd
            WHERE fpd.$FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL = mp.$MARKET_PRODUCE_ID_COL
        )
    """.trimIndent()

        Log.d("DatabaseDebug", "Executing query: $query with parameters: ${extensionServiceIds.map { it.toString() }}")

        val cursor = db.rawQuery(query, extensionServiceIds.map { it.toString() }.toTypedArray())

        if (cursor.moveToFirst()) {
            do {
                val marketProduce = mapOf(
                    MARKET_PRODUCE_ID_COL to cursor.getInt(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_ID_COL)),
                    MARKET_PRODUCE_PRODUCT_COL to cursor.getString(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_PRODUCT_COL)),
                    MARKET_PRODUCE_PRODUCT_CATEGORY_COL to cursor.getString(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_PRODUCT_CATEGORY_COL)),
                    MARKET_PRODUCE_ACERAGE_COL to cursor.getString(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_ACERAGE_COL)),
                    MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL to cursor.getInt(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL)).takeIf { it != 0 },
                    MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL to cursor.getInt(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL)).takeIf { it != 0 }
                )
                marketProduces.add(marketProduce)
            } while (cursor.moveToNext())
        }

        cursor.close()
        Log.d("DatabaseDebug", "Fetched Market Produces: $marketProduces")
        return marketProduces
    }


    // Offline methods to get offline data
    @get:SuppressLint("Range")
    val offlineKeyContacts: List<KeyContact>
        get() {
            val keyContacts: MutableList<KeyContact> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                KEY_CONTACT_ID_COL,
                KEY_CONTACT_OTHER_NAME_COL,
                KEY_CONTACT_LAST_NAME_COL,
                ID_NUMBER_COL,
                GENDER_COL,
                KEY_CONTACT_ROLE_COL,
                DATE_OF_BIRTH_COL,
                KEY_CONTACT_EMAIL_COL,
                PHONE_NUMBER_COL,
                HUB_ID_FK_COL,
                BUYING_CENTER_ID_FK_COL
            )
            val selection = IS_OFFLINE_COL + "=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(KEY_CONTACTS_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val keyContact = KeyContact().apply {
                        id = cursor.getInt(cursor.getColumnIndex(KEY_CONTACT_ID_COL))
                        other_name = cursor.getString(cursor.getColumnIndex(KEY_CONTACT_OTHER_NAME_COL))
                        last_name = cursor.getString(cursor.getColumnIndex(KEY_CONTACT_LAST_NAME_COL))
                        id_number = cursor.getInt(cursor.getColumnIndex(ID_NUMBER_COL))
                        gender = cursor.getString(cursor.getColumnIndex(GENDER_COL))
                        role = cursor.getString(cursor.getColumnIndex(KEY_CONTACT_ROLE_COL))
                        date_of_birth = cursor.getString(cursor.getColumnIndex(DATE_OF_BIRTH_COL))
                        email = cursor.getString(cursor.getColumnIndex(KEY_CONTACT_EMAIL_COL))
                        phone_number = cursor.getString(cursor.getColumnIndex(PHONE_NUMBER_COL))
                        hub_id= cursor.getInt(cursor.getColumnIndex(HUB_ID_FK_COL))
                        buying_center_id = cursor.getInt(cursor.getColumnIndex(BUYING_CENTER_ID_FK_COL))
                    }

                    // Add key contact to list
                    keyContacts.add(keyContact)
                } while (cursor.moveToNext())

                cursor.close()
            }

            // db.close();
            return keyContacts
        }

    @get:SuppressLint("Range")
    val offlineBuyingCenters: List<BuyingCenter>
        get() {
            val buyingCenters: MutableList<BuyingCenter> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                BUYING_CENTER_ID_COL,
                HUB_COL,
                COUNTY_COL,
                SUB_COUNTY_COL,
                WARD_COL,
                VILLAGE_COL,
                BUYING_CENTER_NAME_COL,
                BUYING_CENTER_CODE_COL,
                BUYING_CENTER_ADDRESS_COL,
                BUYING_CENTER_YEAR_ESTABLISHED_COL,
                BUYING_CENTER_OWNERSHIP_COL,
                BUYING_CENTER_FLOOR_SIZE_COL,
                BUYING_CENTER_FACILITIES_COL,
                BUYING_CENTER_INPUT_CENTER_COL,
                BUYING_CENTER_TYPE_OF_BUILDING_COL,
                BUYING_CENTER_LOCATION_COL,
                BUYING_CENTER_USER_ID_COL
            )
            val selection = IS_OFFLINE_COL + "=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(BUYING_CENTER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val buyingCenter = BuyingCenter().apply {
                        id = cursor.getInt(cursor.getColumnIndex(BUYING_CENTER_ID_COL))
                        hub = cursor.getString(cursor.getColumnIndex(HUB_COL))
                        county = cursor.getString(cursor.getColumnIndex(COUNTY_COL))
                        sub_county = cursor.getString(cursor.getColumnIndex(SUB_COUNTY_COL))
                        ward = cursor.getString(cursor.getColumnIndex(WARD_COL))
                        village = cursor.getString(cursor.getColumnIndex(VILLAGE_COL))
                        buying_center_name = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_NAME_COL))
                        buying_center_code = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_CODE_COL))
                        address = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_ADDRESS_COL))
                        year_established = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_YEAR_ESTABLISHED_COL))
                        ownership = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_OWNERSHIP_COL))
                        floor_size = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_FLOOR_SIZE_COL))
                        facilities = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_FACILITIES_COL))
                        input_center = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_INPUT_CENTER_COL))
                        type_of_building = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_TYPE_OF_BUILDING_COL))

                        // Extract and set location (longitude and latitude)
                        var location = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_LOCATION_COL))
                        if (!location.isNullOrEmpty()) {
                            val coordinates = location.split(",").map { it.trim() }
                            if (coordinates.size == 2) {
                                location = coordinates.joinToString(", ")
                            }
                        }

                        user_id = cursor.getString(cursor.getColumnIndex(BUYING_CENTER_USER_ID_COL))
                    }

                    // Add buying center to list
                    buyingCenters.add(buyingCenter)
                } while (cursor.moveToNext())

                cursor.close()
            }

            // db.close();
            return buyingCenters
        }

    @get:SuppressLint("Range")
    val offlineCIGs: List<CIG>
        get() {
            val cigs: MutableList<CIG> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                CIG_ID_COL,
                CIG_NAME_COL,
                CIG_HUB_COL,
                NO_OF_MEMBERS_COL,
                DATE_ESTABLISHED_COL,
                CONSTITUTION_COL,
                REGISTRATION_COL,
                ELECTIONS_HELD_COL,
                DATE_OF_LAST_ELECTIONS_COL,
                MEETING_VENUE_COL,
                FREQUENCY_COL,
                SCHEDULED_MEETING_DAY_COL,
                SCHEDULED_MEETING_TIME_COL,
                hubUserIdCol
            )
            val selection = IS_OFFLINE_COL + "=?"
            val selectionArgs = arrayOf("1") // 1 indicates true for is_offline

            val cursor = db.query(CIG_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val cig = CIG().apply {
                        id = cursor.getInt(cursor.getColumnIndex(CIG_ID_COL))
                        cig_name = cursor.getString(cursor.getColumnIndex(CIG_NAME_COL))
                        hub = cursor.getString(cursor.getColumnIndex(CIG_HUB_COL))
                        no_of_members = cursor.getInt(cursor.getColumnIndex(NO_OF_MEMBERS_COL))
                        date_established = cursor.getString(cursor.getColumnIndex(DATE_ESTABLISHED_COL))
                        constitution = cursor.getString(cursor.getColumnIndex(CONSTITUTION_COL))
                        registration = cursor.getString(cursor.getColumnIndex(REGISTRATION_COL))
                        elections_held = cursor.getString(cursor.getColumnIndex(ELECTIONS_HELD_COL))
                        date_of_last_elections = cursor.getString(cursor.getColumnIndex(DATE_OF_LAST_ELECTIONS_COL))
                        meeting_venue = cursor.getString(cursor.getColumnIndex(MEETING_VENUE_COL))
                        frequency = cursor.getString(cursor.getColumnIndex(FREQUENCY_COL))
                        scheduled_meeting_day = cursor.getString(cursor.getColumnIndex(SCHEDULED_MEETING_DAY_COL))
                        scheduled_meeting_time = cursor.getString(cursor.getColumnIndex(SCHEDULED_MEETING_TIME_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(hubUserIdCol))
                    }

                    // Add CIG to list
                    cigs.add(cig)
                } while (cursor.moveToNext())

                cursor.close()
            }

            // db.close();
            return cigs
        }

    @get:SuppressLint("Range")
    val offlineMembers: List<Member>
        get() {
            val members: MutableList<Member> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                MEMBER_ID_COL,
                MEMBER_OTHER_NAME_COL,
                MEMBER_LAST_NAME_COL,
                MEMBER_EMAIL_COL,
                MEMBER_DATE_OF_BIRTH_COL,
                MEMBER_ID_NUMBER_COL,
                MEMBER_PHONE_NUMBER_COL,
                MEMBER_GENDER_COL,
                PRODUCT_INVOLVED_COL,
                HECTORAGE_REGISTERED_UNDER_CIG_COL,
                MEMBER_CIG_ID_COL
            )
            val selection = IS_OFFLINE_COL + "=?"
            val selectionArgs = arrayOf("1") // 1 indicates true for is_offline

            val cursor = db.query(MEMBERS_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val member = Member().apply {
                        id = cursor.getInt(cursor.getColumnIndex(MEMBER_ID_COL))
                        other_name = cursor.getString(cursor.getColumnIndex(MEMBER_OTHER_NAME_COL))
                        last_name = cursor.getString(cursor.getColumnIndex(MEMBER_LAST_NAME_COL))
                        email = cursor.getString(cursor.getColumnIndex(MEMBER_EMAIL_COL))
                        date_of_birth = cursor.getString(cursor.getColumnIndex(MEMBER_DATE_OF_BIRTH_COL))
                        id_number = cursor.getInt(cursor.getColumnIndex(MEMBER_ID_NUMBER_COL))
                        phone_number = cursor.getLong(cursor.getColumnIndex(MEMBER_PHONE_NUMBER_COL))
                        gender = cursor.getString(cursor.getColumnIndex(MEMBER_GENDER_COL))
                        product_involved = cursor.getString(cursor.getColumnIndex(PRODUCT_INVOLVED_COL))
                        hectorage_registered_under_cig = cursor.getString(cursor.getColumnIndex(HECTORAGE_REGISTERED_UNDER_CIG_COL))
                        cig_id = cursor.getInt(cursor.getColumnIndex(MEMBER_CIG_ID_COL))
                    }

                    // Add member to list
                    members.add(member)
                } while (cursor.moveToNext())

                cursor.close()
            }

            // db.close();
            return members
        }

    @get:SuppressLint("Range")
    val offlineHQUsers: List<HQUser>
        get() {
            val hqUsers: MutableList<HQUser> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                HQ_USER_ID_COL,
                HQ_USER_OTHER_NAME_COL,
                HQ_USER_LAST_NAME_COL,
                HQ_USER_STAFF_CODE_COL,
                HQ_USER_DEPARTMENT_COL,
                HQ_USER_ID_NUMBER_COL,
                HQ_USER_GENDER_COL,
                HQ_USER_DATE_OF_BIRTH_COL,
                HQ_USER_EMAIL_COL,
                HQ_USER_PHONE_NUMBER_COL,
                HQ_USER_EDUCATION_LEVEL_COL,
                HQ_USER_ROLE_COL,
                HQ_USER_REPORTING_TO_COL,
                HQ_USER_RELATED_ROLES_COL,
                HQ_USER_USER_ID_COL
            )
            val selection = IS_OFFLINE_COL + "=?"
            val selectionArgs = arrayOf("1") // 1 indicates true for is_offline

            val cursor = db.query(HQ_USER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val hqUser = HQUser().apply {
                        id = cursor.getInt(cursor.getColumnIndex(HQ_USER_ID_COL))
                        other_name = cursor.getString(cursor.getColumnIndex(HQ_USER_OTHER_NAME_COL))
                        last_name = cursor.getString(cursor.getColumnIndex(HQ_USER_LAST_NAME_COL))
                        staff_code = cursor.getString(cursor.getColumnIndex(HQ_USER_STAFF_CODE_COL))
                        department = cursor.getString(cursor.getColumnIndex(HQ_USER_DEPARTMENT_COL))
                        id_number = cursor.getString(cursor.getColumnIndex(HQ_USER_ID_NUMBER_COL))
                        gender = cursor.getString(cursor.getColumnIndex(HQ_USER_GENDER_COL))
                        date_of_birth = cursor.getString(cursor.getColumnIndex(HQ_USER_DATE_OF_BIRTH_COL))
                        email = cursor.getString(cursor.getColumnIndex(HQ_USER_EMAIL_COL))
                        phone_number = cursor.getString(cursor.getColumnIndex(HQ_USER_PHONE_NUMBER_COL))
                        education_level = cursor.getString(cursor.getColumnIndex(HQ_USER_EDUCATION_LEVEL_COL))
                        role = cursor.getString(cursor.getColumnIndex(HQ_USER_ROLE_COL))
                        reporting_to = cursor.getString(cursor.getColumnIndex(HQ_USER_REPORTING_TO_COL))
                        related_roles = cursor.getString(cursor.getColumnIndex(HQ_USER_RELATED_ROLES_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(HQ_USER_USER_ID_COL))
                    }

                    // Add HQ user to list
                    hqUsers.add(hqUser)
                } while (cursor.moveToNext())

                cursor.close()
            }

            // db.close(); (Commented out since db should not be closed here to avoid CursorWindowClosedException)
            return hqUsers
        }


    @get:SuppressLint("Range")
    val offlineHubUsers: List<HubUser>
        get() {
            val hubUsers: MutableList<HubUser> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                HUB_USER_ID_COL,
                HUB_USER_OTHER_NAME_COL,
                HUB_USER_LAST_NAME_COL,
                HUB_USER_CODE_COL,
                HUB_USER_ROLE_COL,
                HUB_USER_ID_NUMBER_COL,
                HUB_USER_GENDER_COL,
                HUB_USER_DATE_OF_BIRTH_COL,
                HUB_USER_EMAIL_COL,
                HUB_USER_PHONE_NUMBER_COL,
                HUB_USER_EDUCATION_LEVEL_COL,
                HUB_USER_HUB_COL,
                HUB_USER_BUYING_CENTER_COL,
                HUB_USER_COUNTY_COL,
                HUB_USER_SUB_COUNTY_COL,
                HUB_USER_WARD_COL,
                HUB_USER_VILLAGE_COL,
                HUB_USER_USER_ID_COL
            )
            val selection = IS_OFFLINE_COL + "=?"
            val selectionArgs = arrayOf("1") // 1 indicates true for is_offline

            val cursor = db.query(HUB_USER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val hubUser = HubUser().apply {
                        id = cursor.getInt(cursor.getColumnIndex(HUB_USER_ID_COL))
                        other_name = cursor.getString(cursor.getColumnIndex(HUB_USER_OTHER_NAME_COL))
                        last_name = cursor.getString(cursor.getColumnIndex(HUB_USER_LAST_NAME_COL))
                        code = cursor.getString(cursor.getColumnIndex(HUB_USER_CODE_COL))
                        role = cursor.getString(cursor.getColumnIndex(HUB_USER_ROLE_COL))
                        id_number = cursor.getString(cursor.getColumnIndex(HUB_USER_ID_NUMBER_COL))
                        gender = cursor.getString(cursor.getColumnIndex(HUB_USER_GENDER_COL))
                        date_of_birth = cursor.getString(cursor.getColumnIndex(HUB_USER_DATE_OF_BIRTH_COL))
                        email = cursor.getString(cursor.getColumnIndex(HUB_USER_EMAIL_COL))
                        phone_number = cursor.getString(cursor.getColumnIndex(HUB_USER_PHONE_NUMBER_COL))
                        education_level = cursor.getString(cursor.getColumnIndex(HUB_USER_EDUCATION_LEVEL_COL))
                        hub = cursor.getString(cursor.getColumnIndex(HUB_USER_HUB_COL))
                        buying_center = cursor.getString(cursor.getColumnIndex(HUB_USER_BUYING_CENTER_COL))
                        county = cursor.getString(cursor.getColumnIndex(HUB_USER_COUNTY_COL))
                        sub_county = cursor.getString(cursor.getColumnIndex(HUB_USER_SUB_COUNTY_COL))
                        ward = cursor.getString(cursor.getColumnIndex(HUB_USER_WARD_COL))
                        village = cursor.getString(cursor.getColumnIndex(HUB_USER_VILLAGE_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(HUB_USER_USER_ID_COL))
                    }

                    // Add Hub user to list
                    hubUsers.add(hubUser)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return hubUsers
        }


    @get:SuppressLint("Range")
    val offlineIndividualLogistician: List<IndividualLogistician>
        get() {
            val logisticianList: MutableList<IndividualLogistician> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                INDIVIDUAL_LOGISTICIAN_USER_ID_COL,
                INDIVIDUAL_LOGISTICIAN_USER_OTHER_NAME_COL,
                INDIVIDUAL_LOGISTICIAN_USER_LAST_NAME_COL,
                LOGISTICIAN_CODE_COL,
                INDIVIDUAL_LOGISTICIAN_USER_ID_NUMBER_COL,
                INDIVIDUAL_LOGISTICIAN_USER_DATE_OF_BIRTH_COL,
                INDIVIDUAL_LOGISTICIAN_USER_EMAIL_COL,
                INDIVIDUAL_LOGISTICIAN_USER_PHONE_NUMBER_COL,
                INDIVIDUAL_LOGISTICIAN_USER_ADDRESS_COL,
                INDIVIDUAL_LOGISTICIAN_USER_HUB_COL,
                INDIVIDUAL_LOGISTICIAN_USER_REGION_COL,
                IS_OFFLINE_COL,
                INDIVIDUAL_LOGISTICIAN_USER_USER_ID_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(INDIVIDUAL_LOGISTICIAN_USER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val logistician = IndividualLogistician().apply {
                        id = cursor.getInt(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_ID_COL))
                        other_name = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_OTHER_NAME_COL))
                        last_name = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_LAST_NAME_COL))
                        logistician_code = cursor.getString(cursor.getColumnIndex(LOGISTICIAN_CODE_COL))
                        id_number = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_ID_NUMBER_COL))
                        date_of_birth = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_DATE_OF_BIRTH_COL))
                        email = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_EMAIL_COL))
                        phone_number = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_PHONE_NUMBER_COL))
                        address = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_ADDRESS_COL))
                        hub = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_HUB_COL))
                        region = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_REGION_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_USER_USER_ID_COL))
                    }

                    // Log individual logistician data
                    Log.d("LogisticianData", "ID: ${logistician.id}, Name: ${logistician.other_name} ${logistician.last_name}, Code: ${logistician.logistician_code}")

                    // Get associated cars
                    val cars: List<Car> = getCarsForLogistician(logistician.id)
                    logistician.cars = cars

                    // Log cars associated with logistician
                    cars.forEach { car ->
                        Log.d("CarData", "Car ID: ${car.id}, Model: ${car.car_model}, License Plate: ${car.number_plate}")
                    }

                    // Add logistician to list
                    logisticianList.add(logistician)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return logisticianList
        }

    @SuppressLint("Range")
    fun getCarsForLogistician(logisticianId: Int): List<Car> {
        val carList: MutableList<Car> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            CAR_ID_COL,
            CAR_BODY_TYPE_COL,
            CAR_MODEL_COL,
            CAR_DRIVER1_NAME_COL,
            CAR_DRIVER2_NAME_COL,
            CAR_NUMBER_PLATE_COL,
            INDIVIDUAL_LOGISTICIAN_ID_FK_COL
        )
        val selection = "$INDIVIDUAL_LOGISTICIAN_ID_FK_COL=?"
        val selectionArgs = arrayOf(logisticianId.toString())

        val cursor = db.query(CAR_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val car = Car().apply {
                    id = cursor.getInt(cursor.getColumnIndex(CAR_ID_COL))
                    car_body_type = cursor.getString(cursor.getColumnIndex(CAR_BODY_TYPE_COL))
                    car_model = cursor.getString(cursor.getColumnIndex(CAR_MODEL_COL))
                    driver1_name = cursor.getString(cursor.getColumnIndex(CAR_DRIVER1_NAME_COL))
                    driver2_name = cursor.getString(cursor.getColumnIndex(CAR_DRIVER2_NAME_COL))
                    number_plate = cursor.getString(cursor.getColumnIndex(CAR_NUMBER_PLATE_COL))
                    individual_logistician_id = cursor.getInt(cursor.getColumnIndex(INDIVIDUAL_LOGISTICIAN_ID_FK_COL))
                }

                // Add car to list
                carList.add(car)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return carList
    }

    @get:SuppressLint("Range")
    val offlineOrganisationalLogistician: List<OrganisationalLogistician>
        get() {
            val logisticianList: MutableList<OrganisationalLogistician> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                ORGANISATION_LOGISTICIAN_USER_ID_COL,
                ORGANISATION_LOGISTICIAN_USER_NAME_COL,
                ORGANISATION_LOGISTICIAN_USER_LOGISTICIAN_CODE_COL,
                ORGANISATION_LOGISTICIAN_USER_REGISTRATION_NUMBER_COL,
                ORGANISATION_LOGISTICIAN_USER_DATE_OF_REGISTRATION_COL,
                ORGANISATION_LOGISTICIAN_USER_EMAIL_COL,
                ORGANISATION_LOGISTICIAN_USER_PHONE_NUMBER_COL,
                ORGANISATION_LOGISTICIAN_USER_ADDRESS_COL,
                ORGANISATION_LOGISTICIAN_USER_HUB_COL,
                ORGANISATION_LOGISTICIAN_USER_REGION_COL,
                IS_OFFLINE_COL,
                ORGANISATION_LOGISTICIAN_USER_USER_ID_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(ORGANISATION_LOGISTICIAN_USER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val logistician = OrganisationalLogistician().apply {
                        id = cursor.getInt(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_ID_COL))
                        name = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_NAME_COL))
                        logistician_code = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_LOGISTICIAN_CODE_COL))
                        registration_number = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_REGISTRATION_NUMBER_COL))
                        date_of_registration = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_DATE_OF_REGISTRATION_COL))
                        email = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_EMAIL_COL))
                        phone_number = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_PHONE_NUMBER_COL))
                        address = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_ADDRESS_COL))
                        hub = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_HUB_COL))
                        region = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_REGION_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_USER_USER_ID_COL))
                    }

                    // Get associated cars
                    val cars: List<Car> = getCarsForOrganisationalLogistician(logistician.id)
                    logistician.cars = cars

                    // Add logistician to list
                    logisticianList.add(logistician)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return logisticianList
        }

    @SuppressLint("Range")
    fun getCarsForOrganisationalLogistician(logisticianId: Int): List<Car> {
        val carList: MutableList<Car> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            CAR_ID_COL,
            CAR_BODY_TYPE_COL,
            CAR_MODEL_COL,
            CAR_NUMBER_PLATE_COL,
            CAR_DRIVER1_NAME_COL,
            CAR_DRIVER2_NAME_COL,
            IS_OFFLINE_COL,
            ORGANISATION_LOGISTICIAN_ID_FK_COL
        )
        val selection = "$ORGANISATION_LOGISTICIAN_ID_FK_COL=?"
        val selectionArgs = arrayOf(logisticianId.toString())

        val cursor = db.query(CAR_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val car = Car().apply {
                    id = cursor.getInt(cursor.getColumnIndex(CAR_ID_COL))
                    car_body_type = cursor.getString(cursor.getColumnIndex(CAR_BODY_TYPE_COL))
                    car_model = cursor.getString(cursor.getColumnIndex(CAR_MODEL_COL))
                    number_plate = cursor.getString(cursor.getColumnIndex(CAR_NUMBER_PLATE_COL))
                    driver1_name = cursor.getString(cursor.getColumnIndex(CAR_DRIVER1_NAME_COL))
                    driver2_name = cursor.getString(cursor.getColumnIndex(CAR_DRIVER2_NAME_COL))
                    organisation_logistician_id = cursor.getInt(cursor.getColumnIndex(ORGANISATION_LOGISTICIAN_ID_FK_COL))
                }

                // Add car to list
                carList.add(car)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return carList
    }


    @SuppressLint("Range")
    fun getMarketProducesForSeasonPlanning(seasonPlanningId: Int): List<MarketProduce> {
        val marketProduceList: MutableList<MarketProduce> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            MARKET_PRODUCE_ID_COL,
            MARKET_PRODUCE_PRODUCT_COL,
            MARKET_PRODUCE_PRODUCT_CATEGORY_COL,
            MARKET_PRODUCE_ACERAGE_COL,
            MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL,
            MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL
        )
        val selection = "$MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL=?"
        val selectionArgs = arrayOf(seasonPlanningId.toString())

        Log.d("MarketProduceSync", "Querying database for seasonPlanningId: $seasonPlanningId")

        val cursor = db.query(MARKET_PRODUCE_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val marketProduce = MarketProduce().apply {
                    id = cursor.getInt(cursor.getColumnIndex(MARKET_PRODUCE_ID_COL))
                    product = cursor.getString(cursor.getColumnIndex(MARKET_PRODUCE_PRODUCT_COL))
                    product_category = cursor.getString(cursor.getColumnIndex(MARKET_PRODUCE_PRODUCT_CATEGORY_COL))
                    acerage = cursor.getString(cursor.getColumnIndex(MARKET_PRODUCE_ACERAGE_COL))
                }

                // Add market produce to list
                marketProduceList.add(marketProduce)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return marketProduceList
    }

    @SuppressLint("Range")
    fun getPlanNutritionsForSeasonPlanning(seasonPlanningId: Int): List<PlanNutrition> {
        val planNutritionList: MutableList<PlanNutrition> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            PLAN_NUTRITION_ID_COL,
            PLAN_NUTRITION_PRODUCT_COL,
            PLAN_NUTRITION_PRODUCT_NAME_COL,
            PLAN_NUTRITION_UNIT_COL,
            PLAN_NUTRITION_COST_PER_UNIT_COL,
            PLAN_NUTRITION_APPLICATION_RATE_COL,
            PLAN_NUTRITION_TIME_OF_APPLICATION_COL,
            PLAN_NUTRITION_METHOD_OF_APPLICATION_COL,
            PLAN_NUTRITION_PRODUCT_FORMULATION_COL,
            PLAN_NUTRITION_DATE_OF_APPLICATION_COL,
            PLAN_NUTRITION_TOTAL_MIXING_RATIO_COL,
            PLAN_NUTRITION_SEASON_PLANNING_ID_FK_COL
        )
        val selection = "$PLAN_NUTRITION_SEASON_PLANNING_ID_FK_COL=?"
        val selectionArgs = arrayOf(seasonPlanningId.toString())

        val cursor = db.query(PLAN_NUTRITION_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val planNutrition = PlanNutrition().apply {
                    id = cursor.getInt(cursor.getColumnIndex(PLAN_NUTRITION_ID_COL))
                    product = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_PRODUCT_COL))
                    product_name = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_PRODUCT_NAME_COL))
                    unit = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_UNIT_COL))
                    cost_per_unit = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_COST_PER_UNIT_COL))
                    application_rate = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_APPLICATION_RATE_COL))
                    time_of_application = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_TIME_OF_APPLICATION_COL))
                    method_of_application = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_METHOD_OF_APPLICATION_COL))
                    product_formulation = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_PRODUCT_FORMULATION_COL))
                    date_of_application = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_DATE_OF_APPLICATION_COL))
                    total_mixing_ratio = cursor.getString(cursor.getColumnIndex(PLAN_NUTRITION_TOTAL_MIXING_RATIO_COL))
                }

                // Add plan nutrition to list
                planNutritionList.add(planNutrition)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return planNutritionList
    }

    @SuppressLint("Range")
    fun getScoutingStationsForSeasonPlanning(seasonPlanningId: Int): List<ScoutingStation> {
        val scoutingStationList: MutableList<ScoutingStation> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            SCOUTING_STATION_ID_COL,
            SCOUTING_STATION_BAIT_STATION_COL,
            SCOUTING_STATION_TYPE_OF_BAIT_PROVIDED_COL,
            SCOUTING_STATION_FREQUENCY_COL,
            SCOUTING_STATION_SEASON_PLANNING_ID_FK_COL,

            )
        val selection = "$SCOUTING_STATION_SEASON_PLANNING_ID_FK_COL=?"
        val selectionArgs = arrayOf(seasonPlanningId.toString())

        val cursor = db.query(SCOUTING_STATION_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val scoutingStation = ScoutingStation().apply {

                    id = cursor.getInt(cursor.getColumnIndex(SCOUTING_STATION_ID_COL))
                    bait_station = cursor.getString(cursor.getColumnIndex(SCOUTING_STATION_BAIT_STATION_COL))
                    type_of_bait_provided = cursor.getString(cursor.getColumnIndex(SCOUTING_STATION_TYPE_OF_BAIT_PROVIDED_COL))
                    frequency = cursor.getString(cursor.getColumnIndex(SCOUTING_STATION_FREQUENCY_COL))
                    season_planning_id = cursor.getInt(cursor.getColumnIndex(SCOUTING_STATION_SEASON_PLANNING_ID_FK_COL))
                }

                // Add scouting station to list
                scoutingStationList.add(scoutingStation)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return scoutingStationList
    }

    @SuppressLint("Range")
    fun getPreventativePestsForSeasonPlanning(seasonPlanningId: Int): List<PreventativePest> {
        val preventativePestList: MutableList<PreventativePest> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            PREVENTATIVE_PEST_ID_COL,
            PREVENTATIVE_PEST_PEST_COL,
            PREVENTATIVE_PEST_PRODUCT_COL,
            PREVENTATIVE_PEST_CATEGORY_COL,
            PREVENTATIVE_PEST_FORMULATION_COL,
            PREVENTATIVE_PEST_DOSAGE_COL,
            PREVENTATIVE_PEST_UNIT_COL,
            PREVENTATIVE_PEST_COST_PER_UNIT_COL,
            PREVENTATIVE_PEST_VOLUME_OF_WATER_COL,
            PREVENTATIVE_PEST_FREQUENCY_OF_APPLICATION_COL,
            PREVENTATIVE_PEST_TOTAL_COST_COL,
            PREVENTATIVE_PEST_SEASON_PLANNING_ID_FK_COL
        )
        val selection = "$PREVENTATIVE_PEST_SEASON_PLANNING_ID_FK_COL=?"
        val selectionArgs = arrayOf(seasonPlanningId.toString())

        val cursor = db.query(PREVENTATIVE_PEST_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val preventativePest = PreventativePest().apply {
                    id = cursor.getInt(cursor.getColumnIndex(PREVENTATIVE_PEST_ID_COL))
                    pest = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_PEST_COL))
                    product = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_PRODUCT_COL))
                    category = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_CATEGORY_COL))
                    formulation = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_FORMULATION_COL))
                    dosage = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_DOSAGE_COL))
                    unit = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_UNIT_COL))
                    cost_per_unit = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_COST_PER_UNIT_COL))
                    volume_of_water = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_VOLUME_OF_WATER_COL))
                    frequency_of_application = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_FREQUENCY_OF_APPLICATION_COL))
                    total_cost = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_PEST_TOTAL_COST_COL))
                }

                // Add preventative pest to list
                preventativePestList.add(preventativePest)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return preventativePestList
    }

    @SuppressLint("Range")
    fun getPreventativeDiseasesForSeasonPlanning(seasonPlanningId: Int): List<PreventativeDisease> {
        val preventativeDiseaseList: MutableList<PreventativeDisease> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            PREVENTATIVE_DISEASE_ID_COL,
            PREVENTATIVE_DISEASE_DISEASE_COL,
            PREVENTATIVE_DISEASE_PRODUCT_COL,
            PREVENTATIVE_DISEASE_CATEGORY_COL,
            PREVENTATIVE_DISEASE_FORMULATION_COL,
            PREVENTATIVE_DISEASE_DOSAGE_COL,
            PREVENTATIVE_DISEASE_UNIT_COL,
            PREVENTATIVE_DISEASE_COST_PER_UNIT_COL,
            PREVENTATIVE_DISEASE_VOLUME_OF_WATER_COL,
            PREVENTATIVE_DISEASE_FREQUENCY_OF_APPLICATION_COL,
            PREVENTATIVE_DISEASE_TOTAL_COST_COL,
            PREVENTATIVE_DISEASE_SEASON_PLANNING_ID_FK_COL
        )
        val selection = "$PREVENTATIVE_DISEASE_SEASON_PLANNING_ID_FK_COL=?"
        val selectionArgs = arrayOf(seasonPlanningId.toString())

        val cursor = db.query(PREVENTATIVE_DISEASE_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val preventativeDisease = PreventativeDisease().apply {
                    id = cursor.getInt(cursor.getColumnIndex(PREVENTATIVE_DISEASE_ID_COL))
                    disease = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_DISEASE_COL))
                    product = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_PRODUCT_COL))
                    category = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_CATEGORY_COL))
                    formulation = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_FORMULATION_COL))
                    dosage = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_DOSAGE_COL))
                    unit = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_UNIT_COL))
                    cost_per_unit = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_COST_PER_UNIT_COL))
                    volume_of_water = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_VOLUME_OF_WATER_COL))
                    frequency_of_application = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_FREQUENCY_OF_APPLICATION_COL))
                    total_cost = cursor.getString(cursor.getColumnIndex(PREVENTATIVE_DISEASE_TOTAL_COST_COL))
                }

                // Add preventative disease to list
                preventativeDiseaseList.add(preventativeDisease)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return preventativeDiseaseList
    }

    @SuppressLint("Range")
    fun getPlanIrrigationForSeasonPlanning(seasonPlanningId: Int): List<PlanIrrigation> {
        val planIrrigationList: MutableList<PlanIrrigation> = ArrayList()
        val db = this.readableDatabase
        val columns = arrayOf(
            PLAN_IRRIGATION_ID_COL,
            PLAN_IRRIGATION_TYPE_OF_IRRIGATION_COL,
            PLAN_IRRIGATION_DISCHARGE_HOURS_COL,
            PLAN_IRRIGATION_FREQUENCY_COL,
            PLAN_IRRIGATION_COST_OF_FUEL_COL,
            PLAN_IRRIGATION_UNIT_COST_COL,
            PLAN_IRRIGATION_SEASON_PLANNING_ID_FK_COL
        )
        val selection = "$PLAN_IRRIGATION_SEASON_PLANNING_ID_FK_COL=?"
        val selectionArgs = arrayOf(seasonPlanningId.toString())

        val cursor = db.query(PLAN_IRRIGATION_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val planIrrigation = PlanIrrigation().apply {
                    id = cursor.getInt(cursor.getColumnIndex(PLAN_IRRIGATION_ID_COL))
                    type_of_irrigation = cursor.getString(cursor.getColumnIndex(PLAN_IRRIGATION_TYPE_OF_IRRIGATION_COL))
                    discharge_hours = cursor.getString(cursor.getColumnIndex(PLAN_IRRIGATION_DISCHARGE_HOURS_COL))
                    frequency = cursor.getString(cursor.getColumnIndex(PLAN_IRRIGATION_FREQUENCY_COL))
                    cost_of_fuel = cursor.getString(cursor.getColumnIndex(PLAN_IRRIGATION_COST_OF_FUEL_COL))
                    unit_cost = cursor.getString(cursor.getColumnIndex(PLAN_IRRIGATION_UNIT_COST_COL))
                }

                // Add plan irrigation to list
                planIrrigationList.add(planIrrigation)
            } while (cursor.moveToNext())

            cursor.close()
        }

        return planIrrigationList
    }




    private fun getExtScoutingStationsForExtensionService(extensionServiceId: Int): List<ExtScoutingStation> {
        val scoutingStations = mutableListOf<ExtScoutingStation>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $EXT_SCOUTING_STATION_TABLE_NAME WHERE $EXT_SCOUTING_STATION_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL = ?"
        val cursor = db.rawQuery(query, arrayOf(extensionServiceId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val station = ExtScoutingStation().apply {
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(EXT_SCOUTING_STATION_ID_COL))
                    scouting_method = cursor.getString(cursor.getColumnIndexOrThrow(EXT_SCOUTING_STATION_SCOUTING_METHOD_COL))
                    bait_station = cursor.getString(cursor.getColumnIndexOrThrow(EXT_SCOUTING_STATION_BAIT_STATION_COL))
                    pest_or_disease = cursor.getString(cursor.getColumnIndexOrThrow(EXT_SCOUTING_STATION_PEST_OR_DISEASE_COL))
                    management = cursor.getString(cursor.getColumnIndexOrThrow(EXT_SCOUTING_STATION_MANAGEMENT_COL))
                    extension_service_registration_id = extensionServiceId
                }
                scoutingStations.add(station)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return scoutingStations
    }

    private fun getPesticidesUsedForExtensionService(extensionServiceId: Int): List<PesticideUsed> {
        val pesticidesUsed = mutableListOf<PesticideUsed>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $PESTICIDE_USED_TABLE_NAME WHERE $PESTICIDE_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL = ?"
        val cursor = db.rawQuery(query, arrayOf(extensionServiceId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val pesticideUsed = PesticideUsed().apply {
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(PESTICIDE_USED_ID_COL))
                    register = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_REGISTER_COL))
                    product = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_PRODUCT_COL))
                    category = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_CATEGORY_COL))
                    formulation = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_FORMULATION_COL))
                    dosage = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_DOSAGE_COL))
                    unit = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_UNIT_COL))
                    cost_per_unit = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_COST_PER_UNIT_COL))
                    volume_of_water = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_VOLUME_OF_WATER_COL))
                    frequency_of_application = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_FREQUENCY_OF_APPLICATION_COL))
                    total_cost = cursor.getString(cursor.getColumnIndexOrThrow(PESTICIDE_USED_TOTAL_COST_COL))
                    extension_service_registration_id = extensionServiceId
                }
                pesticidesUsed.add(pesticideUsed)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return pesticidesUsed
    }

    private fun getFertilizersUsedForExtensionService(extensionServiceId: Int): List<FertilizerUsed> {
        val fertilizersUsed = mutableListOf<FertilizerUsed>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $FERTILIZER_USED_TABLE_NAME WHERE $FERTILIZER_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL = ?"
        val cursor = db.rawQuery(query, arrayOf(extensionServiceId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val fertilizerUsed = FertilizerUsed().apply {
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(FERTILIZER_USED_ID_COL))
                    register = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_REGISTER_COL))
                    product = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_PRODUCT_COL))
                    category = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_CATEGORY_COL))
                    formulation = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_FORMULATION_COL))
                    dosage = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_DOSAGE_COL))
                    unit = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_UNIT_COL))
                    cost_per_unit = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_COST_PER_UNIT_COL))
                    volume_of_water = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_VOLUME_OF_WATER_COL))
                    frequency_of_application = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_FREQUENCY_OF_APPLICATION_COL))
                    total_cost = cursor.getString(cursor.getColumnIndexOrThrow(FERTILIZER_USED_TOTAL_COST_COL))
                    extension_service_registration_id = extensionServiceId
                }
                fertilizersUsed.add(fertilizerUsed)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return fertilizersUsed
    }

    private fun getForecastYieldsForExtensionService(extensionServiceId: Int): List<ForecastYield> {
        val forecastYields = mutableListOf<ForecastYield>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $FORECAST_YIELD_TABLE_NAME WHERE $FORECAST_YIELD_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL = ?"
        val cursor = db.rawQuery(query, arrayOf(extensionServiceId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val forecastYield = ForecastYield().apply {
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(FORECAST_YIELD_ID_COL))
                    crop_population_pc = cursor.getString(cursor.getColumnIndexOrThrow(FORECAST_YIELD_CROP_POPULATION_PC_COL))
                    yield_forecast_pc = cursor.getString(cursor.getColumnIndexOrThrow(FORECAST_YIELD_YIELD_FORECAST_PC_COL))
                    forecast_quality = cursor.getString(cursor.getColumnIndexOrThrow(FORECAST_YIELD_FORECAST_QUALITY_COL))
                    ta_comments = cursor.getString(cursor.getColumnIndexOrThrow(FORECAST_YIELD_TA_COMMENTS_COL))
                    extension_service_registration_id = extensionServiceId
                }
                forecastYields.add(forecastYield)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return forecastYields
    }

    private fun getMarketProducesForExtensionService(extensionServiceId: Int): List<MarketProduce> {
        val marketProduces = mutableListOf<MarketProduce>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $MARKET_PRODUCE_TABLE_NAME WHERE $MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL = ?"
        val cursor = db.rawQuery(query, arrayOf(extensionServiceId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val marketProduce = MarketProduce().apply {
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_ID_COL))
                    product = cursor.getString(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_PRODUCT_COL))
                    product_category = cursor.getString(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_PRODUCT_CATEGORY_COL))
                    acerage = cursor.getString(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_ACERAGE_COL))
                    season_planning_id = cursor.getInt(cursor.getColumnIndexOrThrow(MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL))
                    extension_service_id = extensionServiceId // Set extension service ID
                }
                marketProduces.add(marketProduce)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return marketProduces
    }


    // Offline training data
    @get:SuppressLint("Range")
    val offlineTrainings: List<Training>
        get() {
            val trainingList: MutableList<Training> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                TRAINING_ID_COL,
                TRAINING_COURSE_NAME_COL,
                TRAINING_TRAINER_NAME_COL,
                TRAINING_BUYING_CENTER_COL,
                TRAINING_COURSE_DESCRIPTION_COL,
                TRAINING_DATE_OF_TRAINING_COL,
                TRAINING_CONTENT_OF_TRAINING_COL,
                TRAINING_VENUE_COL,
                TRAINING_PARTICIPANTS_COL, // This should be JSON string
                TRAINING_USER_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(TRAINING_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val training = Training().apply {
                        id = cursor.getInt(cursor.getColumnIndex(TRAINING_ID_COL))
                        course_name = cursor.getString(cursor.getColumnIndex(TRAINING_COURSE_NAME_COL))
                        trainer_name = cursor.getString(cursor.getColumnIndex(TRAINING_TRAINER_NAME_COL))
                        buying_center = cursor.getString(cursor.getColumnIndex(TRAINING_BUYING_CENTER_COL))
                        course_description = cursor.getString(cursor.getColumnIndex(TRAINING_COURSE_DESCRIPTION_COL))
                        date_of_training = cursor.getString(cursor.getColumnIndex(TRAINING_DATE_OF_TRAINING_COL))
                        content_of_training = cursor.getString(cursor.getColumnIndex(TRAINING_CONTENT_OF_TRAINING_COL))
                        venue = cursor.getString(cursor.getColumnIndex(TRAINING_VENUE_COL))

                        // Parse participants JSON string to Map<String, String>
                        val participantsJson = cursor.getString(cursor.getColumnIndex(TRAINING_PARTICIPANTS_COL))
                        participants = parseJsonObject(participantsJson)?.let { jsonObject ->
                            jsonObject.entrySet().associate {
                                it.key to it.value.asString
                            }
                        }

                        user_id = cursor.getString(cursor.getColumnIndex(TRAINING_USER_ID_FK_COL))
                    }

                    // Add training to list
                    trainingList.add(training)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return trainingList
        }

    fun deleteTraining(trainingId: Int) {
        val db = this.writableDatabase
        val whereClause = "$TRAINING_ID_COL=?"
        val whereArgs = arrayOf(trainingId.toString())

        val rowsDeleted = db.delete(TRAINING_TABLE_NAME, whereClause, whereArgs)
        if (rowsDeleted > 0) {
            Log.d("PostTrainingData", "Successfully deleted training with ID: $trainingId")
        } else {
            Log.e("PostTrainingData", "Failed to delete training with ID: $trainingId")
        }
    }

    // Utility function to parse JSON string
    private fun parseJsonObject(json: String?): JsonObject? {
        return json?.let {
            try {
                JSONObject(it).toString().let { jsonString ->
                    JsonParser.parseString(jsonString).asJsonObject
                }
            } catch (e: JSONException) {
                null
            }
        }
    }


    @get:SuppressLint("Range")
    val offlineAttendances: List<Attendance>
        get() {
            val attendanceList: MutableList<Attendance> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                ATTENDANCE_ID_COL,
                ATTENDANCE_ATTENDANCE_COL,
                ATTENDANCE_TRAINING_ID_COL,
                ATTENDANCE_USER_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(ATTENDANCE_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val attendance = Attendance().apply {
                        id = cursor.getInt(cursor.getColumnIndex(ATTENDANCE_ID_COL))
                        attendance = cursor.getString(cursor.getColumnIndex(ATTENDANCE_ATTENDANCE_COL))
                        training_id = cursor.getInt(cursor.getColumnIndex(ATTENDANCE_TRAINING_ID_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(ATTENDANCE_USER_ID_FK_COL))
                    }

                    attendanceList.add(attendance)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return attendanceList
        }

    @get:SuppressLint("Range")
    val offlineFarmerPriceDistributions: List<FarmerPriceDistribution>
        get() {
            val priceDistributionList: MutableList<FarmerPriceDistribution> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                FARMER_PRICE_DISTRIBUTION_ID_COL,
                FARMER_PRICE_DISTRIBUTION_HUB_COL,
                FARMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL,
                FARMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL,
                FARMER_PRICE_DISTRIBUTION_UNIT_COL,
                FARMER_PRICE_DISTRIBUTION_DATE_COL,
                FARMER_PRICE_DISTRIBUTION_COMMENTS_COL,
                FARMER_PRICE_DISTRIBUTION_SOLD_COL,
                FARMER_PRICE_DISTRIBUTION_USER_ID_FK_COL,
                FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(FARMER_PRICE_DISTRIBUTION_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val priceDistribution = FarmerPriceDistribution().apply {
                        id = cursor.getInt(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_ID_COL))
                        hub = cursor.getString(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_HUB_COL))
                        buying_center = cursor.getString(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL))
                        online_price = cursor.getFloat(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL))
                        unit = cursor.getString(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_UNIT_COL))
                        date = cursor.getString(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_DATE_COL))
                        comments = cursor.getString(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_COMMENTS_COL))
                        sold = cursor.getInt(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_SOLD_COL)) == 1 // Convert to Boolean
                        user_id = cursor.getString(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_USER_ID_FK_COL))
                        produce_id = cursor.getInt(cursor.getColumnIndex(FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL))
                    }

                    // Add priceDistribution to list
                    priceDistributionList.add(priceDistribution)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return priceDistributionList
        }

    fun deleteFarmerPriceDistribution(id: Int) {
        val db = this.writableDatabase
        db.delete(FARMER_PRICE_DISTRIBUTION_TABLE_NAME, "$FARMER_PRICE_DISTRIBUTION_ID_COL = ?", arrayOf(id.toString()))
    }

    @get:SuppressLint("Range")
    val offlineCustomerPriceDistributions: List<CustomerPriceDistribution>
        get() {
            val priceDistributionList: MutableList<CustomerPriceDistribution> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                CUSTOMER_PRICE_DISTRIBUTION_ID_COL,
                CUSTOMER_PRICE_DISTRIBUTION_HUB_COL,
                CUSTOMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL,
                CUSTOMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL,
                CUSTOMER_PRICE_DISTRIBUTION_UNIT_COL,
                CUSTOMER_PRICE_DISTRIBUTION_DATE_COL,
                CUSTOMER_PRICE_DISTRIBUTION_COMMENTS_COL,
                CUSTOMER_PRICE_DISTRIBUTION_SOLD_COL,
                CUSTOMER_PRICE_DISTRIBUTION_USER_ID_FK_COL,
                CUSTOMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(CUSTOMER_PRICE_DISTRIBUTION_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val priceDistribution = CustomerPriceDistribution().apply {
                        id = cursor.getInt(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_ID_COL))
                        hub = cursor.getString(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_HUB_COL))
                        buying_center = cursor.getString(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL))
                        online_price = cursor.getFloat(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL))
                        unit = cursor.getString(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_UNIT_COL))
                        date = cursor.getString(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_DATE_COL))
                        comments = cursor.getString(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_COMMENTS_COL))
                        sold = cursor.getInt(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_SOLD_COL)) == 1 // Convert to Boolean
                        user_id = cursor.getString(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_USER_ID_FK_COL))
                        produce_id = cursor.getInt(cursor.getColumnIndex(CUSTOMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL))
                    }

                    // Add priceDistribution to list
                    priceDistributionList.add(priceDistribution)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return priceDistributionList
        }

    fun deleteCustomerPriceDistribution(id: Int) {
        val db = this.writableDatabase
        db.delete(CUSTOMER_PRICE_DISTRIBUTION_TABLE_NAME, "$CUSTOMER_PRICE_DISTRIBUTION_ID_COL = ?", arrayOf(id.toString()))
    }

    @get:SuppressLint("Range")
    val offlineBuyingFarmers: List<BuyingFarmer>
        get() {
            val buyingFarmerList: MutableList<BuyingFarmer> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                BUYING_FARMER_ID_COL,
                BUYING_FARMER_BUYING_CENTER_COL,
                BUYING_FARMER_PRODUCER_COL,
                BUYING_FARMER_PRODUCE_COL,
                BUYING_FARMER_GRN_NUMBER_COL,
                BUYING_FARMER_UNIT_COL,
                BUYING_FARMER_QUALITY_COL,
                BUYING_FARMER_ACTION_COL,
                BUYING_FARMER_WEIGHT_COL,
                BUYING_FARMER_LOADED_COL,
                BUYING_FARMER_USER_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(BUYING_FARMER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val buyingFarmer = BuyingFarmer().apply {
                        id = cursor.getInt(cursor.getColumnIndex(BUYING_FARMER_ID_COL))
                        buying_center = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_BUYING_CENTER_COL))
                        producer = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_PRODUCER_COL))
                        produce = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_PRODUCE_COL))
                        grn_number = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_GRN_NUMBER_COL))
                        unit = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_UNIT_COL))

                        // Parse quality JSON string to Map<String, Map<String, String>>
                        val qualityJson = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_QUALITY_COL))
                        val parsedJson: Map<String, JsonElement>? = parseJsonObject(qualityJson)?.asJsonObject?.entrySet()?.associate {
                            it.key to it.value
                        }

                        quality = parsedJson?.let { jsonObject ->
                            jsonObject.mapValues { entry ->
                                entry.value.asJsonObject.entrySet().associate { innerEntry ->
                                    innerEntry.key to innerEntry.value.asString
                                }
                            }
                        }

                        action = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_ACTION_COL))
                        weight = cursor.getDouble(cursor.getColumnIndex(BUYING_FARMER_WEIGHT_COL))
                        loaded = cursor.getInt(cursor.getColumnIndex(BUYING_FARMER_LOADED_COL)) == 1 // Convert to Boolean
                        user_id = cursor.getString(cursor.getColumnIndex(BUYING_FARMER_USER_ID_FK_COL))
                    }

                    // Add buyingFarmer to list
                    buyingFarmerList.add(buyingFarmer)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return buyingFarmerList
        }

    fun deleteBuyingFarmer(id: Int) {
        val db = this.writableDatabase
        db.delete(BUYING_FARMER_TABLE_NAME, "$BUYING_FARMER_ID_COL = ?", arrayOf(id.toString()))
    }


    @get:SuppressLint("Range")
    val offlineBuyingCustomers: List<BuyingCustomer>
        get() {
            val buyingCustomerList: MutableList<BuyingCustomer> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                BUYING_CUSTOMER_ID_COL,
                BUYING_CUSTOMER_PRODUCE_COL,
                BUYING_CUSTOMER_CUSTOMER_COL,
                BUYING_CUSTOMER_GRN_NUMBER_COL,
                BUYING_CUSTOMER_UNIT_COL,
                BUYING_CUSTOMER_QUALITY_COL,
                BUYING_CUSTOMER_ACTION_COL,
                BUYING_CUSTOMER_WEIGHT_COL,
                BUYING_CUSTOMER_ONLINE_PRICE_COL,
                BUYING_CUSTOMER_LOADED_COL,
                BUYING_CUSTOMER_USER_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(BUYING_CUSTOMER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val buyingCustomer = BuyingCustomer().apply {
                        id = cursor.getInt(cursor.getColumnIndex(BUYING_CUSTOMER_ID_COL))
                        produce = cursor.getString(cursor.getColumnIndex(BUYING_CUSTOMER_PRODUCE_COL))
                        customer = cursor.getString(cursor.getColumnIndex(BUYING_CUSTOMER_CUSTOMER_COL))
                        grn_number = cursor.getString(cursor.getColumnIndex(BUYING_CUSTOMER_GRN_NUMBER_COL))
                        unit = cursor.getString(cursor.getColumnIndex(BUYING_CUSTOMER_UNIT_COL))

                        // Parse quality JSON string to Map<String, Map<String, String>>
                        val qualityJson = cursor.getString(cursor.getColumnIndex(BUYING_CUSTOMER_QUALITY_COL))
                        val parsedJson: Map<String, JsonElement>? = parseJsonObject(qualityJson)?.asJsonObject?.entrySet()?.associate {
                            it.key to it.value
                        }

                        quality = parsedJson?.let { jsonObject ->
                            jsonObject.mapValues { entry ->
                                entry.value.asJsonObject.entrySet().associate { innerEntry ->
                                    innerEntry.key to innerEntry.value.asString
                                }
                            }
                        }
                        action = cursor.getString(cursor.getColumnIndex(BUYING_CUSTOMER_ACTION_COL))
                        weight = cursor.getDouble(cursor.getColumnIndex(BUYING_CUSTOMER_WEIGHT_COL))
                        online_price = cursor.getDouble(cursor.getColumnIndex(BUYING_CUSTOMER_ONLINE_PRICE_COL))
                        loaded = cursor.getInt(cursor.getColumnIndex(BUYING_CUSTOMER_LOADED_COL)) == 1 // Convert to Boolean
                        user_id = cursor.getString(cursor.getColumnIndex(BUYING_CUSTOMER_USER_ID_FK_COL))
                    }

                    // Add buyingCustomer to list
                    buyingCustomerList.add(buyingCustomer)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return buyingCustomerList
        }

    fun deleteBuyingCustomer(id: Int) {
        val db = this.writableDatabase
        db.delete(BUYING_CUSTOMER_TABLE_NAME, "$BUYING_CUSTOMER_ID_COL = ?", arrayOf(id.toString()))
    }



    @get:SuppressLint("Range")
    val offlineLoadings: List<Loading>
        get() {
            val loadingList: MutableList<Loading> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                LOADING_ID_COL,
                LOADING_GRN_COL,
                LOADING_TOTAL_WEIGHT_COL,
                LOADING_TRUCK_LOADING_NUMBER_COL,
                LOADING_FROM_COL,
                "`$LOADING_TO_COL`",
                LOADING_COMMENT_COL,
                LOADING_OFFLOADED_COL,
                LOADING_USER_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(LOADING_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val loading = Loading().apply {
                        id = cursor.getInt(cursor.getColumnIndex(LOADING_ID_COL))
                        grn = cursor.getString(cursor.getColumnIndex(LOADING_GRN_COL))
                        total_weight = cursor.getString(cursor.getColumnIndex(LOADING_TOTAL_WEIGHT_COL))
                        truck_loading_number = cursor.getString(cursor.getColumnIndex(LOADING_TRUCK_LOADING_NUMBER_COL))
                        from_ = cursor.getString(cursor.getColumnIndex(LOADING_FROM_COL))
                        to = cursor.getString(cursor.getColumnIndex(LOADING_TO_COL)) // No backticks here
                        comment = cursor.getString(cursor.getColumnIndex(LOADING_COMMENT_COL))
                        offloaded = cursor.getInt(cursor.getColumnIndex(LOADING_OFFLOADED_COL)) == 1 // Convert to Boolean
                        user_id = cursor.getString(cursor.getColumnIndex(LOADING_USER_ID_FK_COL))
                    }
                    loadingList.add(loading)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return loadingList
        }

    fun deleteLoading(id: Int) {
        val db = writableDatabase
        db.delete(LOADING_TABLE_NAME, "$LOADING_ID_COL = ?", arrayOf(id.toString()))
    }

    @get:SuppressLint("Range")
    val offlineOffloadings: List<Offloading>
        get() {
            val offloadingList: MutableList<Offloading> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                OFFLOADING_ID_COL,
                OFFLOADING_OFFLOADED_LOAD_COL,
                OFFLOADING_TOTAL_WEIGHT_COL,
                OFFLOADING_TRUCK_OFFLOADING_NUMBER_COL,
                OFFLOADING_COMMENT_COL,
                OFFLOADING_USER_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(OFFLOADING_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val offloading = Offloading().apply {
                        id = cursor.getInt(cursor.getColumnIndex(OFFLOADING_ID_COL))
                        offloaded_load = cursor.getString(cursor.getColumnIndex(OFFLOADING_OFFLOADED_LOAD_COL))
                        total_weight = cursor.getDouble(cursor.getColumnIndex(OFFLOADING_TOTAL_WEIGHT_COL))
                        truck_offloading_number = cursor.getString(cursor.getColumnIndex(OFFLOADING_TRUCK_OFFLOADING_NUMBER_COL))
                        comment = cursor.getString(cursor.getColumnIndex(OFFLOADING_COMMENT_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(OFFLOADING_USER_ID_FK_COL))
                    }

                    // Add offloading to list
                    offloadingList.add(offloading)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return offloadingList
        }

    fun deleteOffloading(id: Int) {
        val db = writableDatabase
        db.delete(OFFLOADING_TABLE_NAME, "$OFFLOADING_ID_COL = ?", arrayOf(id.toString()))
    }

    @get:SuppressLint("Range")
    val offlineRuralWorkers: List<RuralWorker>
        get() {
            val ruralWorkerList: MutableList<RuralWorker> = ArrayList()
            val db = this.readableDatabase
            val columns = arrayOf(
                RURAL_WORKER_ID_COL,
                RURAL_WORKER_OTHER_NAME_COL,
                RURAL_WORKER_LAST_NAME_COL,
                RURAL_WORKER_RURAL_WORKER_CODE_COL,
                RURAL_WORKER_ID_NUMBER_COL,
                RURAL_WORKER_GENDER_COL,
                RURAL_WORKER_DATE_OF_BIRTH_COL,
                RURAL_WORKER_EMAIL_COL,
                RURAL_WORKER_PHONE_NUMBER_COL,
                RURAL_WORKER_EDUCATION_LEVEL_COL,
                RURAL_WORKER_SERVICE_COL,
                RURAL_WORKER_OTHER_COL,
                RURAL_WORKER_COUNTY_COL,
                RURAL_WORKER_SUB_COUNTY_COL,
                RURAL_WORKER_WARD_COL,
                RURAL_WORKER_VILLAGE_COL,
                RURAL_WORKER_USER_ID_FK_COL,
                IS_OFFLINE_COL
            )
            val selection = "$IS_OFFLINE_COL=?"
            val selectionArgs = arrayOf("1")

            val cursor = db.query(RURAL_WORKER_TABLE_NAME, columns, selection, selectionArgs, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val ruralWorker = RuralWorker().apply {
                        id = cursor.getInt(cursor.getColumnIndex(RURAL_WORKER_ID_COL))
                        other_name = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_OTHER_NAME_COL))
                        last_name = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_LAST_NAME_COL))
                        rural_worker_code = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_RURAL_WORKER_CODE_COL))
                        id_number = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_ID_NUMBER_COL))
                        gender = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_GENDER_COL))
                        date_of_birth = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_DATE_OF_BIRTH_COL))
                        email = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_EMAIL_COL))
                        phone_number = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_PHONE_NUMBER_COL))
                        education_level = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_EDUCATION_LEVEL_COL))
                        service = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_SERVICE_COL))
                        other = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_OTHER_COL))
                        county = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_COUNTY_COL))
                        sub_county = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_SUB_COUNTY_COL))
                        ward = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_WARD_COL))
                        village = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_VILLAGE_COL))
                        user_id = cursor.getString(cursor.getColumnIndex(RURAL_WORKER_USER_ID_FK_COL))
                    }

                    // Add ruralWorker to list
                    ruralWorkerList.add(ruralWorker)
                } while (cursor.moveToNext())

                cursor.close()
            }

            return ruralWorkerList
        }

    fun deleteRuralWorker(id: Int) {
        val db = this.writableDatabase
        db.delete(RURAL_WORKER_TABLE_NAME, "$RURAL_WORKER_ID_COL = ?", arrayOf(id.toString()))
    }

    companion object {
        private const val DB_NAME = "farmdatasqldb"
        private const val DB_VERSION = 5

        // Table names
        const val usersTableName: String = "users"

        //    hubs
        const val HUB_TABLE_NAME: String = "hubs"
        const val KEY_CONTACTS_TABLE_NAME: String = "KeyContacts"
        const val BUYING_CENTER_TABLE_NAME: String = "buyingCenters"
        const val MEMBERS_TABLE_NAME: String = "members"
        const val CUSTOM_USER_TABLE_NAME: String = "CustomUsers"
        const val HUB_USER_TABLE_NAME: String = "hubUsers"
        const val HQ_USER_TABLE_NAME: String = "HQUsers"
        const val PROCESSING_USER_TABLE_NAME: String = "ProcessingUsers"
        const val INDIVIDUAL_LOGISTICIAN_USER_TABLE_NAME: String = "individualLogisticianusers"
        const val ORGANISATION_LOGISTICIAN_USER_TABLE_NAME: String = "OrganisationLogisticianUsers"
        const val CAR_TABLE_NAME: String = "cars"
        const val INDIVIDUAL_CUSTOMER_USER_TABLE_NAME: String = "individualCustomerusers"
        const val ORGANISATION_CUSTOMER_USER_TABLE_NAME: String = "OrganisationCustomerusers"
        const val PRODUCT_TABLE_NAME: String = "products"
        const val PRODUCER_BIODATA_TABLE_NAME: String = "producersBiodata"
        const val COMMERCIAL_PRODUCE_TABLE_NAME: String = "commercialProduces"
        const val DOMESTIC_PRODUCE_TABLE_NAME: String = "domesticProduces"
        const val FARMER_FIELD_REGISTRATION_TABLE_NAME: String = "farmerFieldRegistrations"
        const val CIG_PRODUCER_BIODATA_TABLE_NAME: String = "cigProducersBiodata"
        const val CIG_FARMER_FIELD_REGISTRATION_TABLE_NAME: String = "cigFarmerFieldRegistrations"
        const val SEASON_PLANNING_TABLE_NAME: String = "seasonsPlanning"
        const val MARKET_PRODUCE_TABLE_NAME: String = "marketProduces"
        const val PLAN_NUTRITION_TABLE_NAME: String = "planNutritions"
        const val SCOUTING_STATION_TABLE_NAME: String = "scoutingStations"
        const val PREVENTATIVE_DISEASE_TABLE_NAME: String = "preventativeDiseases"
        const val PREVENTATIVE_PEST_TABLE_NAME: String = "preventativePests"
        const val PLAN_IRRIGATION_TABLE_NAME: String = "planIrrigatons"
        const val EXTENSION_SERVICE_TABLE_NAME: String = "extensionServices"
        const val EXT_SCOUTING_STATION_TABLE_NAME: String = "extScoutingStations"
        const val PESTICIDE_USED_TABLE_NAME: String = "pesticidesUsed"
        const val FERTILIZER_USED_TABLE_NAME: String = "fertilizersUsed"
        const val FORECAST_YIELD_TABLE_NAME: String = "forecastYields"
        const val TRAINING_TABLE_NAME: String = "trainings"
        const val ATTENDANCE_TABLE_NAME: String = "attendances"
        const val FARMER_PRICE_DISTRIBUTION_TABLE_NAME: String = "farmerPriceDistributions"
        const val CUSTOMER_PRICE_DISTRIBUTION_TABLE_NAME: String = "customerPriceDistributions"
        const val BUYING_FARMER_TABLE_NAME: String = "buyingFarmers"
        const val QUARANTINE_TABLE_NAME: String = "quarantines"
        const val BUYING_CUSTOMER_TABLE_NAME: String = "buyingCustomers"
        const val PAYMENT_FARMER_TABLE_NAME: String = "paymentFarmers"
        const val PAYMENT_CUSTOMER_TABLE_NAME: String = "paymentCustomers"
        const val PLAN_JOURNEY_TABLE_NAME: String = "planJournies"
        const val DISPATCH_INPUT_TABLE_NAME: String = "dispatchInputs"
        const val LOADING_TABLE_NAME: String = "loadings"
        const val OFFLOADING_TABLE_NAME: String = "offloadings"
        const val RURAL_WORKER_TABLE_NAME: String = "ruralWorkers"

        // Users Table Columns
        const val idCol: String = "id"
        const val lastNameCol: String = "last_name"
        const val otherNameCol: String = "other_name"
        const val userTypeCol: String = "user_type"
        const val emailCol: String = "email"
        const val roleCol: String = "role"
        const val emailVerifiedCol: String = "email_verified"
        const val passwordCol: String = "password"
        const val verificationTokenCol: String = "verification_token"
        const val createdAtCol: String = "created_at"
        const val updatedAtCol: String = "updated_at"

        // Hubs Table Columns
        const val HUB_ID_COL: String = "id"
        const val regionCol: String = "region"
        const val hubNameCol: String = "hub_name"
        const val hubCodeCol: String = "hub_code"
        const val addressCol: String = "address"
        const val yearEstablishedCol: String = "year_established"
        const val ownershipCol: String = "ownership"
        const val floorSizeCol: String = "floor_size"
        const val facilitiesCol: String = "facilities"
        const val inputCenterCol: String = "input_center"
        const val typeOfBuildingCol: String = "type_of_building"
        const val longitudeCol: String = "longitude"
        const val latitudeCol: String = "latitude"
        const val hubUserIdCol: String = "user_id"

        // KeyContacts Table Columns
        const val KEY_CONTACT_ID_COL: String = "id"
        const val KEY_CONTACT_OTHER_NAME_COL: String = "other_name"
        const val KEY_CONTACT_LAST_NAME_COL: String = "last_name"
        const val ID_NUMBER_COL: String = "id_number"
        const val GENDER_COL: String = "gender"
        const val KEY_CONTACT_ROLE_COL: String = "role"
        const val DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val KEY_CONTACT_EMAIL_COL: String = "email"
        const val PHONE_NUMBER_COL: String = "phone_number"
        const val HUB_ID_FK_COL: String = "hub_id" // Foreign key
        const val BUYING_CENTER_ID_FK_COL: String = "buying_center_id"

        // BuyingCenter Table Columns";
        const val BUYING_CENTER_ID_COL: String = "id"
        const val HUB_COL: String = "hub"
        const val COUNTY_COL: String = "county"
        const val SUB_COUNTY_COL: String = "sub_county"
        const val WARD_COL: String = "ward"
        const val VILLAGE_COL: String = "village"
        const val BUYING_CENTER_NAME_COL: String = "buying_center_name"
        const val BUYING_CENTER_CODE_COL: String = "buying_center_code"
        const val BUYING_CENTER_ADDRESS_COL: String = "address"
        const val BUYING_CENTER_YEAR_ESTABLISHED_COL: String = "year_established"
        const val BUYING_CENTER_OWNERSHIP_COL: String = "ownership"
        const val BUYING_CENTER_FLOOR_SIZE_COL: String = "floor_size"
        const val BUYING_CENTER_FACILITIES_COL: String = "facilities"
        const val BUYING_CENTER_INPUT_CENTER_COL: String = "input_center"
        const val BUYING_CENTER_TYPE_OF_BUILDING_COL: String = "type_of_building"
        const val BUYING_CENTER_LOCATION_COL: String = "location"
        const val BUYING_CENTER_USER_ID_COL: String = "user_id"

        // cig table
        const val CIG_TABLE_NAME: String = "cigs"
        const val CIG_ID_COL: String = "id"
        const val CIG_HUB_COL: String = "hub"
        const val CIG_NAME_COL: String = "cig_name"
        const val NO_OF_MEMBERS_COL: String = "no_of_members"
        const val DATE_ESTABLISHED_COL: String = "date_established"
        const val CONSTITUTION_COL: String = "constitution"
        const val REGISTRATION_COL: String = "registration"
        const val ELECTIONS_HELD_COL: String = "elections_held"
        const val DATE_OF_LAST_ELECTIONS_COL: String = "date_of_last_elections"
        const val MEETING_VENUE_COL: String = "meeting_venue"
        const val FREQUENCY_COL: String = "frequency"
        const val SCHEDULED_MEETING_DAY_COL: String = "scheduled_meeting_day"
        const val SCHEDULED_MEETING_TIME_COL: String = "scheduled_meeting_time"
        const val CIG_USER_ID_COL: String = "user_id"

        // member table
        const val MEMBER_ID_COL: String = "id"
        const val MEMBER_OTHER_NAME_COL: String = "other_name"
        const val MEMBER_LAST_NAME_COL: String = "last_name"
        const val MEMBER_GENDER_COL: String = "gender"
        const val MEMBER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val MEMBER_EMAIL_COL: String = "email"
        const val MEMBER_PHONE_NUMBER_COL: String = "phone_number"
        const val MEMBER_ID_NUMBER_COL: String = "id_number"
        const val PRODUCT_INVOLVED_COL: String = "product_involved"
        const val HECTORAGE_REGISTERED_UNDER_CIG_COL: String = "hectorage_registered_under_cig"
        const val MEMBER_CIG_ID_COL: String = "cig_id"

        // CustomUser Table Columns
        const val CUSTOM_USER_ID_COL: String = "id"
        const val CUSTOM_USER_OTHER_NAME_COL: String = "other_name"
        const val CUSTOM_USER_LAST_NAME_COL: String = "last_name"
        const val CUSTOM_USER_STAFF_CODE_COL: String = "staff_code"
        const val CUSTOM_USER_ID_NUMBER_COL: String = "id_number"
        const val CUSTOM_USER_GENDER_COL: String = "gender"
        const val CUSTOM_USER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val CUSTOM_USER_EMAIL_COL: String = "email"
        const val CUSTOM_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val CUSTOM_USER_EDUCATION_LEVEL_COL: String = "education_level"
        const val CUSTOM_USER_ROLE_COL: String = "role"
        const val CUSTOM_USER_REPORTING_TO_COL: String = "reporting_to"
        const val CUSTOM_USER_USER_ID_COL: String = "user_id"

        // HubUser Table Columns
        const val HUB_USER_ID_COL: String = "id"
        const val HUB_USER_OTHER_NAME_COL: String = "other_name"
        const val HUB_USER_LAST_NAME_COL: String = "last_name"
        const val HUB_USER_CODE_COL: String = "code"
        const val HUB_USER_ROLE_COL: String = "role"
        const val HUB_USER_ID_NUMBER_COL: String = "id_number"
        const val HUB_USER_GENDER_COL: String = "gender"
        const val HUB_USER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val HUB_USER_EMAIL_COL: String = "email"
        const val HUB_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val HUB_USER_EDUCATION_LEVEL_COL: String = "education_level"
        const val HUB_USER_HUB_COL: String = "hub"
        const val HUB_USER_BUYING_CENTER_COL: String = "buying_center"
        const val HUB_USER_COUNTY_COL: String = "county"
        const val HUB_USER_SUB_COUNTY_COL: String = "sub_county"
        const val HUB_USER_WARD_COL: String = "ward"
        const val HUB_USER_VILLAGE_COL: String = "village"
        const val HUB_USER_USER_ID_COL: String = "user_id"

        // HQUser Table Columns
        const val HQ_USER_ID_COL: String = "id"
        const val HQ_USER_OTHER_NAME_COL: String = "other_name"
        const val HQ_USER_LAST_NAME_COL: String = "last_name"
        const val HQ_USER_STAFF_CODE_COL: String = "staff_code"
        const val HQ_USER_DEPARTMENT_COL: String = "department"
        const val HQ_USER_ID_NUMBER_COL: String = "id_number"
        const val HQ_USER_GENDER_COL: String = "gender"
        const val HQ_USER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val HQ_USER_EMAIL_COL: String = "email"
        const val HQ_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val HQ_USER_EDUCATION_LEVEL_COL: String = "education_level"
        const val HQ_USER_ROLE_COL: String = "role"
        const val HQ_USER_REPORTING_TO_COL: String = "reporting_to"
        const val HQ_USER_RELATED_ROLES_COL: String = "related_roles"
        const val HQ_USER_USER_ID_COL: String = "user_id"

        // ProcessingUser Table Columns
        const val PROCESSING_USER_ID_COL: String = "id"
        const val PROCESSING_USER_OTHER_NAME_COL: String = "other_name"
        const val PROCESSING_USER_LAST_NAME_COL: String = "last_name"
        const val PROCESSING_USER_PROCESSOR_CODE_COL: String = "processor_code"
        const val PROCESSING_USER_PROCESSING_PLANT_COL: String = "processing_plant"
        const val PROCESSING_USER_ID_NUMBER_COL: String = "id_number"
        const val PROCESSING_USER_GENDER_COL: String = "gender"
        const val PROCESSING_USER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val PROCESSING_USER_EMAIL_COL: String = "email"
        const val PROCESSING_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val PROCESSING_USER_EDUCATION_LEVEL_COL: String = "education_level"
        const val PROCESSING_USER_HUB_COL: String = "hub"
        const val PROCESSING_USER_BUYING_CENTER_COL: String = "buying_center"
        const val PROCESSING_USER_COUNTY_COL: String = "county"
        const val PROCESSING_USER_SUB_COUNTY_COL: String = "sub_county"
        const val PROCESSING_USER_WARD_COL: String = "ward"
        const val PROCESSING_USER_VILLAGE_COL: String = "village"
        const val PROCESSING_USER_USER_ID_COL: String = "user_id"

        // IndividualLogisticianUser Table Columns
        const val INDIVIDUAL_LOGISTICIAN_USER_ID_COL: String = "id"
        const val INDIVIDUAL_LOGISTICIAN_USER_OTHER_NAME_COL: String = "other_name"
        const val INDIVIDUAL_LOGISTICIAN_USER_LAST_NAME_COL: String = "last_name"
        const val LOGISTICIAN_CODE_COL: String = "logistician_code"
        const val INDIVIDUAL_LOGISTICIAN_USER_ID_NUMBER_COL: String = "id_number"
        const val INDIVIDUAL_LOGISTICIAN_USER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val INDIVIDUAL_LOGISTICIAN_USER_EMAIL_COL: String = "email"
        const val INDIVIDUAL_LOGISTICIAN_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val INDIVIDUAL_LOGISTICIAN_USER_ADDRESS_COL: String = "address"
        const val INDIVIDUAL_LOGISTICIAN_USER_HUB_COL: String = "hub"
        const val INDIVIDUAL_LOGISTICIAN_USER_REGION_COL: String = "region"
        const val INDIVIDUAL_LOGISTICIAN_USER_USER_ID_COL: String = "user_id"

        // OrganisationLogisticianUser Table Columns
        const val ORGANISATION_LOGISTICIAN_USER_ID_COL: String = "id"
        const val ORGANISATION_LOGISTICIAN_USER_NAME_COL: String = "name"
        const val ORGANISATION_LOGISTICIAN_USER_LOGISTICIAN_CODE_COL: String = "logistician_code"
        const val ORGANISATION_LOGISTICIAN_USER_REGISTRATION_NUMBER_COL: String = "registration_number"
        const val ORGANISATION_LOGISTICIAN_USER_DATE_OF_REGISTRATION_COL: String = "date_of_registration"
        const val ORGANISATION_LOGISTICIAN_USER_EMAIL_COL: String = "email"
        const val ORGANISATION_LOGISTICIAN_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val ORGANISATION_LOGISTICIAN_USER_ADDRESS_COL: String = "address"
        const val ORGANISATION_LOGISTICIAN_USER_HUB_COL: String = "hub"
        const val ORGANISATION_LOGISTICIAN_USER_REGION_COL: String = "region"
        const val ORGANISATION_LOGISTICIAN_USER_USER_ID_COL: String = "user_id"

        // Car Table Columns
        const val CAR_ID_COL: String = "id"
        const val CAR_BODY_TYPE_COL: String = "car_body_type"
        const val CAR_MODEL_COL: String = "car_model"
        const val CAR_NUMBER_PLATE_COL: String = "number_plate"
        const val CAR_DRIVER1_NAME_COL: String = "driver1_name"
        const val CAR_DRIVER2_NAME_COL: String = "driver2_name"
        const val INDIVIDUAL_LOGISTICIAN_ID_FK_COL: String = "individual_logistician_id"
        const val ORGANISATION_LOGISTICIAN_ID_FK_COL: String = "organisation_logistician_id"

        // IndividualCustomerUser Table Columns
        const val INDIVIDUAL_CUSTOMER_USER_ID_COL: String = "id"
        const val INDIVIDUAL_CUSTOMER_USER_OTHER_NAME_COL: String = "other_name"
        const val INDIVIDUAL_CUSTOMER_USER_LAST_NAME_COL: String = "last_name"
        const val INDIVIDUAL_CUSTOMER_USER_CUSTOMER_CODE_COL: String = "customer_code"
        const val INDIVIDUAL_CUSTOMER_USER_ID_NUMBER_COL: String = "id_number"
        const val INDIVIDUAL_CUSTOMER_USER_GENDER_COL: String = "gender"
        const val INDIVIDUAL_CUSTOMER_USER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val INDIVIDUAL_CUSTOMER_USER_EMAIL_COL: String = "email"
        const val INDIVIDUAL_CUSTOMER_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val INDIVIDUAL_CUSTOMER_USER_COUNTY_COL: String = "county"
        const val INDIVIDUAL_CUSTOMER_USER_SUB_COUNTY_COL: String = "sub_county"
        const val INDIVIDUAL_CUSTOMER_USER_WARD_COL: String = "ward"
        const val INDIVIDUAL_CUSTOMER_USER_VILLAGE_COL: String = "village"
        const val INDIVIDUAL_CUSTOMER_USER_USER_AUTHORISED_COL: String = "user_authorised"
        const val INDIVIDUAL_CUSTOMER_USER_AUTHORISATION_TOKEN_COL: String = "authorisation_token"
        const val INDIVIDUAL_CUSTOMER_USER_USER_ID_COL: String = "user_id"

        // OrganisationCustomerUser Table Columns
        const val ORGANISATION_CUSTOMER_USER_ID_COL: String = "id"
        const val ORGANISATION_CUSTOMER_USER_COMPANY_NAME_COL: String = "company_name"
        const val ORGANISATION_CUSTOMER_USER_CUSTOMER_CODE_COL: String = "customer_code"
        const val ORGANISATION_CUSTOMER_USER_REGISTRATION_NUMBER_COL: String = "registration_number"
        const val ORGANISATION_CUSTOMER_USER_SECTOR_COL: String = "sector"
        const val ORGANISATION_CUSTOMER_USER_DATE_OF_REGISTRATION_COL: String = "date_of_registration"
        const val ORGANISATION_CUSTOMER_USER_EMAIL_COL: String = "email"
        const val ORGANISATION_CUSTOMER_USER_PHONE_NUMBER_COL: String = "phone_number"
        const val ORGANISATION_CUSTOMER_USER_COUNTY_COL: String = "county"
        const val ORGANISATION_CUSTOMER_USER_SUB_COUNTY_COL: String = "sub_county"
        const val ORGANISATION_CUSTOMER_USER_WARD_COL: String = "ward"
        const val ORGANISATION_CUSTOMER_USER_VILLAGE_COL: String = "village"
        const val ORGANISATION_CUSTOMER_USER_OTHER_NAME1_COL: String = "other_name1"
        const val ORGANISATION_CUSTOMER_USER_LAST_NAME1_COL: String = "last_name1"
        const val ORGANISATION_CUSTOMER_USER_ID_NUMBER1_COL: String = "id_number1"
        const val ORGANISATION_CUSTOMER_USER_GENDER1_COL: String = "gender1"
        const val ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH1_COL: String = "date_of_birth1"
        const val ORGANISATION_CUSTOMER_USER_EMAIL1_COL: String = "email1"
        const val ORGANISATION_CUSTOMER_USER_PHONE_NUMBER1_COL: String = "phone_number1"
        const val ORGANISATION_CUSTOMER_USER_OTHER_NAME2_COL: String = "other_name2"
        const val ORGANISATION_CUSTOMER_USER_LAST_NAME2_COL: String = "last_name2"
        const val ORGANISATION_CUSTOMER_USER_ID_NUMBER2_COL: String = "id_number2"
        const val ORGANISATION_CUSTOMER_USER_GENDER2_COL: String = "gender2"
        const val ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH2_COL: String = "date_of_birth2"
        const val ORGANISATION_CUSTOMER_USER_EMAIL2_COL: String = "email2"
        const val ORGANISATION_CUSTOMER_USER_PHONE_NUMBER2_COL: String = "phone_number2"
        const val ORGANISATION_CUSTOMER_USER_OTHER_NAME3_COL: String = "other_name3"
        const val ORGANISATION_CUSTOMER_USER_LAST_NAME3_COL: String = "last_name3"
        const val ORGANISATION_CUSTOMER_USER_ID_NUMBER3_COL: String = "id_number3"
        const val ORGANISATION_CUSTOMER_USER_GENDER3_COL: String = "gender3"
        const val ORGANISATION_CUSTOMER_USER_DATE_OF_BIRTH3_COL: String = "date_of_birth3"
        const val ORGANISATION_CUSTOMER_USER_EMAIL3_COL: String = "email3"
        const val ORGANISATION_CUSTOMER_USER_PHONE_NUMBER3_COL: String = "phone_number3"
        const val ORGANISATION_CUSTOMER_USER_USER_AUTHORISED_COL: String = "user_authorised"
        const val ORGANISATION_CUSTOMER_USER_AUTHORISATION_TOKEN_COL: String = "authorisation_token"
        const val ORGANISATION_CUSTOMER_USER_USER_ID_COL: String = "user_id"

        // Product Table Columns
        const val PRODUCT_ID_COL: String = "id"
        const val PRODUCT_CATEGORY_COL: String = "category"
        const val PRODUCT_PRODUCTS_INTERESTED_IN_COL: String = "products_interested_in"
        const val PRODUCT_VOLUME_IN_KGS_COL: String = "volume_in_kgs"
        const val PRODUCT_PACKAGING_COL: String = "packaging"
        const val PRODUCT_QUALITY_COL: String = "quality"
        const val PRODUCT_FREQUENCY_COL: String = "frequency"
        const val INDIVIDUAL_CUSTOMER_ID_FK_COL: String = "individual_customer_id"
        const val ORGANISATION_CUSTOMER_ID_FK_COL: String = "organisation_customer_id"

        // Producer Biodata Table Columns
        const val PRODUCER_BIODATA_ID_COL: String = "id"
        const val PRODUCER_OTHER_NAME_COL: String = "other_name"
        const val PRODUCER_LAST_NAME_COL: String = "last_name"
        const val PRODUCER_FARMER_CODE_COL: String = "farmer_code"
        const val PRODUCER_ID_NUMBER_COL: String = "id_number"
        const val PRODUCER_GENDER_COL: String = "gender"
        const val PRODUCER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val PRODUCER_EMAIL_COL: String = "email"
        const val PRODUCER_PHONE_NUMBER_COL: String = "phone_number"
        const val PRODUCER_HUB_COL: String = "hub"
        const val PRODUCER_BUYING_CENTER_COL: String = "buying_center"
        const val PRODUCER_EDUCATION_LEVEL_COL: String = "education_level"
        const val PRODUCER_COUNTY_COL: String = "county"
        const val PRODUCER_SUB_COUNTY_COL: String = "sub_county"
        const val PRODUCER_WARD_COL: String = "ward"
        const val PRODUCER_VILLAGE_COL: String = "village"
        const val PRODUCER_PRIMARY_PRODUCER_COL: String = "primary_producer"
        const val PRODUCER_TOTAL_LAND_SIZE_COL: String = "total_land_size"
        const val PRODUCER_CULTIVATE_LAND_SIZE_COL: String = "cultivate_land_size"
        const val PRODUCER_HOMESTEAD_SIZE_COL: String = "homestead_size"
        const val PRODUCER_UNCULTIVATED_LAND_SIZE_COL: String = "uncultivated_land_size"
        const val PRODUCER_FARM_ACCESSIBILITY_COL: String = "farm_accessibility"
        const val PRODUCER_NUMBER_OF_FAMILY_WORKERS_COL: String = "number_of_family_workers"
        const val PRODUCER_NUMBER_OF_HIRED_WORKERS_COL: String = "number_of_hired_workers"
        const val PRODUCER_ACCESS_TO_IRRIGATION_COL: String = "access_to_irrigation"
        const val PRODUCER_CROP_LIST_COL: String = "crop_list"
        const val PRODUCER_FARMER_INTEREST_IN_EXTENSION_COL: String = "farmer_interest_in_extension"
        const val PRODUCER_KNOWLEDGE_RELATED_COL: String = "knowledge_related"
        const val PRODUCER_SOIL_RELATED_COL: String = "soil_related"
        const val PRODUCER_COMPOST_RELATED_COL: String = "compost_related"
        const val PRODUCER_NUTRITION_RELATED_COL: String = "nutrition_related"
        const val PRODUCER_PESTS_RELATED_COL: String = "pests_related"
        const val PRODUCER_DISEASE_RELATED_COL: String = "disease_related"
        const val PRODUCER_QUALITY_RELATED_COL: String = "quality_related"
        const val PRODUCER_MARKET_RELATED_COL: String = "market_related"
        const val PRODUCER_FOOD_LOSS_RELATED_COL: String = "food_loss_related"
        const val PRODUCER_FINANCE_RELATED_COL: String = "finance_related"
        const val PRODUCER_WEATHER_RELATED_COL: String = "weather_related"
        const val PRODUCER_DAIRY_CATTLE_COL: String = "dairy_cattle"
        const val PRODUCER_BEEF_CATTLE_COL: String = "beef_cattle"
        const val PRODUCER_SHEEP_COL: String = "sheep"
        const val PRODUCER_POULTRY_COL: String = "poultry"
        const val PRODUCER_PIGS_COL: String = "pigs"
        const val PRODUCER_RABBITS_COL: String = "rabbits"
        const val PRODUCER_BEEHIVES_COL: String = "beehives"
        const val PRODUCER_DONKEYS_COL: String = "donkeys"
        const val PRODUCER_GOATS_COL: String = "goats"
        const val PRODUCER_CAMELS_COL: String = "camels"
        const val PRODUCER_AQUACULTURE_COL: String = "aquaculture"
        const val PRODUCER_HOUSING_TYPE_COL: String = "housing_type"
        const val PRODUCER_HOUSING_FLOOR_COL: String = "housing_floor"
        const val PRODUCER_HOUSING_ROOF_COL: String = "housing_roof"
        const val PRODUCER_LIGHTING_FUEL_COL: String = "lighting_fuel"
        const val PRODUCER_COOKING_FUEL_COL: String = "cooking_fuel"
        const val PRODUCER_WATER_FILTER_COL: String = "water_filter"
        const val PRODUCER_WATER_TANK_GREATER_THAN_5000LTS_COL: String = "water_tank_greater_than_5000lts"
        const val PRODUCER_HAND_WASHING_FACILITIES_COL: String = "hand_washing_facilities"
        const val PRODUCER_PPES_COL: String = "ppes"
        const val PRODUCER_WATER_WELL_OR_WEIR_COL: String = "water_well_or_weir"
        const val PRODUCER_IRRIGATION_PUMP_COL: String = "irrigation_pump"
        const val PRODUCER_HARVESTING_EQUIPMENT_COL: String = "harvesting_equipment"
        const val PRODUCER_TRANSPORTATION_TYPE_COL: String = "transportation_type"
        const val PRODUCER_TOILET_FLOOR_COL: String = "toilet_floor"
        const val PRODUCER_USER_APPROVED_COL: String = "user_approved"
        const val PRODUCER_TA_COL: String = "ta"
        const val PRODUCER_USER_ID_COL: String = "user_id"

        // Commercial Produce Table
        const val COMMERCIAL_PRODUCE_ID_COL: String = "id"
        const val COMMERCIAL_PRODUCE_PRODUCT_COL: String = "product"
        const val COMMERCIAL_PRODUCE_PRODUCT_CATEGORY_COL: String = "product_category"
        const val COMMERCIAL_PRODUCE_ACERAGE_COL: String = "acerage"
        const val COMMERCIAL_PRODUCER_BIODATA_ID_FK_COL: String = "producer_biodata_id" // Foreign key
        const val COMMERCIAL_CIG_PRODUCER_BIODATA_ID_FK_COL: String = "cig_producer_biodata_id" // Foreign key

        // Domestic Produce Table
        const val DOMESTIC_PRODUCE_ID_COL: String = "id"
        const val DOMESTIC_PRODUCE_PRODUCT_COL: String = "product"
        const val DOMESTIC_PRODUCE_PRODUCT_CATEGORY_COL: String = "product_category"
        const val DOMESTIC_PRODUCE_ACERAGE_COL: String = "acerage"
        const val DOMESTIC_PRODUCER_BIODATA_ID_FK_COL: String = "producer_biodata_id" // Foreign key
        const val DOMESTIC_CIG_PRODUCER_BIODATA_ID_FK_COL: String = "cig_producer_biodata_id" // Foreign key

        // Farmer Field Registration Table
        const val FARMER_FIELD_REGISTRATION_ID_COL: String = "id"
        const val FARMER_FIELD_PRODUCER_COL: String = "producer"
        const val FARMER_FIELD_FIELD_NUMBER_COL: String = "field_number"
        const val FARMER_FIELD_FIELD_SIZE_COL: String = "field_size"
        const val FARMER_FIELD_CROP1_COL: String = "crop1"
        const val FARMER_FIELD_CROP_VARIETY1_COL: String = "crop_variety1"
        const val FARMER_FIELD_DATE_PLANTED1_COL: String = "date_planted1"
        const val FARMER_FIELD_DATE_OF_HARVEST1_COL: String = "date_of_harvest1"
        const val FARMER_FIELD_POPULATION1_COL: String = "population1"
        const val FARMER_FIELD_BASELINE_YIELD_LAST_SEASON1_COL: String = "baseline_yield_last_season1"
        const val FARMER_FIELD_BASELINE_INCOME_LAST_SEASON1_COL: String = "baseline_income_last_season1"
        const val FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON1_COL: String = "baseline_cost_of_production_last_season1"
        const val FARMER_FIELD_CROP2_COL: String = "crop2"
        const val FARMER_FIELD_CROP_VARIETY2_COL: String = "crop_variety2"
        const val FARMER_FIELD_DATE_PLANTED2_COL: String = "date_planted2"
        const val FARMER_FIELD_DATE_OF_HARVEST2_COL: String = "date_of_harvest2"
        const val FARMER_FIELD_POPULATION2_COL: String = "population2"
        const val FARMER_FIELD_BASELINE_YIELD_LAST_SEASON2_COL: String = "baseline_yield_last_season2"
        const val FARMER_FIELD_BASELINE_INCOME_LAST_SEASON2_COL: String = "baseline_income_last_season2"
        const val FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON2_COL: String = "baseline_cost_of_production_last_season2"
        const val FARMER_FIELD_PRODUCER_BIODATA_ID_FK_COL: String = "producer_biodata_id" // Foreign key
        const val FARMER_FIELD_USER_ID_FK_COL: String = "user_id"

        // CIG Producer Biodata Table Columns
        const val CIG_PRODUCER_BIODATA_ID_COL: String = "id"
        const val CIG_PRODUCER_OTHER_NAME_COL: String = "other_name"
        const val CIG_PRODUCER_LAST_NAME_COL: String = "last_name"
        const val CIG_PRODUCER_FARMER_CODE_COL: String = "farmer_code"
        const val CIG_PRODUCER_ID_NUMBER_COL: String = "id_number"
        const val CIG_PRODUCER_GENDER_COL: String = "gender"
        const val CIG_PRODUCER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val CIG_PRODUCER_EMAIL_COL: String = "email"
        const val CIG_PRODUCER_PHONE_NUMBER_COL: String = "phone_number"
        const val CIG_PRODUCER_HUB_COL: String = "hub"
        const val CIG_PRODUCER_BUYING_CENTER_COL: String = "buying_center"
        const val CIG_PRODUCER_EDUCATION_LEVEL_COL: String = "education_level"
        const val CIG_PRODUCER_COUNTY_COL: String = "county"
        const val CIG_PRODUCER_SUB_COUNTY_COL: String = "sub_county"
        const val CIG_PRODUCER_WARD_COL: String = "ward"
        const val CIG_PRODUCER_VILLAGE_COL: String = "village"
        const val CIG_PRODUCER_PRIMARY_PRODUCER_COL: String = "primary_producer"
        const val CIG_PRODUCER_TOTAL_LAND_SIZE_COL: String = "total_land_size"
        const val CIG_PRODUCER_CULTIVATE_LAND_SIZE_COL: String = "cultivate_land_size"
        const val CIG_PRODUCER_HOMESTEAD_SIZE_COL: String = "homestead_size"
        const val CIG_PRODUCER_UNCULTIVATED_LAND_SIZE_COL: String = "uncultivated_land_size"
        const val CIG_PRODUCER_FARM_ACCESSIBILITY_COL: String = "farm_accessibility"
        const val CIG_PRODUCER_NUMBER_OF_FAMILY_WORKERS_COL: String = "number_of_family_workers"
        const val CIG_PRODUCER_NUMBER_OF_HIRED_WORKERS_COL: String = "number_of_hired_workers"
        const val CIG_PRODUCER_ACCESS_TO_IRRIGATION_COL: String = "access_to_irrigation"
        const val CIG_PRODUCER_CROP_LIST_COL: String = "crop_list"
        const val CIG_PRODUCER_FARMER_INTEREST_IN_EXTENSION_COL: String = "farmer_interest_in_extension"
        const val CIG_PRODUCER_KNOWLEDGE_RELATED_COL: String = "knowledge_related"
        const val CIG_PRODUCER_SOIL_RELATED_COL: String = "soil_related"
        const val CIG_PRODUCER_COMPOST_RELATED_COL: String = "compost_related"
        const val CIG_PRODUCER_NUTRITION_RELATED_COL: String = "nutrition_related"
        const val CIG_PRODUCER_PESTS_RELATED_COL: String = "pests_related"
        const val CIG_PRODUCER_DISEASE_RELATED_COL: String = "disease_related"
        const val CIG_PRODUCER_QUALITY_RELATED_COL: String = "quality_related"
        const val CIG_PRODUCER_MARKET_RELATED_COL: String = "market_related"
        const val CIG_PRODUCER_FOOD_LOSS_RELATED_COL: String = "food_loss_related"
        const val CIG_PRODUCER_FINANCE_RELATED_COL: String = "finance_related"
        const val CIG_PRODUCER_WEATHER_RELATED_COL: String = "weather_related"
        const val CIG_PRODUCER_DAIRY_CATTLE_COL: String = "dairy_cattle"
        const val CIG_PRODUCER_BEEF_CATTLE_COL: String = "beef_cattle"
        const val CIG_PRODUCER_SHEEP_COL: String = "sheep"
        const val CIG_PRODUCER_POULTRY_COL: String = "poultry"
        const val CIG_PRODUCER_PIGS_COL: String = "pigs"
        const val CIG_PRODUCER_RABBITS_COL: String = "rabbits"
        const val CIG_PRODUCER_BEEHIVES_COL: String = "beehives"
        const val CIG_PRODUCER_DONKEYS_COL: String = "donkeys"
        const val CIG_PRODUCER_GOATS_COL: String = "goats"
        const val CIG_PRODUCER_CAMELS_COL: String = "camels"
        const val CIG_PRODUCER_AQUACULTURE_COL: String = "aquaculture"
        const val CIG_PRODUCER_HOUSING_TYPE_COL: String = "housing_type"
        const val CIG_PRODUCER_HOUSING_FLOOR_COL: String = "housing_floor"
        const val CIG_PRODUCER_HOUSING_ROOF_COL: String = "housing_roof"
        const val CIG_PRODUCER_LIGHTING_FUEL_COL: String = "lighting_fuel"
        const val CIG_PRODUCER_COOKING_FUEL_COL: String = "cooking_fuel"
        const val CIG_PRODUCER_WATER_FILTER_COL: String = "water_filter"
        const val CIG_PRODUCER_WATER_TANK_GREATER_THAN_5000LTS_COL: String = "water_tank_greater_than_5000lts"
        const val CIG_PRODUCER_HAND_WASHING_FACILITIES_COL: String = "hand_washing_facilities"
        const val CIG_PRODUCER_PPES_COL: String = "ppes"
        const val CIG_PRODUCER_WATER_WELL_OR_WEIR_COL: String = "water_well_or_weir"
        const val CIG_PRODUCER_IRRIGATION_PUMP_COL: String = "irrigation_pump"
        const val CIG_PRODUCER_HARVESTING_EQUIPMENT_COL: String = "harvesting_equipment"
        const val CIG_PRODUCER_TRANSPORTATION_TYPE_COL: String = "transportation_type"
        const val CIG_PRODUCER_TOILET_FLOOR_COL: String = "toilet_floor"
        const val CIG_PRODUCER_USER_APPROVED_COL: String = "user_approved"
        const val CIG_PRODUCER_TA_COL: String = "ta"
        const val CIG_PRODUCER_USER_ID_COL: String = "user_id"

        // CIG Farmer Field Registration Table
        const val CIG_FARMER_FIELD_REGISTRATION_ID_COL: String = "id"
        const val CIG_FARMER_FIELD_PRODUCER_COL: String = "producer"
        const val CIG_FARMER_FIELD_FIELD_NUMBER_COL: String = "field_number"
        const val CIG_FARMER_FIELD_FIELD_SIZE_COL: String = "field_size"
        const val CIG_FARMER_FIELD_CROP1_COL: String = "crop1"
        const val CIG_FARMER_FIELD_CROP_VARIETY1_COL: String = "crop_variety1"
        const val CIG_FARMER_FIELD_DATE_PLANTED1_COL: String = "date_planted1"
        const val CIG_FARMER_FIELD_DATE_OF_HARVEST1_COL: String = "date_of_harvest1"
        const val CIG_FARMER_FIELD_POPULATION1_COL: String = "population1"
        const val CIG_FARMER_FIELD_BASELINE_YIELD_LAST_SEASON1_COL: String = "baseline_yield_last_season1"
        const val CIG_FARMER_FIELD_BASELINE_INCOME_LAST_SEASON1_COL: String = "baseline_income_last_season1"
        const val CIG_FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON1_COL: String = "baseline_cost_of_production_last_season1"
        const val CIG_FARMER_FIELD_CROP2_COL: String = "crop2"
        const val CIG_FARMER_FIELD_CROP_VARIETY2_COL: String = "crop_variety2"
        const val CIG_FARMER_FIELD_DATE_PLANTED2_COL: String = "date_planted2"
        const val CIG_FARMER_FIELD_DATE_OF_HARVEST2_COL: String = "date_of_harvest2"
        const val CIG_FARMER_FIELD_POPULATION2_COL: String = "population2"
        const val CIG_FARMER_FIELD_BASELINE_YIELD_LAST_SEASON2_COL: String = "baseline_yield_last_season2"
        const val CIG_FARMER_FIELD_BASELINE_INCOME_LAST_SEASON2_COL: String = "baseline_income_last_season2"
        const val CIG_FARMER_FIELD_BASELINE_COST_OF_PRODUCTION_LAST_SEASON2_COL: String = "baseline_cost_of_production_last_season2"
        const val CIG_FARMER_FIELD_PRODUCER_BIODATA_ID_FK_COL: String = "producer_biodata_id" // Foreign key
        const val CIG_FARMER_FIELD_USER_ID_FK_COL: String = "user_id"

        // Season Planning Table
        const val SEASON_PLANNING_ID_COL: String = "id"
        const val SEASON_PLANNING_PRODUCER_COL: String = "producer"
        const val SEASON_PLANNING_FIELD_COL: String = "field"
        const val SEASON_PLANNING_PLANNED_DATE_OF_PLANTING_COL: String = "planned_date_of_planting"
        const val SEASON_PLANNING_WEEK_NUMBER_COL: String = "week_number"
        const val SEASON_PLANNING_NURSERY_COL: String = "nursery"
        const val SEASON_PLANNING_GAPPING_COL: String = "gapping"
        const val SEASON_PLANNING_SOIL_ANALYSIS_COL: String = "soil_analysis"
        const val SEASON_PLANNING_LIMING_COL: String = "liming"
        const val SEASON_PLANNING_TRANSPLANTING_COL: String = "transplanting"
        const val SEASON_PLANNING_WEEDING_COL: String = "weeding"
        const val SEASON_PLANNING_PRUNNING_THINNING_DESUCKERING_COL: String = "prunning_thinning_desuckering"
        const val SEASON_PLANNING_MULCHING_COL: String = "mulching"
        const val SEASON_PLANNING_HARVESTING_COL: String = "harvesting"
        const val SEASON_PLANNING_USER_ID_FK_COL: String = "user_id" // Foreign key

        // Market Produce Table
        const val MARKET_PRODUCE_ID_COL: String = "id"
        const val MARKET_PRODUCE_PRODUCT_COL: String = "product"
        const val MARKET_PRODUCE_PRODUCT_CATEGORY_COL: String = "product_category"
        const val MARKET_PRODUCE_ACERAGE_COL: String = "acerage"
        const val MARKET_PRODUCE_SEASON_PLANNING_ID_FK_COL: String = "season_planning_id" // Foreign key
        const val MARKET_PRODUCE_EXTENSION_SERVICE_ID_FK_COL: String = "extension_service_id" // Foreign key

        // Plan Nutrition Table
        const val PLAN_NUTRITION_ID_COL: String = "id"
        const val PLAN_NUTRITION_PRODUCT_COL: String = "product"
        const val PLAN_NUTRITION_PRODUCT_NAME_COL: String = "product_name"
        const val PLAN_NUTRITION_UNIT_COL: String = "unit"
        const val PLAN_NUTRITION_COST_PER_UNIT_COL: String = "cost_per_unit"
        const val PLAN_NUTRITION_APPLICATION_RATE_COL: String = "application_rate"
        const val PLAN_NUTRITION_TIME_OF_APPLICATION_COL: String = "time_of_application"
        const val PLAN_NUTRITION_METHOD_OF_APPLICATION_COL: String = "method_of_application"
        const val PLAN_NUTRITION_PRODUCT_FORMULATION_COL: String = "product_formulation"
        const val PLAN_NUTRITION_DATE_OF_APPLICATION_COL: String = "date_of_application"
        const val PLAN_NUTRITION_TOTAL_MIXING_RATIO_COL: String = "total_mixing_ratio"
        const val PLAN_NUTRITION_SEASON_PLANNING_ID_FK_COL: String = "season_planning_id" // Foreign key

        // Scouting Station Table
        const val SCOUTING_STATION_ID_COL: String = "id"
        const val SCOUTING_STATION_BAIT_STATION_COL: String = "bait_station"
        const val SCOUTING_STATION_TYPE_OF_BAIT_PROVIDED_COL: String = "type_of_bait_provided"
        const val SCOUTING_STATION_FREQUENCY_COL: String = "frequency"
        const val SCOUTING_STATION_SEASON_PLANNING_ID_FK_COL: String = "season_planning_id" // Foreign key

        // Preventative Disease Table
        const val PREVENTATIVE_DISEASE_ID_COL: String = "id"
        const val PREVENTATIVE_DISEASE_DISEASE_COL: String = "disease"
        const val PREVENTATIVE_DISEASE_PRODUCT_COL: String = "product"
        const val PREVENTATIVE_DISEASE_CATEGORY_COL: String = "category"
        const val PREVENTATIVE_DISEASE_FORMULATION_COL: String = "formulation"
        const val PREVENTATIVE_DISEASE_DOSAGE_COL: String = "dosage"
        const val PREVENTATIVE_DISEASE_UNIT_COL: String = "unit"
        const val PREVENTATIVE_DISEASE_COST_PER_UNIT_COL: String = "cost_per_unit"
        const val PREVENTATIVE_DISEASE_VOLUME_OF_WATER_COL: String = "volume_of_water"
        const val PREVENTATIVE_DISEASE_FREQUENCY_OF_APPLICATION_COL: String = "frequency_of_application"
        const val PREVENTATIVE_DISEASE_TOTAL_COST_COL: String = "total_cost"
        const val PREVENTATIVE_DISEASE_SEASON_PLANNING_ID_FK_COL: String = "season_planning_id" // Foreign key

        // Preventative Pest Table
        const val PREVENTATIVE_PEST_ID_COL: String = "id"
        const val PREVENTATIVE_PEST_PEST_COL: String = "pest"
        const val PREVENTATIVE_PEST_PRODUCT_COL: String = "product"
        const val PREVENTATIVE_PEST_CATEGORY_COL: String = "category"
        const val PREVENTATIVE_PEST_FORMULATION_COL: String = "formulation"
        const val PREVENTATIVE_PEST_DOSAGE_COL: String = "dosage"
        const val PREVENTATIVE_PEST_UNIT_COL: String = "unit"
        const val PREVENTATIVE_PEST_COST_PER_UNIT_COL: String = "cost_per_unit"
        const val PREVENTATIVE_PEST_VOLUME_OF_WATER_COL: String = "volume_of_water"
        const val PREVENTATIVE_PEST_FREQUENCY_OF_APPLICATION_COL: String = "frequency_of_application"
        const val PREVENTATIVE_PEST_TOTAL_COST_COL: String = "total_cost"
        const val PREVENTATIVE_PEST_SEASON_PLANNING_ID_FK_COL: String = "season_planning_id" // Foreign key

        // Plan Irrigation Table
        const val PLAN_IRRIGATION_ID_COL: String = "id"
        const val PLAN_IRRIGATION_TYPE_OF_IRRIGATION_COL: String = "type_of_irrigation"
        const val PLAN_IRRIGATION_DISCHARGE_HOURS_COL: String = "discharge_hours"
        const val PLAN_IRRIGATION_FREQUENCY_COL: String = "frequency"
        const val PLAN_IRRIGATION_COST_OF_FUEL_COL: String = "cost_of_fuel"
        const val PLAN_IRRIGATION_UNIT_COST_COL: String = "unit_cost"
        const val PLAN_IRRIGATION_SEASON_PLANNING_ID_FK_COL: String = "season_planning_id" // Foreign key

        // Extension Service Table
        const val EXTENSION_SERVICE_ID_COL: String = "id"
        const val EXTENSION_SERVICE_PRODUCER_COL: String = "producer"
        const val EXTENSION_SERVICE_FIELD_COL: String = "field"
        const val EXTENSION_SERVICE_PLANNED_DATE_OF_PLANTING_COL: String = "planned_date_of_planting"
        const val EXTENSION_SERVICE_WEEK_NUMBER_COL: String = "week_number"
        const val EXTENSION_SERVICE_NURSERY_COL: String = "nursery"
        const val EXTENSION_SERVICE_GAPPING_COL: String = "gapping"
        const val EXTENSION_SERVICE_SOIL_ANALYSIS_COL: String = "soil_analysis"
        const val EXTENSION_SERVICE_LIMING_COL: String = "liming"
        const val EXTENSION_SERVICE_TRANSPLANTING_COL: String = "transplanting"
        const val EXTENSION_SERVICE_WEEDING_COL: String = "weeding"
        const val EXTENSION_SERVICE_PRUNNING_THINNING_DESUCKERING_COL: String = "prunning_thinning_desuckering"
        const val EXTENSION_SERVICE_MULCHING_COL: String = "mulching"
        const val EXTENSION_SERVICE_HARVESTING_COL: String = "harvesting"
        const val EXTENSION_SERVICE_USER_ID_FK_COL: String = "user_id" // Foreign key

        // Ext Scouting Station Table
        const val EXT_SCOUTING_STATION_ID_COL: String = "id"
        const val EXT_SCOUTING_STATION_SCOUTING_METHOD_COL: String = "scouting_method"
        const val EXT_SCOUTING_STATION_BAIT_STATION_COL: String = "bait_station"
        const val EXT_SCOUTING_STATION_PEST_OR_DISEASE_COL: String = "pest_or_disease"
        const val EXT_SCOUTING_STATION_MANAGEMENT_COL: String = "management"
        const val EXT_SCOUTING_STATION_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL: String = "extension_service_registration_id"

        // Pesticide Used Table
        const val PESTICIDE_USED_ID_COL: String = "id"
        const val PESTICIDE_USED_REGISTER_COL: String = "register"
        const val PESTICIDE_USED_PRODUCT_COL: String = "product"
        const val PESTICIDE_USED_CATEGORY_COL: String = "category"
        const val PESTICIDE_USED_FORMULATION_COL: String = "formulation"
        const val PESTICIDE_USED_DOSAGE_COL: String = "dosage"
        const val PESTICIDE_USED_UNIT_COL: String = "unit"
        const val PESTICIDE_USED_COST_PER_UNIT_COL: String = "cost_per_unit"
        const val PESTICIDE_USED_VOLUME_OF_WATER_COL: String = "volume_of_water"
        const val PESTICIDE_USED_FREQUENCY_OF_APPLICATION_COL: String = "frequency_of_application"
        const val PESTICIDE_USED_TOTAL_COST_COL: String = "total_cost"
        const val PESTICIDE_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL: String = "extension_service_registration_id" // Foreign key

        // Fertilizer Used Table
        const val FERTILIZER_USED_ID_COL: String = "id"
        const val FERTILIZER_USED_REGISTER_COL: String = "register"
        const val FERTILIZER_USED_PRODUCT_COL: String = "product"
        const val FERTILIZER_USED_CATEGORY_COL: String = "category"
        const val FERTILIZER_USED_FORMULATION_COL: String = "formulation"
        const val FERTILIZER_USED_DOSAGE_COL: String = "dosage"
        const val FERTILIZER_USED_UNIT_COL: String = "unit"
        const val FERTILIZER_USED_COST_PER_UNIT_COL: String = "cost_per_unit"
        const val FERTILIZER_USED_VOLUME_OF_WATER_COL: String = "volume_of_water"
        const val FERTILIZER_USED_FREQUENCY_OF_APPLICATION_COL: String = "frequency_of_application"
        const val FERTILIZER_USED_TOTAL_COST_COL: String = "total_cost"
        const val FERTILIZER_USED_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL: String = "extension_service_registration_id" // Foreign key

        // Forecast Yield Table
        const val FORECAST_YIELD_ID_COL: String = "id"
        const val FORECAST_YIELD_CROP_POPULATION_PC_COL: String = "crop_population_pc"
        const val FORECAST_YIELD_YIELD_FORECAST_PC_COL: String = "yield_forecast_pc"
        const val FORECAST_YIELD_FORECAST_QUALITY_COL: String = "forecast_quality"
        const val FORECAST_YIELD_TA_COMMENTS_COL: String = "ta_comments"
        const val FORECAST_YIELD_EXTENSION_SERVICE_REGISTRATION_ID_FK_COL: String = "extension_service_registration_id" // Foreign key

        // Training Table
        const val TRAINING_ID_COL: String = "id"
        const val TRAINING_COURSE_NAME_COL: String = "course_name"
        const val TRAINING_TRAINER_NAME_COL: String = "trainer_name"
        const val TRAINING_BUYING_CENTER_COL: String = "buying_center"
        const val TRAINING_COURSE_DESCRIPTION_COL: String = "course_description"
        const val TRAINING_DATE_OF_TRAINING_COL: String = "date_of_training"
        const val TRAINING_CONTENT_OF_TRAINING_COL: String = "content_of_training"
        const val TRAINING_VENUE_COL: String = "venue"
        const val TRAINING_PARTICIPANTS_COL: String = "participants"
        const val TRAINING_USER_ID_FK_COL: String = "user_id" // Foreign key

        // Attendance Table
        const val ATTENDANCE_ID_COL: String = "id"
        const val ATTENDANCE_ATTENDANCE_COL: String = "attendance"
        const val ATTENDANCE_TRAINING_ID_COL: String = "training_id"
        const val ATTENDANCE_USER_ID_FK_COL: String = "user_id" // Foreign key

        // FarmerPriceDistribution Table
        const val FARMER_PRICE_DISTRIBUTION_ID_COL: String = "id"
        const val FARMER_PRICE_DISTRIBUTION_HUB_COL: String = "hub"
        const val FARMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL: String = "buying_center"
        const val FARMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL: String = "online_price"
        const val FARMER_PRICE_DISTRIBUTION_UNIT_COL: String = "unit"
        const val FARMER_PRICE_DISTRIBUTION_DATE_COL: String = "date"
        const val FARMER_PRICE_DISTRIBUTION_COMMENTS_COL: String = "comments"
        const val FARMER_PRICE_DISTRIBUTION_SOLD_COL: String = "sold"
        const val FARMER_PRICE_DISTRIBUTION_USER_ID_FK_COL: String = "user_id" // Foreign key
        const val FARMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL: String = "produce_id" // Foreign key

        // CustomerPriceDistribution Table
        const val CUSTOMER_PRICE_DISTRIBUTION_ID_COL: String = "id"
        const val CUSTOMER_PRICE_DISTRIBUTION_HUB_COL: String = "hub"
        const val CUSTOMER_PRICE_DISTRIBUTION_BUYING_CENTER_COL: String = "buying_center"
        const val CUSTOMER_PRICE_DISTRIBUTION_ONLINE_PRICE_COL: String = "online_price"
        const val CUSTOMER_PRICE_DISTRIBUTION_UNIT_COL: String = "unit"
        const val CUSTOMER_PRICE_DISTRIBUTION_DATE_COL: String = "date"
        const val CUSTOMER_PRICE_DISTRIBUTION_COMMENTS_COL: String = "comments"
        const val CUSTOMER_PRICE_DISTRIBUTION_SOLD_COL: String = "sold"
        const val CUSTOMER_PRICE_DISTRIBUTION_USER_ID_FK_COL: String = "user_id" // Foreign key
        const val CUSTOMER_PRICE_DISTRIBUTION_PRODUCE_ID_FK_COL: String = "produce_id" // Foreign key

        // BuyingFarmer Table
        const val BUYING_FARMER_ID_COL: String = "id"
        const val BUYING_FARMER_BUYING_CENTER_COL: String = "buying_center"
        const val BUYING_FARMER_PRODUCER_COL: String = "producer"
        const val BUYING_FARMER_PRODUCE_COL: String = "produce"
        const val BUYING_FARMER_GRN_NUMBER_COL: String = "grn_number"
        const val BUYING_FARMER_UNIT_COL: String = "unit"
        const val BUYING_FARMER_QUALITY_COL: String = "quality"
        const val BUYING_FARMER_ACTION_COL: String = "action"
        const val BUYING_FARMER_WEIGHT_COL: String = "weight"
        const val BUYING_FARMER_LOADED_COL: String = "loaded"
        const val BUYING_FARMER_USER_ID_FK_COL: String = "user_id" // Foreign key

        // Quarantine Table
        const val QUARANTINE_ID_COL: String = "id"
        const val QUARANTINE_ACTION_COL: String = "action"
        const val QUARANTINE_APPROVED_BY_COL: String = "quarantine_approved_by"
        const val QUARANTINE_NEW_WEIGHT_IN_COL: String = "new_weight_in_after_sorting_or_regrading"
        const val QUARANTINE_NEW_WEIGHT_OUT_COL: String = "new_weight_out_after_sorting_or_regrading"
        const val QUARANTINE_BUYING_FARMER_ID_FK_COL: String = "buying_farmer_id" // Foreign key
        const val QUARANTINE_BUYING_CUSTOMER_ID_FK_COL: String = "buying_customer_id" // Foreign key

        // BuyingCustomer Table
        const val BUYING_CUSTOMER_ID_COL: String = "id"
        const val BUYING_CUSTOMER_PRODUCE_COL: String = "produce"
        const val BUYING_CUSTOMER_CUSTOMER_COL: String = "customer"
        const val BUYING_CUSTOMER_GRN_NUMBER_COL: String = "grn_number"
        const val BUYING_CUSTOMER_UNIT_COL: String = "unit"
        const val BUYING_CUSTOMER_QUALITY_COL: String = "quality"
        const val BUYING_CUSTOMER_ACTION_COL: String = "action"
        const val BUYING_CUSTOMER_WEIGHT_COL: String = "weight"
        const val BUYING_CUSTOMER_ONLINE_PRICE_COL: String = "online_price"
        const val BUYING_CUSTOMER_LOADED_COL: String = "loaded"
        const val BUYING_CUSTOMER_USER_ID_FK_COL: String = "user_id" // Foreign key

        // PaymentFarmer Table
        const val PAYMENT_FARMER_ID_COL: String = "id"
        const val PAYMENT_FARMER_BUYING_CENTER_COL: String = "buying_center"
        const val PAYMENT_FARMER_CIG_COL: String = "cig"
        const val PAYMENT_FARMER_PRODUCER_COL: String = "producer"
        const val PAYMENT_FARMER_GRN_COL: String = "grn"
        const val PAYMENT_FARMER_NET_BALANCE_COL: String = "net_balance"
        const val PAYMENT_FARMER_PAYMENT_TYPE_COL: String = "payment_type"
        const val PAYMENT_FARMER_OUTSTANDING_LOAN_AMOUNT_COL: String = "outstanding_loan_amount"
        const val PAYMENT_FARMER_PAYMENT_DUE_COL: String = "payment_due"
        const val PAYMENT_FARMER_SET_LOAN_DEDUCTION_COL: String = "set_loan_deduction"
        const val PAYMENT_FARMER_NET_BALANCE_BEFORE_COL: String = "net_balance_before"
        const val PAYMENT_FARMER_NET_BALANCE_AFTER_LOAN_DEDUCTION_COL: String = "net_balance_after_loan_deduction"
        const val PAYMENT_FARMER_COMMENT_COL: String = "comment"
        const val PAYMENT_FARMER_USER_ID_FK_COL: String = "user_id" // Foreign key

        // PaymentCustomer Table
        const val PAYMENT_CUSTOMER_ID_COL: String = "id"
        const val PAYMENT_CUSTOMER_VILLAGE_OR_ESTATE_COL: String = "village_or_estate"
        const val PAYMENT_CUSTOMER_CUSTOMER_COL: String = "customer"
        const val PAYMENT_CUSTOMER_GRN_COL: String = "grn"
        const val PAYMENT_CUSTOMER_AMOUNT_COL: String = "amount"
        const val PAYMENT_CUSTOMER_NET_BALANCE_COL: String = "net_balance"
        const val PAYMENT_CUSTOMER_PAYMENT_TYPE_COL: String = "payment_type"
        const val PAYMENT_CUSTOMER_ENTER_AMOUNT_COL: String = "enter_amount"
        const val PAYMENT_CUSTOMER_NET_BALANCE_BEFORE_COL: String = "net_balance_before"
        const val PAYMENT_CUSTOMER_NET_BALANCE_AFTER_COL: String = "net_balance_after"
        const val PAYMENT_CUSTOMER_COMMENT_COL: String = "comment"
        const val PAYMENT_CUSTOMER_USER_ID_FK_COL: String = "user_id" // Foreign key

        // PlanJourney Table
        const val PLAN_JOURNEY_ID_COL: String = "id"
        const val PLAN_JOURNEY_TRUCK_COL: String = "truck"
        const val PLAN_JOURNEY_DRIVER_COL: String = "driver"
        const val PLAN_JOURNEY_STARTING_MILEAGE_COL: String = "starting_mileage"
        const val PLAN_JOURNEY_STARTING_FUEL_COL: String = "starting_fuel"
        const val PLAN_JOURNEY_START_LOCATION_COL: String = "start_location"
        const val PLAN_JOURNEY_DOCUMENTATION_COL: String = "documentation"
        const val PLAN_JOURNEY_STOP_POINTS_COL: String = "stop_points"
        const val PLAN_JOURNEY_FINAL_DESTINATION_COL: String = "final_destination"
        const val PLAN_JOURNEY_DATE_AND_TIME_COL: String = "date_and_time"
        const val PLAN_JOURNEY_USER_ID_FK_COL: String = "user_id" // Foreign key

        // DispatchInput Table
        const val DISPATCH_INPUT_ID_COL: String = "id"
        const val DISPATCH_INPUT_GRN_COL: String = "grn"
        const val DISPATCH_INPUT_INPUT_COL: String = "input"
        const val DISPATCH_INPUT_DESCRIPTION_COL: String = "description"
        const val DISPATCH_INPUT_NUMBER_OF_UNITS_COL: String = "number_of_units"
        const val DISPATCH_INPUT_PLAN_JOURNEY_ID_FK_COL: String = "plan_journey_id" // Foreign key

        // Loading Table
        const val LOADING_ID_COL: String = "id"
        const val LOADING_GRN_COL: String = "grn"
        const val LOADING_TOTAL_WEIGHT_COL: String = "total_weight"
        const val LOADING_TRUCK_LOADING_NUMBER_COL: String = "truck_loading_number"
        const val LOADING_FROM_COL: String = "from_"
        const val LOADING_TO_COL: String = "to"
        const val LOADING_COMMENT_COL: String = "comment"
        const val LOADING_OFFLOADED_COL: String = "offloaded"
        const val LOADING_USER_ID_FK_COL: String = "user_id" // Foreign key

        // Offloading Table
        const val OFFLOADING_ID_COL: String = "id"
        const val OFFLOADING_OFFLOADED_LOAD_COL: String = "offloaded_load"
        const val OFFLOADING_TOTAL_WEIGHT_COL: String = "total_weight"
        const val OFFLOADING_TRUCK_OFFLOADING_NUMBER_COL: String = "truck_offloading_number"
        const val OFFLOADING_COMMENT_COL: String = "comment"
        const val OFFLOADING_USER_ID_FK_COL: String = "user_id" // Foreign key

        // Rural Worker Table
        const val RURAL_WORKER_ID_COL: String = "id"
        const val RURAL_WORKER_OTHER_NAME_COL: String = "other_name"
        const val RURAL_WORKER_LAST_NAME_COL: String = "last_name"
        const val RURAL_WORKER_RURAL_WORKER_CODE_COL: String = "rural_worker_code"
        const val RURAL_WORKER_ID_NUMBER_COL: String = "id_number"
        const val RURAL_WORKER_GENDER_COL: String = "gender"
        const val RURAL_WORKER_DATE_OF_BIRTH_COL: String = "date_of_birth"
        const val RURAL_WORKER_EMAIL_COL: String = "email"
        const val RURAL_WORKER_PHONE_NUMBER_COL: String = "phone_number"
        const val RURAL_WORKER_EDUCATION_LEVEL_COL: String = "education_level"
        const val RURAL_WORKER_SERVICE_COL: String = "service"
        const val RURAL_WORKER_OTHER_COL: String = "other"
        const val RURAL_WORKER_COUNTY_COL: String = "county"
        const val RURAL_WORKER_SUB_COUNTY_COL: String = "sub_county"
        const val RURAL_WORKER_WARD_COL: String = "ward"
        const val RURAL_WORKER_VILLAGE_COL: String = "village"
        const val RURAL_WORKER_USER_ID_FK_COL: String = "user_id" // Foreign key

        const val IS_OFFLINE_COL: String = "is_offline"

    }}
