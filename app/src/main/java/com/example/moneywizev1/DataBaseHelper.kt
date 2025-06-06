package com.example.moneywizev1
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.icu.text.SimpleDateFormat
import android.util.Log
import android.widget.Toast
import java.text.ParseException
import java.util.Locale
//This is based on tutorial by The Zone (2021)
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "UserDB", null, 7) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, password TEXT)")
        db.execSQL("""
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                budget TEXT NOT NULL,
                category TEXT NOT NULL,
                notes TEXT NOT NULL,
                imageUri TEXT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE income (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                budget TEXT NOT NULL,
                category TEXT NOT NULL,
                notes TEXT NOT NULL,
                imageUri TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE TABLE budgets (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, amount REAL,maxspend REAL,capital REAL,monthlyGoal REAL, notes TEXT, date TEXT)")

    }
    fun getBudgetDetails(budgetName: String): Pair<Int, Double> {
        val db = this.readableDatabase
        var amount = 0
        var capital = 0.0

        val cursor = db.rawQuery(
            "SELECT amount, capital FROM budgets WHERE name = ?",
            arrayOf(budgetName)
        )
        if (cursor.moveToFirst()) {
            val amountIndex = cursor.getColumnIndex("amount")
            val capitalIndex = cursor.getColumnIndex("capital")

            // This is to check if column index is valid before it accepts the  data
            if (amountIndex >= 0) {
                amount = cursor.getInt(amountIndex)
            }

            if (capitalIndex >= 0) {
                capital = cursor.getDouble(capitalIndex)
            }
        }
        cursor.close()

        return Pair(amount, capital)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS expenses")
        db.execSQL("DROP TABLE IF EXISTS income")
        db.execSQL("DROP TABLE IF EXISTS budgets")
        onCreate(db)
    }
    fun insertUser(email: String, password: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("email", email)
            put("password", password)
        }
        val result = db.insert("users", null, values)

        return result != -1L
    }
    fun addIncomeToBudgetCapital(budgetName: String, incomeAmount: Double) {
        val db = writableDatabase

        // Get current capital value for the budget
        val cursor = db.rawQuery("SELECT capital FROM budgets WHERE name = ?", arrayOf(budgetName))
        var currentCapital = 0.0
        if (cursor.moveToFirst()) {
            currentCapital = cursor.getDouble(0)
        }
        cursor.close()

        // Add income amount to current capital
        val newCapital = currentCapital + incomeAmount

        // Update the capital value in the database
        val contentValues = ContentValues().apply {
            put("capital", newCapital)
        }
        db.update("budgets", contentValues, "name = ?", arrayOf(budgetName))
    }

    fun insertExpense(context: Context,
                      name: String,
                      amount: Double,
                      date: String,
                      budget: String,
                      category: String,
                      notes: String,
                      imageUri: String?
    ): Boolean {
        // Validate date
        if (!isValidDate(date)) {
            Toast.makeText(context, "Error: Date must be in format YYYY-MM-DD", Toast.LENGTH_LONG).show()
            return false
        }
        val db = writableDatabase

        val totalCursor = db.rawQuery(
            "SELECT SUM(amount) FROM expenses WHERE budget = ?",
            arrayOf(budget)
        )
        var currentTotal = 0.0
        if (totalCursor.moveToFirst()) {
            currentTotal = totalCursor.getDouble(0)
        }
        totalCursor.close()


        val maxspendCursor = db.rawQuery(
            "SELECT maxspend FROM budgets WHERE name = ?",
            arrayOf(budget)
        )
        var maxspend = Double.MAX_VALUE // Default to high if not found
        if (maxspendCursor.moveToFirst()) {
            maxspend = maxspendCursor.getDouble(0)
        }
        maxspendCursor.close()


        if (currentTotal + amount > maxspend) {

            return false // This is if the expense not insterted, because it exceeds maxiumum spend
        }

        // Step 4: Proceed with inserting
        val values = ContentValues().apply {
            put("name", name)
            put("amount", amount)
            put("date", date)
            put("budget", budget)
            put("category", category)
            put("notes", notes)
            put("imageUri", imageUri)
        }
        val result = db.insert("expenses", null, values)
        return result != -1L
    }

    fun insertIncome(
        context: Context,
        name: String,
        amount: Double,
        date: String,
        budget: String,
        category: String,
        notes: String,
        imageUri: String?
    ): Boolean {
        // Validate date format YYYY-MM-DD
        if (!isValidDate(date)) {
            Toast.makeText(context, "Error: Date must be in format YYYY-MM-DD", Toast.LENGTH_LONG).show()
            return false
        }

        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("amount", amount)  // Store exactly what the user entered
            put("date", date)
            put("budget", budget)
            put("category", category)
            put("notes", notes)
            put("imageUri", imageUri)
        }
        val result = db.insert("income", null, values)

        // Remove automatic update here if you update capital outside this function
        // to prevent multiple additions

        return result != -1L
    }


    fun getTransactionsObjectsBetweenDates(startDate: String, endDate: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val db = readableDatabase

        val expenseCursor = db.rawQuery(
            "SELECT name, amount, date, category, notes, imageUri FROM expenses WHERE date BETWEEN ? AND ? ORDER BY date",
            arrayOf(startDate, endDate)
        )
        while (expenseCursor.moveToNext()) {
            val name = expenseCursor.getString(0)
            val amount = expenseCursor.getDouble(1)
            val date = expenseCursor.getString(2)
            val category = expenseCursor.getString(3)
            val notes = expenseCursor.getString(4) ?: ""
            val imageUri = expenseCursor.getString(5)

            transactions.add(Transaction(
                type = "Expense",
                name = name,
                amount = amount,
                date = date,
                category = category,
                notes = notes,
                imageUri = imageUri
            ))
        }
        expenseCursor.close()
        val incomeCursor = db.rawQuery(
            "SELECT name, amount, date, category, notes, imageUri FROM income WHERE date BETWEEN ? AND ? ORDER BY date",
            arrayOf(startDate, endDate)
        )
        while (incomeCursor.moveToNext()) {
            val name = incomeCursor.getString(0)
            val amount = incomeCursor.getDouble(1)
            val date = incomeCursor.getString(2)
            val category = incomeCursor.getString(3)
            val notes = incomeCursor.getString(4) ?: ""
            val imageUri = incomeCursor.getString(5)

            transactions.add(Transaction(
                type = "Income",
                name = name,
                amount = amount,
                date = date,
                category = category,
                notes = notes,
                imageUri = imageUri
            ))
        }
        incomeCursor.close()

        return transactions
    }
    fun getAllBudgets2(): List<String> {
        val budgets = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT name FROM budgets", null) // Adjust "name" and "budgets" to your actual table and field names

        if (cursor.moveToFirst()) {
            do {
                budgets.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return budgets
    }
    fun getExpensesOverTime(): List<Double> {
        val db = readableDatabase
        val expenses = mutableListOf<Double>()
        val cursor = db.rawQuery("""
        SELECT SUM(amount) as total 
        FROM expenses 
        GROUP BY date 
        ORDER BY date
    """, null)

        while (cursor.moveToNext()) {
            expenses.add(cursor.getDouble(0))
        }
        cursor.close()
        return expenses
    }
    fun getIncomeOverTime(): List<Double> {
        val db = readableDatabase
        val income = mutableListOf<Double>()

        val cursor = db.rawQuery("""
        SELECT SUM(amount) as total 
        FROM income 
        GROUP BY date 
        ORDER BY date
    """, null)

        while (cursor.moveToNext()) {
            income.add(cursor.getDouble(0))
        }
        cursor.close()
        return income
    }
    fun getCategoryWithMostExpenses(): String {
        val db = readableDatabase
        var topCategory = ""

        val cursor = db.rawQuery("""
        SELECT category, SUM(amount) as total 
        FROM expenses 
        GROUP BY category 
        ORDER BY total DESC 
        LIMIT 1
    """, null)

        if (cursor.moveToFirst()) {
            topCategory = cursor.getString(0)
        }
        cursor.close()
        return topCategory
    }

    fun getCategoryWithMostIncome(): String {
        val db = readableDatabase
        var topCategory = ""

        val cursor = db.rawQuery("""
        SELECT category, SUM(amount) as total 
        FROM income 
        GROUP BY category 
        ORDER BY total DESC 
        LIMIT 1
    """, null)

        if (cursor.moveToFirst()) {
            topCategory = cursor.getString(0)
        }
        cursor.close()
        return topCategory
    }
    fun getBudgetWithHighestCompletion(): String {
        val db = readableDatabase
        var bestBudget = ""
        var highestCompletion = 0.0

        val cursor = db.rawQuery("""
        SELECT b.name, 
               (SELECT SUM(amount) FROM expenses WHERE budget = b.name) as spent,
               b.amount as total
        FROM budgets b
    """, null)

        while (cursor.moveToNext()) {
            val name = cursor.getString(0)
            val spent = cursor.getDouble(1)
            val total = cursor.getDouble(2)
            val completion = (spent / total) * 100

            if (completion > highestCompletion) {
                highestCompletion = completion
                bestBudget = name
            }
        }
        cursor.close()
        return bestBudget
    }
    fun getAllBudgets(): List<Pair<Int, String>> {
        val budgets = mutableListOf<Pair<Int, String>>() // Pair of (id, name)
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, name FROM budgets", null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            budgets.add(Pair(id, name))
        }
        cursor.close()
        return budgets
    }
    fun getAllBudgets3(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM budgets", null)
        val budgets = mutableListOf<String>()

        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                val name = cursor.getString(nameIndex)
                budgets.add(name)
            }
        }
        cursor.close()
        db.close()
        return budgets
    }
    fun deleteBudget(name: String): Boolean {
        val db = writableDatabase
        val result = db.delete("budgets", "name = ?", arrayOf(name))
        return result > 0
    }
    private fun isValidDate(dateStr: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.isLenient = false // Enforce strict format
        return try {
            sdf.parse(dateStr)
            true
        } catch (e: ParseException) {
            false
        }
    }
    fun insertBudget(name: String, amount: Double, maxspend: Double, capital: Double,monthlyGoal: Double, notes: String, date: String): Boolean {


        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("amount", amount)
            put("maxspend", maxspend)
            put("capital", capital)
            put("monthlyGoal", monthlyGoal)
            put("notes", notes)
            put("date", date)
        }
        val result = db.insert("budgets", null, values)
        db.close()
        return result != -1L
    }
    fun checkUser(email: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE email=? AND password=?",
            arrayOf(email, password)
        )
        val exists = cursor.count > 0
        cursor.close()

        return exists
    }
    fun getTotalSpentPerCategory(startDate: String, endDate: String): List<String> {
        val results = mutableListOf<String>()
        val db = readableDatabase

        val cursor = db.rawQuery("""
        SELECT category, SUM(amount) as total 
        FROM expenses 
        WHERE date BETWEEN ? AND ? 
        GROUP BY category 
        ORDER BY total DESC
    """, arrayOf(startDate, endDate))

        while (cursor.moveToNext()) {
            val category = cursor.getString(0)
            val total = cursor.getDouble(1)
            results.add("Category: $category - Total Spent: R$total")
        }
        cursor.close()
        return results
    }
    fun getBudgetsByDate(date: String): List<Pair<Int, String>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT id, name, date FROM budgets WHERE date = ?", arrayOf(date))
        val results = mutableListOf<Pair<Int, String>>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val budgetDate = cursor.getString(2)
                val displayText = "$name\nDate: $budgetDate"
                results.add(Pair(id, displayText))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return results
    }
}



// Youtu.be. (2025b). SQLite + Android - Create Database Schema (Book Library App). [online] Available at: https://youtu.be/hJPk50p7xwA?si=75S1YxWsmkuccPoF [Accessed 23 Apr. 2025].

// Android Knowledge (2023). Upload Retrieve Image from Firebase & Display in RecyclerView, GridView, Staggered | Android Studio. [online] YouTube. Available at: https://www.youtube.com/watch?v=Hn89s4oCdS8 [Accessed 25 Apr. 2025].
