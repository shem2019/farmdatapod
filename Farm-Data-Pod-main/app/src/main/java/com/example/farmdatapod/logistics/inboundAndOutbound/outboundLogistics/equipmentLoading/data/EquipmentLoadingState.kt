package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data


data class EquipmentLoadingState(
    val equipment: EquipmentLoadingEntity,
    var quantityToLoad: Int = equipment.quantity_loaded,
    var isAuthorized: Boolean = equipment.authorised
)