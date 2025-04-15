package com.example.caloriedetector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class ResultActivityy : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val resultText = intent.getStringExtra("RESULT_TEXT") ?: "Bilgi bulunamadı."
        val resultTextView = findViewById<TextView>(R.id.tv_result)
        resultTextView.text = resultText
    }
}
