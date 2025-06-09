package com.example.moneywizev1

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.database.FirebaseDatabase
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit
import android.app.AlertDialog
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class BudgetDetailActivity : AppCompatActivity() {

    private var currentCount = 0
    private var goal = 0
    private var celebrationDone = false
    private var expenseLimitExceeded = false
    private lateinit var budgetName: String
    private lateinit var listView: ListView
    private lateinit var progressBarMinGoal: ProgressBar
    private lateinit var progressBarMaxSpend: ProgressBar
    private lateinit var tvMinGoalLabel: TextView
    private lateinit var tvMaxLimitLabel: TextView
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPercentage: TextView
    private lateinit var konfettiView: KonfettiView
    private lateinit var maxspendTextView: TextView
    private lateinit var btnDownloadBudget: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_detail)
        progressBarMinGoal = findViewById(R.id.progressBarMinGoal)
        progressBarMaxSpend = findViewById(R.id.progressBarMaxSpend)
        tvMinGoalLabel = findViewById(R.id.tvMinGoalLabel)
        tvMaxLimitLabel = findViewById(R.id.tvMaxLimitLabel)
        tvProgress = findViewById(R.id.tvProgress)
        progressBar = findViewById(R.id.progressBar)
        tvPercentage = findViewById(R.id.tvPercentage)
        konfettiView = findViewById(R.id.konfettiView)
        maxspendTextView = findViewById(R.id.maxspendTextView)
        listView = findViewById(R.id.transactionsListView)
        btnDownloadBudget = findViewById(R.id.btnDownloadBudget)
        btnDownloadBudget.setOnClickListener {
            generateBudgetReport()
        }

        findViewById<Button>(R.id.button7).setOnClickListener { finish() }

        budgetName = intent.getStringExtra("budgetName") ?: ""
        goal = intent.getIntExtra("budgetAmount", 100)

        fetchBudgetData(budgetName)
    }
    private fun generateBudgetReport() {
        val filename = "$budgetName-budget-report.txt"
        val fileContents = StringBuilder()

        fileContents.append("Budget Report for: $budgetName\n\n")
        fileContents.append("Capital Saved: R$currentCount\n")
        fileContents.append("Goal: R$goal\n\n")
        fileContents.append("Transactions:\n")

        for (i in 0 until listView.count) {
            val transaction = listView.getItemAtPosition(i) as Transaction
            fileContents.append("- ${transaction.type}: ${transaction.name} (${transaction.category}), Amount: R${String.format("%.2f", transaction.amount)}, Date: ${transaction.date}\n")
        }

        try {
            val downloadsDir = getExternalFilesDir(null)
            val file = File(downloadsDir, filename)
            file.writeText(fileContents.toString())

            Toast.makeText(this, "Report saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, contentResolver.getType(uri) ?: "text/plain")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Open budget report with"))

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save or open report: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


    private fun updateSpendProgress(totalExpenses: Int, monthlyGoal: Int, maxSpend: Double) {
        val minGoalPercentage = if (monthlyGoal > 0) (totalExpenses.toFloat() / monthlyGoal * 100).toInt() else 0
        val maxSpendPercentage = if (maxSpend > 0) (totalExpenses.toFloat() / maxSpend * 100).toInt() else 0

        progressBarMinGoal.progress = minGoalPercentage.coerceAtMost(100)
        progressBarMaxSpend.progress = maxSpendPercentage.coerceAtMost(100)

        expenseLimitExceeded = totalExpenses >= maxSpend
        if (expenseLimitExceeded) {
            showExpenseLimitExceededPopup()
        }
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
                currentCount = capital

                fetchTransactions(budgetName) { transactions ->
                    var totalExpenses = 0
                    for (t in transactions) {
                        if (t.type == "Expense") {
                            totalExpenses += t.amount.toInt()
                        }
                    }

                    updateSpendProgress(totalExpenses, monthlyGoal, maxspend)

                    val minGoalReachedText = if (totalExpenses >= monthlyGoal) " (Reached)" else ""
                    tvMinGoalLabel.text = "Min Goal: R$monthlyGoal$minGoalReachedText"

                    val maxLimitReachedText = if (totalExpenses >= maxspend) " (Reached)" else ""
                    tvMaxLimitLabel.text = "Max Limit: R${String.format("%.2f", maxspend)}$maxLimitReachedText"

                    maxspendTextView.text = "Spending limit: R${String.format("%.2f", maxspend)} | Budget Goal: R$amount | Monthly Goal: R$monthlyGoal"

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

        tvProgress.text = "$budgetName savings Progress: $currentCount / R$goal"
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🎉 Goal Complete!")
            .setMessage("You completed your goal for $budgetName!")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showExpenseLimitExceededPopup() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_expense_limit_exceeded, null)
        val lottieAnimationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        lottieAnimationView.setAnimation(R.raw.warning)
        lottieAnimationView.playAnimation()

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setTitle("⚠️ Expense Limit Exceeded")
            .setMessage("You have exceeded your expense limit for $budgetName. No further expenses can be added.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    inner class TransactionAdapter(
        context: android.content.Context,
        transactions: List<Transaction>
    ) : ArrayAdapter<Transaction>(context, 0, transactions) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)
            val transaction = getItem(position)

            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)

            if (transaction != null) {
                text1.text = "${transaction.type}: ${transaction.name} (${transaction.category})"
                text2.text = "Amount: R${String.format("%.2f", transaction.amount)} | Date: ${transaction.date}"

                val color = if (transaction.type == "Expense") 0xFFFF0000.toInt() else 0xFF008000.toInt()
                text1.setTextColor(color)
                text2.setTextColor(color)
            }

            return view
        }
    }
}




