package edu.ddukk.fixapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoleSelectionActivity : AppCompatActivity() {
    private lateinit var btnCustomer: Button
    private lateinit var btnProvider: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnCustomer = findViewById(R.id.btnCustomer)
        btnProvider = findViewById(R.id.btnProvider)

        btnCustomer.setOnClickListener { saveUserRole("customer") }
        btnProvider.setOnClickListener { saveUserRole("provider") }
    }

    private fun saveUserRole(role: String) {
        val userId = auth.currentUser?.uid ?: return
        val user = hashMapOf("role" to role, "phone" to auth.currentUser?.phoneNumber)

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                val intent = if (role == "customer") Intent(this, CustomerActivity::class.java)
                else Intent(this, ServiceProviderActivity::class.java)
                startActivity(intent)
                finish()
            }
    }
}
