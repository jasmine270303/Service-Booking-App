package edu.ddukk.fixapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServiceRequestAdapter(
    private var requests: List<ServiceRequest>,
    private val onAcceptClick: (ServiceRequest) -> Unit // Callback for handling button clicks
) : RecyclerView.Adapter<ServiceRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serviceTypeTextView: TextView = view.findViewById(R.id.serviceName)
        val dateTimeTextView: TextView = view.findViewById(R.id.serviceDate)
        val notesTextView: TextView = view.findViewById(R.id.serviceNotes)
        val acceptButton: Button = view.findViewById(R.id.acceptButton) // Add missing reference
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.service_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.serviceTypeTextView.text = request.serviceType
        holder.dateTimeTextView.text = request.dateTime
        holder.notesTextView.text = request.description

        // Set button click listener
        holder.acceptButton.setOnClickListener {
            onAcceptClick(request) // Pass the request to the callback
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateData(newRequests: List<ServiceRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }
}
