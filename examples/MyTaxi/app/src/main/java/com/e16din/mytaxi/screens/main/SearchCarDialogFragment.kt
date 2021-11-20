package com.e16din.mytaxi.screens.main

import android.os.Bundle

import android.view.ViewGroup

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.e16din.mytaxi.R


class SearchCarDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(): SearchCarDialogFragment {
            return SearchCarDialogFragment()
        }
    }

    var onCancelClick: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_fragment_search_car, container)
        val cancelButton = view.findViewById<View>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            onCancelClick?.invoke()
            dismiss()
        }
        return view
    }
}