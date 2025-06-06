package com.example.moneywizev1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class signup : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val emailField = findViewById<EditText>(R.id.editTextSignupEmail)
        val passwordField = findViewById<EditText>(R.id.editTextSignupPassword)
        val confirmPasswordField = findViewById<EditText>(R.id.editTextSignupConfirmPassword)
        val signupButton = findViewById<Button>(R.id.btnCreateAccount)

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance().getReference("users")

        signupButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    registerUser(email, password)
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        // Optional: Check if the email already exists
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var exists = false
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && user.email == email) {
                        exists = true
                        break
                    }
                }

                if (exists) {
                    Toast.makeText(this@signup, "Email already registered", Toast.LENGTH_SHORT).show()
                } else {
                    val userId = database.push().key!!
                    val newUser = User(email, password)
                    database.child(userId).setValue(newUser).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@signup, "Account created for $email", Toast.LENGTH_SHORT).show()
                            finish() // Go back to login screen
                        } else {
                            Toast.makeText(this@signup, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@signup, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}


