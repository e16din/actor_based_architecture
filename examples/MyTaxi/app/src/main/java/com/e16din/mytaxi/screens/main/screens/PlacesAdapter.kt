package com.e16din.mytaxi.screens.main.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.e16din.mytaxi.databinding.ItemPlaceBinding
import com.e16din.mytaxi.server.Place

class PlacesAdapter(
    var places: List<Place>,
    private val onItemClick: (place: Place) -> Unit
) :
    RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlaceBinding.inflate(inflater, parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val item = places[position]
        holder.binding.placeLabel.text = item.name
        holder.binding.additionLabel.text = item.addition
        holder.binding.root.setOnClickListener {
            onItemClick.invoke(item)
        }
    }

    override fun getItemCount() = places.size

    class PlaceViewHolder(val binding: ItemPlaceBinding) : RecyclerView.ViewHolder(binding.root)
}
