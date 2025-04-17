package edu.ddukk.fixapp

data class ServiceRequest(
    var id: String = "",
    var customerId: String = "",
    var providerId: String? = null,
    var serviceType: String = "",
    var description: String = "",
    var dateTime: String = "",
    var status: String = "Pending",
    var timestamp: Long = System.currentTimeMillis()
)
