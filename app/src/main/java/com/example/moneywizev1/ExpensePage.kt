package com.example.moneywizev1

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.util.*

class ExpensePage : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    private lateinit var multiSelectBudgetTextView: TextView
    private lateinit var budgetsList: List<String>
    private lateinit var selectedBudgets: BooleanArray
    private lateinit var database: FirebaseDatabase
    private lateinit var budgetsRef: DatabaseReference
    private lateinit var expensesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_page)

        // Firebase refs
        database = FirebaseDatabase.getInstance()
        budgetsRef = database.getReference("budgets")
        expensesRef = database.getReference("expenses")

        val addPhoto = findViewById<Button>(R.id.addPhoto)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val backButton = findViewById<Button>(R.id.button5)

        val nameField = findViewById<EditText>(R.id.editTextText8)
        val amountField = findViewById<EditText>(R.id.editTextNumber)
        val dateField = findViewById<EditText>(R.id.editTextDate2)
        val categoryField = findViewById<EditText>(R.id.editTextText6)
        val notesField = findViewById<EditText>(R.id.editTextText5)
        multiSelectBudgetTextView = findViewById(R.id.multiSelectBudget)

        // Load budgets
        loadBudgets()

        multiSelectBudgetTextView.setOnClickListener {
            showMultiSelectDialog()
        }

        // Set up date picker
        val calendar = Calendar.getInstance()
        val currentDate = "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dateField.setText(currentDate)

        dateField.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                    dateField.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, MainAct2::class.java))
            finish()
        }

        addPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            val name = nameField.text.toString()
            val amount = amountField.text.toString().toDoubleOrNull()
            val date = dateField.text.toString()
            val selectedBudgetsList = getSelectedBudgets()
            val budget = selectedBudgetsList.joinToString(",")
            val category = categoryField.text.toString()
            val notes = notesField.text.toString()

            if (name.isBlank() || amount == null || date.isBlank() || budget.isBlank() || category.isBlank() || notes.isBlank()) {
                Toast.makeText(this, "Please fill all fields except the photo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkBudgetsAndSave(name, amount, date, budget, category, notes, selectedBudgetsList)
        }
    }

    private fun loadBudgets() {
        budgetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                budgetsList = snapshot.children.mapNotNull { it.child("name").getValue(String::class.java) }
                selectedBudgets = BooleanArray(budgetsList.size)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ExpensePage, "Failed to load budgets", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showMultiSelectDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Budgets")
        builder.setMultiChoiceItems(budgetsList.toTypedArray(), selectedBudgets) { _, which, isChecked ->
            selectedBudgets[which] = isChecked
        }
        builder.setPositiveButton("OK") { dialog, _ ->
            updateMultiSelectBudgetTextView()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun updateMultiSelectBudgetTextView() {
        val selectedNames = getSelectedBudgets()
        multiSelectBudgetTextView.text = if (selectedNames.isEmpty()) "" else selectedNames.joinToString(", ")
    }

    private fun getSelectedBudgets(): List<String> {
        val selected = mutableListOf<String>()
        for (i in budgetsList.indices) {
            if (selectedBudgets[i]) selected.add(budgetsList[i])
        }
        return selected
    }

    private fun checkBudgetsAndSave(
        name: String,
        amount: Double,
        date: String,
        budget: String,
        category: String,
        notes: String,
        selectedBudgetsList: List<String>
    ) {
        budgetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (selectedBudget in selectedBudgetsList) {
                    val budgetSnap = snapshot.children.find { it.child("name").value == selectedBudget }
                    if (budgetSnap != null) {
                        val maxSpend = budgetSnap.child("maxspend").getValue(Double::class.java) ?: Double.MAX_VALUE
                        val capital = budgetSnap.child("capital").getValue(Double::class.java) ?: 0.0

                        if (amount > maxSpend || amount > capital) {
                            Toast.makeText(this@ExpensePage, "Expense exceeds max spend or capital for $selectedBudget", Toast.LENGTH_LONG).show()
                            return
                        }
                    }
                }

                // Save expense as transaction under each selected budget
                for (selectedBudget in selectedBudgetsList) {
                    val transactionId = FirebaseDatabase.getInstance().getReference("transactions").child(selectedBudget).push().key ?: continue
                    val transactionData = mapOf(
                        "id" to transactionId,
                        "name" to name,
                        "amount" to amount,
                        "date" to date,
                        "category" to category,
                        "type" to "Expense",  // important: mark as expense
                        "notes" to notes,
                        "photoUri" to selectedImageUri?.toString()
                    )
                    FirebaseDatabase.getInstance().getReference("transactions").child(selectedBudget).child(transactionId)
                        .setValue(transactionData)
                }

                updateBudgetsCapital(selectedBudgetsList, amount)
                Toast.makeText(this@ExpensePage, "Expense saved!", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ExpensePage, "Error validating budget limits", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun updateBudgetsCapital(selectedBudgetsList: List<String>, amount: Double) {
        budgetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (selectedBudget in selectedBudgetsList) {
                    val budgetSnap = snapshot.children.find { it.child("name").value == selectedBudget }
                    if (budgetSnap != null) {
                        val capital = budgetSnap.child("capital").getValue(Double::class.java) ?: continue
                        val newCapital = capital - amount
                        budgetSnap.ref.child("capital").setValue(newCapital)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


// TonikamiTV (2015). Android Studio Tutorial - Upload Picture Part 1 - User Interface. [online] YouTube. Available at: https://www.youtube.com/watch?v=e8x-nu9-_BM [Accessed 25 Apr. 2025].

