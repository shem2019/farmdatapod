package com.example.farmdatapod.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.example.farmdatapod.R
import com.example.farmdatapod.models.RouteStopPoint

class StopPointAdapter(
    private val stopPoints: MutableList<RouteStopPoint>
) : RecyclerView.Adapter<StopPointAdapter.StopPointViewHolder>() {

    private var onDataChangedListener: ((List<RouteStopPoint>) -> Unit)? = null

    fun setOnDataChangedListener(listener: (List<RouteStopPoint>) -> Unit) {
        onDataChangedListener = listener
    }

    class StopPointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stopPointNameEditText: TextInputEditText = itemView.findViewById(R.id.stopPointName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopPointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_add_stop_over, parent, false)
        return StopPointViewHolder(view)
    }

    override fun onBindViewHolder(holder: StopPointViewHolder, position: Int) {
        val stopPoint = stopPoints[position]
        holder.stopPointNameEditText.setText(stopPoint.stop)

        // Remove any existing TextWatchers to avoid duplicates
        holder.stopPointNameEditText.removeTextChangedListener(holder.stopPointNameEditText.tag as? TextWatcher)

        // Create and set a new TextWatcher
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                stopPoint.stop = s.toString()
                onDataChangedListener?.invoke(stopPoints)
            }
        }

        // Set the TextWatcher and save it as a tag for future removal
        holder.stopPointNameEditText.addTextChangedListener(textWatcher)
        holder.stopPointNameEditText.tag = textWatcher
    }

    override fun getItemCount(): Int = stopPoints.size

    fun addStopPoint(newStopPoint: RouteStopPoint) {
        stopPoints.add(newStopPoint)
        notifyItemInserted(stopPoints.size - 1)
        onDataChangedListener?.invoke(stopPoints)
    }

    fun getStopPoints(): List<RouteStopPoint> = stopPoints

    fun clearStopPoints() {
        val size = stopPoints.size
        stopPoints.clear()
        notifyItemRangeRemoved(0, size)
        onDataChangedListener?.invoke(stopPoints)
    }
}