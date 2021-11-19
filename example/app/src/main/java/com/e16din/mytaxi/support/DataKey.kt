package com.e16din.mytaxi.support

interface DataKey {
    val dataKey: String get() = this::class.java.simpleName
}