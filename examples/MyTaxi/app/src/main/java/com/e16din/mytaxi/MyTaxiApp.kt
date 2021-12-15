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

    var isAuthorized: Boolean = false
        get() {
            return sharedPreferences()
                .getBoolean(KEY_IS_AUTHORIZED, field)
        }
        set(value) {
            field = value
            sharedPreferences()
                .edit().putBoolean(KEY_IS_AUTHORIZED, value)
                .apply()
        }

    override fun onCreate() {
        super.onCreate()
    }

    fun sharedPreferences(): SharedPreferences = getSharedPreferences(dataKey, Context.MODE_PRIVATE)

    fun httpClient() = HttpClient.instance
}