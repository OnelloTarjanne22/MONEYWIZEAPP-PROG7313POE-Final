package com.example.moneywizev1

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var listView: ListView
    private lateinit var searchButton: Button
    private lateinit var categorySummaryTextView: TextView
    private lateinit var database: DatabaseReference

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        listView = findViewById(R.id.transactionListView)
        searchButton = findViewById(R.id.searchButton)
        categorySummaryTextView = findViewById(R.id.categorySummaryTextView)

        database = FirebaseDatabase.getInstance().reference.child("transactions")

        startDateEditText.setOnClickListener { showDatePickerDialog(startDateEditText) }
        endDateEditText.setOnClickListener { showDatePickerDialog(endDateEditText) }

        searchButton.setOnClickListener {
            val startDate = startDateEditText.text.toString()
            val endDate = endDateEditText.text.toString()

            if (startDate.isBlank() || endDate.isBlank()) {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fetchTransactions(startDate, endDate)
        }
    }

    private fun fetchTransactions(startDate: String, endDate: String) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionsInRange = mutableListOf<Transaction>()
                val categoryMap = mutableMapOf<String, Double>()

                for (budgetSnapshot in snapshot.children) {
                    for (transactionSnapshot in budgetSnapshot.children) {
                        val transaction = transactionSnapshot.getValue(Transaction::class.java)
                        if (transaction?.date != null && isWithinRange(transaction.date, startDate, endDate)) {
                            transactionsInRange.add(transaction)

                            if (transaction.type.equals("Expense", ignoreCase = true)) {
                                val current = categoryMap.getOrDefault(transaction.category, 0.0)
                                categoryMap[transaction.category] = current + transaction.amount
                            }
                        }
                    }
                }

                updateUI(transactionsInRange, categoryMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TransactionHistoryActivity, "Error loading data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(transactions: List<Transaction>, categoryMap: Map<String, Double>) {
        val summary = if (categoryMap.isEmpty()) {
            "No expense transactions found in this range."
        } else {
            categoryMap.entries.joinToString("\n") { (category, total) ->
                "$category: R${"%.2f".format(total)}"
            }
        }
        categorySummaryTextView.text = summary

        listView.adapter = TransactionColorAdapter(this, transactions)

        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = transactions[position]
            val intent = Intent(this, TransactionDetailActivity::class.java).apply {
                putExtra("name", selected.name)
                putExtra("amount", selected.amount)
                putExtra("date", selected.date)
                putExtra("category", selected.category)
                putExtra("notes", selected.notes)
                putExtra("imageUri", selected.imageUri ?: "")
            }
            startActivity(intent)
        }
    }

    private fun isWithinRange(dateStr: String, start: String, end: String): Boolean {
        return try {
            val date = dateFormat.parse(dateStr)
            val startDate = dateFormat.parse(start)
            val endDate = dateFormat.parse(end)
            date != null && startDate != null && endDate != null &&
                    !date.before(startDate) && !date.after(endDate)
        } catch (e: Exception) {
            false
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, day)
                editText.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    inner class TransactionColorAdapter(
        context: android.content.Context,
        private val transactions: List<Transaction>
    ) : ArrayAdapter<Transaction>(context, android.R.layout.simple_list_item_1, transactions) {

        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = convertView ?: layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            val transaction = transactions[position]

            textView.text = "${transaction.type}: ${transaction.name} - ${transaction.category} - R${transaction.amount} on ${transaction.date}"

            textView.setTextColor(
                if (transaction.type.equals("Expense", ignoreCase = true))
                    0xFFFF0000.toInt() else 0xFF008000.toInt()
            )

            return view
        }
    }
}



