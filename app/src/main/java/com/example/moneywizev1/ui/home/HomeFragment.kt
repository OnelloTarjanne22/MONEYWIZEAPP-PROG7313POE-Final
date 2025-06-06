package com.example.moneywizev1.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.graphics.Color
import com.example.moneywizev1.R
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var chart: LineChart
    private lateinit var spinnerBudget: Spinner
    private lateinit var btnSelectRange: Button

    private var selectedBudget: String? = null
    private var startDate: Date = getDefaultStartDate()
    private var endDate: Date = Date()

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        chart = view.findViewById(R.id.line_chart)
        spinnerBudget = view.findViewById(R.id.spinner_budget)
        btnSelectRange = view.findViewById(R.id.btn_select_range)

        // Initialize Firebase DB reference
        database = FirebaseDatabase.getInstance().reference

        loadBudgetsFromFirebase()

        btnSelectRange.setOnClickListener {
            showDateRangePicker()
        }

        return view
    }

    private fun loadBudgetsFromFirebase() {
        // Assuming budgets are under "budgets" node, each with "name" property
        database.child("budgets").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val budgetList = mutableListOf<String>()
                for (budgetSnapshot in snapshot.children) {
                    val name = budgetSnapshot.child("name").getValue(String::class.java)
                    name?.let { budgetList.add(it) }
                }
                setupBudgetSpinner(budgetList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load budgets: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBudgetSpinner(budgets: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, budgets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBudget.adapter = adapter

        spinnerBudget.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBudget = budgets[position]
                updateChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (budgets.isNotEmpty()) {
            selectedBudget = budgets[0]
            updateChart()
        }
    }

    private fun updateChart() {
        if (selectedBudget == null) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = sdf.format(startDate)
        val endStr = sdf.format(endDate)

        // Load income and expense data for the selected budget from Firebase
        loadIncomeForBudget(selectedBudget!!, startStr, endStr) { incomeData ->
            loadExpensesForBudget(selectedBudget!!, startStr, endStr) { expenseData ->

                val incomeEntries = mutableListOf<Entry>()
                val expenseEntries = mutableListOf<Entry>()

                var index = 0f
                for ((_, amount) in incomeData) {
                    incomeEntries.add(Entry(index++, amount.toFloat()))
                }

                index = 0f
                for ((_, amount) in expenseData) {
                    expenseEntries.add(Entry(index++, amount.toFloat()))
                }

                val incomeSet = LineDataSet(incomeEntries, "Income").apply {
                    color = Color.GREEN
                    valueTextColor = Color.BLACK
                    lineWidth = 2f
                }

                val expenseSet = LineDataSet(expenseEntries, "Expenses").apply {
                    color = Color.RED
                    valueTextColor = Color.BLACK
                    lineWidth = 2f
                }

                val lineData = LineData(incomeSet, expenseSet)
                chart.data = lineData
                chart.invalidate()
            }
        }
    }

    private fun loadIncomeForBudget(budgetName: String, startDate: String, endDate: String, callback: (List<Pair<String, Double>>) -> Unit) {
        // Query incomes filtered by budget, date range, grouped by date (simulate grouping)
        database.child("income").orderByChild("budget").equalTo(budgetName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val incomeMap = mutableMapOf<String, Double>()
                    for (incomeSnapshot in snapshot.children) {
                        val date = incomeSnapshot.child("date").getValue(String::class.java) ?: continue
                        val amount = incomeSnapshot.child("amount").getValue(Double::class.java) ?: 0.0

                        if (date in startDate..endDate) {
                            incomeMap[date] = incomeMap.getOrDefault(date, 0.0) + amount
                        }
                    }
                    // Sort by date
                    val sortedIncome = incomeMap.toList().sortedBy { it.first }
                    callback(sortedIncome)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load income: ${error.message}", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
            })
    }

    private fun loadExpensesForBudget(budgetName: String, startDate: String, endDate: String, callback: (List<Pair<String, Double>>) -> Unit) {
        database.child("expenses").orderByChild("budget").equalTo(budgetName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expenseMap = mutableMapOf<String, Double>()
                    for (expenseSnapshot in snapshot.children) {
                        val date = expenseSnapshot.child("date").getValue(String::class.java) ?: continue
                        val amount = expenseSnapshot.child("amount").getValue(Double::class.java) ?: 0.0

                        if (date in startDate..endDate) {
                            expenseMap[date] = expenseMap.getOrDefault(date, 0.0) + amount
                        }
                    }
                    // Sort by date
                    val sortedExpenses = expenseMap.toList().sortedBy { it.first }
                    callback(sortedExpenses)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load expenses: ${error.message}", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
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


