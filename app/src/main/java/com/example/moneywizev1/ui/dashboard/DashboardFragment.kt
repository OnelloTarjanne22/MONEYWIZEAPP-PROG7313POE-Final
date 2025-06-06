package com.example.moneywizev1.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.moneywizev1.Budget
import com.example.moneywizev1.BudgetDetailActivity
import com.example.moneywizev1.BudgetModel
import com.example.moneywizev1.databinding.FragmentDashboardBinding
import com.google.firebase.database.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val budgetList = mutableListOf<BudgetModel>()
    private val budgetNames = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        database = FirebaseDatabase.getInstance().getReference("budgets")

        loadBudgetsFromFirebase()

        binding.budgetListView.setOnItemClickListener { _, _, position, _ ->
            val selectedBudget = budgetList[position]

            val intent = Intent(requireContext(), BudgetDetailActivity::class.java).apply {
                putExtra("budgetName", selectedBudget.name)
                putExtra("budgetAmount", selectedBudget.amount)
                putExtra("budgetCapital", selectedBudget.capital)
                putExtra("budgetMaxSpend", selectedBudget.maxspend)
                putExtra("budgetMonthlyGoal", selectedBudget.monthlyGoal)
                putExtra("budgetNotes", selectedBudget.notes)
                putExtra("budgetDate", selectedBudget.date)
            }
            startActivity(intent)
        }

        binding.floatingActionButton3.setOnClickListener {
            val intent = Intent(requireContext(), Budget::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    private fun loadBudgetsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                budgetList.clear()
                budgetNames.clear()

                for (budgetSnapshot in snapshot.children) {
                    val budget = budgetSnapshot.getValue(BudgetModel::class.java)
                    if (budget != null) {
                        budgetList.add(budget)
                        budgetNames.add(budget.name)
                    }
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, budgetNames)
                binding.budgetListView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors here
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



