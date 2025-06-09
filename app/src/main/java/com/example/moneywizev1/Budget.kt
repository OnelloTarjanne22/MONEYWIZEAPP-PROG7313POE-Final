package com.example.moneywizev1

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
// Adapted from code by Etcetera (2022)
class Budget : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var maxspendInput: EditText
    private lateinit var capitalInput: EditText
    private lateinit var monthlyGoalInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var confirmButton: Button
    private lateinit var mainGoalInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        nameInput = findViewById(R.id.editTextText)
        amountInput = findViewById(R.id.editTextDate)
        maxspendInput = findViewById(R.id.minSpendEditTxt)
        capitalInput = findViewById(R.id.editTextNumber)
        monthlyGoalInput = findViewById(R.id.monthlyGoalEditTxt)
        notesInput = findViewById(R.id.editTextText2)
        dateInput = findViewById(R.id.editTextDate2)
        confirmButton = findViewById(R.id.button)

        // Date Picker  dateInput
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDayOfMonth)
                    dateInput.setText(formattedDate)
                },
                year, month, day
            ).show()
        }

        confirmButton.setOnClickListener {
            saveBudgetToFirebase()
        }
    }

    private fun saveBudgetToFirebase() {
        val name = nameInput.text.toString().trim()
        val amountStr = amountInput.text.toString().trim()
        val maxspendStr = maxspendInput.text.toString().trim()
        val capitalStr = capitalInput.text.toString().trim()
        val monthlyGoalStr = monthlyGoalInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()
        val date = dateInput.text.toString().trim()

        if (name.isEmpty() || amountStr.isEmpty() || maxspendStr.isEmpty() || capitalStr.isEmpty() || monthlyGoalStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        val capital = capitalStr.toDoubleOrNull()
        val maxspend = maxspendStr.toDoubleOrNull()
        val monthlyGoal = monthlyGoalStr.toDoubleOrNull()

        if (amount == null || capital == null || maxspend == null || monthlyGoal == null) {
            Toast.makeText(this, "Amount, Capital, Spend limit, and Goal must be valid numbers.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidDate(date)) {
            Toast.makeText(this, "Date must be in format YYYY-MM-DD", Toast.LENGTH_LONG).show()
            return
        }

        if (monthlyGoal > maxspend) {
            Toast.makeText(this, "Monthly goal cannot be greater than spend limit.", Toast.LENGTH_LONG).show()
            return
        }

        val database = FirebaseDatabase.getInstance().getReference("budgets")
        val id = database.push().key ?: ""

        if (id.isEmpty()) {
            Toast.makeText(this, "Failed to generate unique ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val budget = BudgetModel(
            name = name,
            amount = amount,
            maxspend = maxspend,
            capital = capital,
            monthlyGoal = monthlyGoal,
            notes = notes,
            date = date
        )

        database.child(id).setValue(budget)
            .addOnSuccessListener {
                Toast.makeText(this, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainAct2::class.java).putExtra("navigateTo", "home"))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save budget.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isValidDate(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(dateStr)
            true
        } catch (e: Exception) {
            false
        }
    }
}


