package com.e16din.mytaxi.server

import kotlinx.serialization.Serializable

@Serializable
class CarDataResult(
    val waitingTimeMinutes: Int,
    val carLocation: Place.Location,
    val hasCarArrived: Boolean = false
)