package com.example.moneywizev1.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.moneywizev1.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color

class HomeFragment : Fragment() {

    private lateinit var chart: BarChart
    private lateinit var btnSelectRange: Button
    private lateinit var database: DatabaseReference

    private var startDate: Date = getDefaultStartDate()
    private var endDate: Date = Date()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        chart = view.findViewById(R.id.bar_chart)
        btnSelectRange = view.findViewById(R.id.btn_select_range)

        database = FirebaseDatabase.getInstance().reference

        btnSelectRange.setOnClickListener {
            showDateRangePicker()
        }

        updateChart()

        return view
    }

    private fun updateChart() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = sdf.format(startDate)
        val endStr = sdf.format(endDate)
        loadTotalSpentPerBudget(startStr, endStr) { budgetData ->
            val entries = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()
            var index = 0f

            for ((budget, totalSpent) in budgetData) {
                entries.add(BarEntry(index, totalSpent.toFloat()))
                labels.add(budget)
                index++
            }

            val dataSet = BarDataSet(entries, "Total Spent per Budget").apply {
                color = Color.parseColor("#F9A826")
                valueTextColor = Color.WHITE
                valueTextSize = 12f
            }

            val data = BarData(dataSet)
            chart.data = data

            chart.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -45f
                textColor = Color.WHITE
            }
            chart.axisLeft.apply {
                axisMinimum = 0f
                textColor = Color.WHITE
            }

            chart.axisRight.isEnabled = false
            chart.description.isEnabled = false
            chart.legend.isEnabled = true
            chart.legend.textColor = Color.WHITE
            chart.invalidate()

            if (budgetData.isNotEmpty()) {
                val top = budgetData.maxByOrNull { it.value }
                top?.let {
                    Toast.makeText(requireContext(), "Top spending: ${it.key} - R${it.value}", Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    private fun loadTotalSpentPerBudget(
        startDate: String,
        endDate: String,
        callback: (Map<String, Double>) -> Unit
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val budgetMap = mutableMapOf<String, Double>()

        // First: Load from /expenses
        database.child("expenses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(expenseSnapshot: DataSnapshot) {
                for (expense in expenseSnapshot.children) {
                    val dateStr = expense.child("date").getValue(String::class.java) ?: continue
                    val budget = expense.child("budget").getValue(String::class.java) ?: continue
                    val amount = expense.child("amount").getValue(Double::class.java) ?: 0.0

                    if (dateStr in startDate..endDate) {
                        budgetMap[budget] = budgetMap.getOrDefault(budget, 0.0) + amount
                    }
                }

                // Now: Load from /transactions/{budgetName}
                database.child("transactions").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(transSnapshot: DataSnapshot) {
                        for (budgetNode in transSnapshot.children) {
                            val budgetName = budgetNode.key ?: continue

                            for (txn in budgetNode.children) {
                                val type = txn.child("type").getValue(String::class.java)
                                val dateStr = txn.child("date").getValue(String::class.java)
                                val amount = txn.child("amount").getValue(Double::class.java) ?: 0.0

                                if (type == "Expense" && dateStr != null && dateStr in startDate..endDate) {
                                    budgetMap[budgetName] = budgetMap.getOrDefault(budgetName, 0.0) + amount
                                }
                            }
                        }

                        callback(budgetMap)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error loading transactions: ${error.message}", Toast.LENGTH_SHORT).show()
                        callback(budgetMap) // Still return what we have from /expenses
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading expenses: ${error.message}", Toast.LENGTH_SHORT).show()
                callback(emptyMap())
            }
        })
    }



    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            startDate = GregorianCalendar(y, m, d).time

            DatePickerDialog(requireContext(), { _, y2, m2, d2 ->
                endDate = GregorianCalendar(y2, m2, d2).time
                updateChart()
            }, year, month, day).show()

        }, year, month, day).show()
    }

    private fun getDefaultStartDate(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -30)
        return cal.time
    }
}
