package com.e16din.mytaxi.screens.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.e16din.mytaxi.R
import com.e16din.mytaxi.databinding.ItemServiceBinding
import com.e16din.mytaxi.server.Service

class ServicesAdapter(private val services: List<Service>) :
    RecyclerView.Adapter<ServicesAdapter.OrderTypeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderTypeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemServiceBinding.inflate(inflater, parent, false)
        return OrderTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderTypeViewHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount() = services.size

    inner class OrderTypeViewHolder(private val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Service) {
            binding.carTypeImage.setImageResource(getCarIconResId(item))
            binding.carTypeLabel.text = item.name
            binding.priceLabel.text = "${item.price} ₽"
        }

        private fun getCarIconResId(item: Service) =
            when (Service.CarType.values()[item.carType]) {
                Service.CarType.Light -> R.drawable.ic_car_light
                Service.CarType.Comfort -> R.drawable.ic_car_comfort
                Service.CarType.Business -> R.drawable.ic_car_business
            }
    }
}


