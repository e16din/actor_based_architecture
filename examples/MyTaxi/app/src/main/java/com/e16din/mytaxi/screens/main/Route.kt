package com.e16din.mytaxi.screens.main

import com.e16din.mytaxi.server.Place
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
class Route(
    var startPlace: Place? = null,
    var finishPlace: Place? = null
): java.io.Serializable