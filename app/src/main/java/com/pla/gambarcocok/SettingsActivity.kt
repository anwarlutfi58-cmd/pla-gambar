package com.pla.gambarcocok

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = getSharedPreferences("gambarCocok", MODE_PRIVATE)

        val et1 = findViewById<EditText>(R.id.etApi1)
        val et2 = findViewById<EditText>(R.id.etApi2)
        val et3 = findViewById<EditText>(R.id.etApi3)
        val et4 = findViewById<EditText>(R.id.etApi4)
        val txtSaved = findViewById<TextView>(R.id.txtSaved)

        et1.setText(prefs.getString("api1", ""))
        et2.setText(prefs.getString("api2", ""))
        et3.setText(prefs.getString("api3", ""))
        et4.setText(prefs.getString("api4", ""))

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putString("api1", et1.text.toString().trim())
                .putString("api2", et2.text.toString().trim())
                .putString("api3", et3.text.toString().trim())
                .putString("api4", et4.text.toString().trim())
                .apply()
            txtSaved.visibility = View.VISIBLE
            txtSaved.postDelayed({ txtSaved.visibility = View.GONE }, 2000)
        }
    }
}