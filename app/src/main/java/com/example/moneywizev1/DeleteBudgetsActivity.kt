package com.example.moneywizev1

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class DeleteBudgetsActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var listView: ListView
    private val budgetNames = mutableListOf<String>()
    private val budgetIds = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_budgets)

        listView = findViewById(R.id.listViewBudgets)
        database = FirebaseDatabase.getInstance().getReference("budgets")

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, budgetNames)
        listView.adapter = adapter

        loadBudgets()

        listView.setOnItemClickListener { _, _, position, _ ->
            val budgetName = budgetNames[position]
            val budgetId = budgetIds[position]
            AlertDialog.Builder(this)
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete \"$budgetName\"?")
                .setPositiveButton("Yes") { _, _ -> deleteBudget(budgetId, position) }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun loadBudgets() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                budgetNames.clear()
                budgetIds.clear()
                for (budgetSnapshot in snapshot.children) {
                    val name = budgetSnapshot.child("name").getValue(String::class.java)
                    val id = budgetSnapshot.key
                    if (name != null && id != null) {
                        budgetNames.add(name)
                        budgetIds.add(id)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DeleteBudgetsActivity, "Failed to load budgets: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteBudget(budgetId: String, position: Int) {
        database.child(budgetId).removeValue()
            .addOnSuccessListener {
                budgetNames.removeAt(position)
                budgetIds.removeAt(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Budget deleted successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete budget.", Toast.LENGTH_SHORT).show()
            }
    }
}


