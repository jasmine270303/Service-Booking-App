package edu.ddukk.fixapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ServiceProviderActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var serviceRequestAdapter: ServiceRequestAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var requestListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_provider)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        serviceRequestAdapter = ServiceRequestAdapter(mutableListOf()) { request ->
            acceptServiceRequest(request)
        }
        recyclerView.adapter = serviceRequestAdapter

        loadAvailableRequests()

    }

    private fun loadAvailableRequests() {
        // Real-time listener instead of one-time fetch
        requestListener = db.collection("service_requests")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load requests: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val requests = mutableListOf<ServiceRequest>()
                if (documents != null) {
                    for (document in documents) {
                        val request = document.toObject(ServiceRequest::class.java).copy(id = document.id)
                        requests.add(request)
                    }
                }

                serviceRequestAdapter.updateData(requests)
            }
    }

    private fun acceptServiceRequest(request: ServiceRequest) {
        val providerId = auth.currentUser?.uid ?: return

        db.collection("service_requests").document(request.id)
            .update(mapOf("status" to "Accepted", "providerId" to providerId))
            .addOnSuccessListener {
                Toast.makeText(this, "Service request accepted!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        requestListener?.remove()
    }
}
