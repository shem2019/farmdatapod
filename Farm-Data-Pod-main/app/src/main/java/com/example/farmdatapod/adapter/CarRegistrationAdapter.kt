package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemCarRegistrationBinding
import com.example.farmdatapod.models.Car

class CarRegistrationAdapter(
    private var cars: MutableList<Car>
) : RecyclerView.Adapter<CarRegistrationAdapter.CarViewHolder>() {

    class CarViewHolder(val binding: ItemCarRegistrationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarRegistrationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = cars[position]
        holder.binding.apply {
            carBodyTypeInput.setText(car.car_body_type)
            carModelInput.setText(car.car_model)
            firstDriverNameInput.setText(car.driver1_name)
            secondDriverNameInput.setText(car.driver2_name)
            numberPlateInput.setText(car.number_plate)

            // Set up text change listeners to update the car object
            carBodyTypeInput.addTextChangedListener { car.car_body_type = it?.toString() ?: "" }
            carModelInput.addTextChangedListener { car.car_model = it?.toString() ?: "" }
            firstDriverNameInput.addTextChangedListener { car.driver1_name = it?.toString() ?: "" }
            secondDriverNameInput.addTextChangedListener { car.driver2_name = it?.toString() ?: "" }
            numberPlateInput.addTextChangedListener { car.number_plate = it?.toString() ?: "" }
        }
    }

    override fun getItemCount() = cars.size

    fun addCar() {
        cars.add(Car("", "", "", "", ""))
        notifyItemInserted(cars.size - 1)
    }

    fun getCars(): List<Car> = cars

    fun setCars(newCars: List<Car>) {
        cars.clear()
        cars.addAll(newCars)
        notifyDataSetChanged()
    }
    fun clearCars() {
        cars.clear()
        notifyDataSetChanged()
    }
}