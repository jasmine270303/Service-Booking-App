package edu.ddukk.fixapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CustomerActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var serviceRequestAdapter: ServiceRequestAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var serviceTypeSpinner: Spinner
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var notesEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var btnLogout: Button
    private lateinit var selectedDateTextView: TextView
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        serviceTypeSpinner = findViewById(R.id.serviceTypeSpinner)
        dateButton = findViewById(R.id.dateButton)
        timeButton = findViewById(R.id.timeButton)
        notesEditText = findViewById(R.id.notesEditText)
        submitButton = findViewById(R.id.submitButton)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        btnLogout = findViewById(R.id.btnLogout)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        serviceRequestAdapter = ServiceRequestAdapter(mutableListOf()) { request -> }
        recyclerView.adapter = serviceRequestAdapter

        // Setup spinner with service types
        val serviceTypes = arrayOf("Plumbing", "Electrical", "Carpentry", "Painting", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceTypeSpinner.adapter = adapter

        dateButton.setOnClickListener { pickDate() }
        timeButton.setOnClickListener { pickTime() }
        submitButton.setOnClickListener { submitRequest() }

        btnLogout.setOnClickListener {
            auth.signOut() // Sign out from Firebase
            val intent = Intent(this, LoginActivity::class.java) // Change to your actual login activity
            startActivity(intent)
        }

        loadServiceRequests()
    }

    private fun loadServiceRequests() {
        val currentUser = auth.currentUser ?: return

        db.collection("service_requests")
            .whereEqualTo("customerId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                val requests = mutableListOf<ServiceRequest>()
                for (document in documents) {
                    val request = document.toObject(ServiceRequest::class.java).copy(id = document.id)
                    requests.add(request)
                }
                serviceRequestAdapter.updateData(requests)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load requests: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, { _, year, month, day ->
            selectedDate = "$day/${month + 1}/$year"
            updateSelectedDateTime()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        // Prevent selecting past dates
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun pickTime() {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(this, { _, hour, minute ->
            selectedTime = String.format("%02d:%02d", hour, minute)
            updateSelectedDateTime()
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
        timePicker.show()
    }

    private fun updateSelectedDateTime() {
        selectedDateTextView.text = "Selected: $selectedDate $selectedTime"
    }

    private fun submitRequest() {
        // Null and validation checks
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceType = serviceTypeSpinner.selectedItem?.toString() ?: run {
            Toast.makeText(this, "Please select a service type", Toast.LENGTH_SHORT).show()
            return
        }

        val notes = notesEditText.text?.toString() ?: ""
        val customerId = currentUser.uid

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }

        val dateTime = "$selectedDate $selectedTime"

        val request = hashMapOf(
            "customerId" to customerId,
            "providerId" to null,
            "serviceType" to serviceType,
            "description" to notes,
            "dateTime" to dateTime,
            "status" to "Pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("service_requests").add(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Request submitted!", Toast.LENGTH_SHORT).show()
                loadServiceRequests()
                clearForm()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to submit request: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // Optional method to reset form after submission
    private fun clearForm() {
        serviceTypeSpinner.setSelection(0)
        notesEditText.text.clear()
        selectedDate = ""
        selectedTime = ""
        selectedDateTextView.text = "Select Date and Time"
    }
}