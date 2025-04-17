package edu.ddukk.fixapp

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var otpEditText: EditText
    private lateinit var verifyOtpButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var verificationId: String? = null  // Stores OTP verification ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Bind UI Elements
        nameEditText = findViewById(R.id.nameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        sendOtpButton = findViewById(R.id.sendOtpButton)
        otpEditText = findViewById(R.id.otpEditText)
        verifyOtpButton = findViewById(R.id.verifyOtpButton)
        progressBar = ProgressBar(this)


        progressBar.visibility = View.GONE

        val loginTextView = findViewById<TextView>(R.id.loginTextView)
        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Send OTP Button Click
        sendOtpButton.setOnClickListener {
            val phone = phoneEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            if (phone.isEmpty() || phone.length < 10) {
                phoneEditText.error = "Enter a valid phone number"
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                nameEditText.error = "Enter your name"
                return@setOnClickListener
            }

            checkUserExists(phone, name)
        }

        // Verify OTP Button Click
        verifyOtpButton.setOnClickListener {
            val otp = otpEditText.text.toString().trim()
            if (TextUtils.isEmpty(otp)) {
                otpEditText.error = "Enter OTP"
                return@setOnClickListener
            }
            verifyOtp(otp) // Verify the entered OTP
        }
    }

    // Function to Check if User Already Exists
    private fun checkUserExists(phone: String, name: String) {
        val userRef = firestore.collection("users").document(phone)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // User exists, redirect to login
                Toast.makeText(this, "User already exists! Redirecting to Login", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                // User does not exist, send OTP
                sendOtp("+91$phone", name)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error checking user: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to Send OTP
    private fun sendOtp(phoneNumber: String, name: String) {
        progressBar.visibility = View.VISIBLE
        sendOtpButton.isEnabled = false

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential, name) // Auto-verification
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true
                Toast.makeText(this@RegisterActivity, "OTP Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                this@RegisterActivity.verificationId = verificationId
                progressBar.visibility = View.GONE

                // Show OTP Input Field
                otpEditText.visibility = View.VISIBLE
                verifyOtpButton.visibility = View.VISIBLE
                sendOtpButton.text = "Resend OTP"
                sendOtpButton.isEnabled = true

                Toast.makeText(this@RegisterActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // The phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout duration
            .setActivity(this) // Activity reference
            .setCallbacks(callbacks) // Callback function
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Function to Verify OTP
    private fun verifyOtp(code: String) {
        progressBar.visibility = View.VISIBLE
        val credential = verificationId?.let { PhoneAuthProvider.getCredential(it, code) }
        credential?.let { signInWithPhoneAuthCredential(it, nameEditText.text.toString()) }
    }

    // Function to Sign In with Firebase and Save User Data
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, name: String) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = task.result?.user
                    saveUserToFirestore(user?.phoneNumber ?: "", name)

                    // Navigate to Login
                    Toast.makeText(this, "Registered Successfully! Redirecting to Login", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "OTP Verification Failed", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Function to Save User in Firestore
    private fun saveUserToFirestore(phone: String, name: String) {
        val user = hashMapOf(
            "phone" to phone,
            "name" to name,
            "timestamp" to System.currentTimeMillis(),

        )
        firestore.collection("users").document(phone).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "User Registered Successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to Save User Data", Toast.LENGTH_SHORT).show()
            }
    }
}
