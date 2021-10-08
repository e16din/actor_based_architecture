package com.e16din.mytaxi

import android.app.Application


class MyTaxiApp : Application() {

    companion object {
        const val KEY_IS_AUTHORIZED = "KEY_IS_AUTHORIZED"
        const val KEY_LAST_LOCATION = "KEY_LAST_LOCATION"
    }
}