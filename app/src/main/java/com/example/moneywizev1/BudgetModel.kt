package com.example.moneywizev1

data class BudgetModel(
    var name: String = "",
    var amount: Double = 0.0,
    var maxspend: Double = 0.0,
    var capital: Double = 0.0,
    var monthlyGoal: Double = 0.0,
    var notes: String = "",
    var date: String = ""
)
