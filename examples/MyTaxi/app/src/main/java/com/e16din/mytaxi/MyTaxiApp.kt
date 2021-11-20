package com.e16din.mytaxi

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.e16din.mytaxi.server.HttpClient
import com.e16din.mytaxi.support.DataKey


class MyTaxiApp : Application(), DataKey {

    companion object {
        const val KEY_IS_AUTHORIZED = "KEY_IS_AUTHORIZED"
        const val KEY_LAST_LOCATION = "KEY_LAST_LOCATION"
    }

    override fun onCreate() {
        super.onCreate()

        // todo: add auth logic and remove this setter
        sharedPreferences()
            .edit().putBoolean(KEY_IS_AUTHORIZED, true)
            .apply()
    }

    fun sharedPreferences(): SharedPreferences = getSharedPreferences(dataKey, Context.MODE_PRIVATE)

    fun httpClient() = HttpClient.instance
}