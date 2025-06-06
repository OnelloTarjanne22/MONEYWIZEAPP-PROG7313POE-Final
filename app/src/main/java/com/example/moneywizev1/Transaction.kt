package com.example.moneywizev1

data class Transaction(
    val type: String = "",         // Type of transaction: income/expense
    val name: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val category: String = "",
    val notes: String = "",
    val imageUri: String? = null
)


// Youtu.be. (2025a). Expense Manager App - Part 36 | Retrieve Income Data to Dashboard. [online] Available at: https://youtu.be/Mz3rGWZw-n8?si=dnhJrDakUIMlrhHv [Accessed 24 Apr. 2025].
