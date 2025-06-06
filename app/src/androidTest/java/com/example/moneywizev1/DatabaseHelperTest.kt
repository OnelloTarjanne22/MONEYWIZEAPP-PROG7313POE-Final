package com.example.moneywizev1

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperTest {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbHelper = DatabaseHelper(context)
        dbHelper.writableDatabase // force database creation
    }

    @After
    fun tearDown() {
        dbHelper.close()
        context.deleteDatabase("UserDB")
    }

    @Test
    fun testInsertUser_success() {
        val result = dbHelper.insertUser("test@example.com", "password")
        assertTrue(result)
    }

    @Test
    fun testCheckUser_correctCredentials() {
        dbHelper.insertUser("mozart@doe.com", "1234")
        val result = dbHelper.checkUser("john@doe.com", "1234")
        assertTrue(result)
    }

    @Test
    fun testInsertBudget_validInput() {
        val result = dbHelper.insertBudget( "April Budget", 3000.0, 1000.0, 500.0, 1500.0, "Notes", "2025-05-01")
        assertTrue(result)
    }


    @Test
    fun testInsertExpense_withinLimit() {
        dbHelper.insertBudget( "May Budget", 5000.0, 2000.0, 1000.0, 1500.0, "note", "2025-05-01")
        val result = dbHelper.insertExpense(context, "Dinner", 50.0, "2025-05-01", "May Budget", "Food", "note", null)
        assertTrue(result)
    }

    @Test
    fun testInsertExpense_exceedsLimit() {
        dbHelper.insertBudget( "May Budget2", 5000.0, 100.0, 1000.0, 1500.0, "note", "2025-05-01")
        val result = dbHelper.insertExpense(context, "Steak", 200.0, "2025-05-01", "May Budget2", "Food", "note", null)
        assertFalse(result)
    }

    @Test
    fun testGetCategoryWithMostExpenses() {
        dbHelper.insertBudget( "June Budget", 5000.0, 3000.0, 1000.0, 1500.0, "note", "2025-05-01")
        dbHelper.insertExpense(context, "Food 1", 100.0, "2025-05-01", "June Budget", "Food", "note", null)
        dbHelper.insertExpense(context, "Food 2", 150.0, "2025-05-01", "June Budget", "Food", "note", null)
        dbHelper.insertExpense(context, "Transport", 50.0, "2025-05-01", "June Budget", "Transport", "note", null)
        val result = dbHelper.getCategoryWithMostExpenses()
        assertEquals("Food", result)
    }
}
