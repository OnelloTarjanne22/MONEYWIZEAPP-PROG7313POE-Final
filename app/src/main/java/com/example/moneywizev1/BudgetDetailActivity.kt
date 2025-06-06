package com.example.moneywizev1

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class BudgetDetailActivity : AppCompatActivity() {

    private var currentCount = 0
    private var goal = 0
    private var celebrationDone = false
    private lateinit var budgetName: String
    private lateinit var listView: ListView

    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPercentage: TextView
    private lateinit var konfettiView: KonfettiView
    private lateinit var maxspendTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_detail)

        tvProgress = findViewById(R.id.tvProgress)
        progressBar = findViewById(R.id.progressBar)
        tvPercentage = findViewById(R.id.tvPercentage)
        konfettiView = findViewById(R.id.konfettiView)
        maxspendTextView = findViewById(R.id.maxspendTextView)
        listView = findViewById(R.id.transactionsListView)

        findViewById<Button>(R.id.button7).setOnClickListener { finish() }

        budgetName = intent.getStringExtra("budgetName") ?: ""
        goal = intent.getIntExtra("budgetAmount", 100)

        fetchBudgetData(budgetName)
    }

    private fun fetchBudgetData(budgetName: String) {
        val budgetRef = FirebaseDatabase.getInstance().getReference("budgets")
        budgetRef.orderByChild("name").equalTo(budgetName).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val budgetSnapshot = snapshot.children.first()
                val budget = budgetSnapshot.getValue(BudgetModel::class.java)

                val capital = budget?.capital?.toInt() ?: 0
                val amount = budget?.amount?.toInt() ?: 0
                val monthlyGoal = budget?.monthlyGoal?.toInt() ?: 0
                val maxspend = budget?.maxspend ?: 0.0

                goal = amount

                fetchTransactions(budgetName) { transactions ->
                    var income = 0
                    var expenses = 0

                    for (t in transactions) {
                        if (t.type == "Income") {
                            income += t.amount.toInt()
                        } else if (t.type == "Expense") {
                            expenses += t.amount.toInt()
                        }
                    }

                    currentCount = (capital + income) - expenses

                    maxspendTextView.text =
                        "Spending limit: R${String.format("%.2f", maxspend)} | Budget Goal: R$amount | Monthly Goal: R$monthlyGoal"

                    updateUI()
                    listView.adapter = TransactionAdapter(this, transactions)
                }
            } else {
                Toast.makeText(this, "Budget not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load budget", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchTransactions(budgetName: String, callback: (List<Transaction>) -> Unit) {
        val transactions = mutableListOf<Transaction>()
        val dbRef = FirebaseDatabase.getInstance().getReference("transactions").child(budgetName)

        dbRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                val transaction = child.getValue(Transaction::class.java)
                transaction?.let { transactions.add(it) }
            }
            callback(transactions.sortedByDescending { it.date })
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load transactions", Toast.LENGTH_SHORT).show()
            callback(emptyList())
        }
    }

    private fun updateUI() {
        val percentage = if (goal > 0) (currentCount.toFloat() / goal * 100).toInt() else 0

        Log.d("BudgetDetail", "currentCount: $currentCount, goal: $goal, percentage: $percentage")

        tvProgress.text = "$budgetName Progress: R$currentCount / R$goal"
        progressBar.max = goal
        progressBar.progress = currentCount.coerceAtMost(goal)
        tvPercentage.text = "$percentage% Complete"

        if (currentCount >= goal && !celebrationDone) {
            celebrationDone = true
            launchConfetti()
            showGoalPopup()
        }
    }

    private fun launchConfetti() {
        val party = Party(
            speed = 5f,
            maxSpeed = 50f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(
                0xfffce18a.toInt(),
                0xffff726d.toInt(),
                0xffb48def.toInt(),
                0xfff4306d.toInt()
            ),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(300),
            position = Position.Relative(0.5, 0.0),
            size = listOf(Size.SMALL, Size.LARGE),
            shapes = listOf(Shape.Square, Shape.Circle)
        )
        konfettiView.start(party)
    }

    private fun showGoalPopup() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("🎉 Goal Complete!")
            .setMessage("You completed your goal for $budgetName!")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    inner class TransactionAdapter(
        context: android.content.Context,
        transactions: List<Transaction>
    ) : ArrayAdapter<Transaction>(context, 0, transactions) {

        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = convertView ?: layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            val transaction = getItem(position)

            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)

            if (transaction != null) {
                text1.text = "${transaction.type}: ${transaction.name} (${transaction.category})"
                text2.text = "Amount: R${String.format("%.2f", transaction.amount)} | Date: ${transaction.date}"

                // Red for expenses, green for incomes
                val color = if (transaction.type == "Expense") 0xFFFF0000.toInt() else 0xFF008000.toInt()
                text1.setTextColor(color)
                text2.setTextColor(color)
            }

            return view
        }
    }
}






