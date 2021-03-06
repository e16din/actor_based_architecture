package com.e16din.mytaxi.support

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.e16din.mytaxi.MyTaxiApp

fun <T> Fragment.getNavigationResult(key: String = "result") =
    findNavController().currentBackStackEntry?.savedStateHandle?.get<T>(key)

fun <T> Fragment.getNavigationResultLiveData(key: String = "result") =
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)

fun <T> Fragment.setNavigationResult(key: String = "result", result: T) =
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)

fun Fragment.getApplication() = requireActivity().application as MyTaxiApp
