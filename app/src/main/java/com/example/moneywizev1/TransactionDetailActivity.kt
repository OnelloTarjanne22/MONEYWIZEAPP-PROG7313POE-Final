package com.example.moneywizev1

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TransactionDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        val name = intent.getStringExtra("name")
        val amount = intent.getDoubleExtra("amount", 0.0)
        val date = intent.getStringExtra("date")
        val category = intent.getStringExtra("category")
        val notes = intent.getStringExtra("notes")
        val imageUriString = intent.getStringExtra("imageUri")

        findViewById<TextView>(R.id.nameTextView).text = "Name: $name"
        findViewById<TextView>(R.id.amountTextView).text = "Amount: R$amount"
        findViewById<TextView>(R.id.dateTextView).text = "Date: $date"
        findViewById<TextView>(R.id.categoryTextView).text = "Category: $category"
        findViewById<TextView>(R.id.notesTextView).text = "Notes: $notes"


        val imageView = findViewById<ImageView>(R.id.imageView)

        if (!imageUriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(imageUriString)
                imageView.setImageURI(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                imageView.setImageResource(R.drawable.logo)
            }
        } else {
            imageView.setImageResource(R.drawable.logo)
        }
    }
}