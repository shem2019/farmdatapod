package com.example.farmdatapod.produce.indipendent.fieldregistration.data


import androidx.room.Embedded
import androidx.room.Relation

data class FieldRegistrationWithCrops(
    @Embedded val fieldRegistration: FieldRegistrationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "field_registration_id"
    )
    val crops: List<CropEntity>
)