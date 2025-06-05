package com.example.farmdatapod.models

data class Quality(
    val x : X,
    val cerealsDiv: CerealsDiv,
    var defaultDiv: DefaultDiv,
    val honeyDiv: HoneyDiv,
    val meatDiv: MeatDiv,
    val nutsDiv: NutsDiv,
    var rabbitAndPoultryDiv: RabbitAndPoultryDiv,
    val vegetableDiv: VegetableDiv
)