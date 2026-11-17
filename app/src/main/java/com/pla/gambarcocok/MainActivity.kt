package com.pla.gambarcocok

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var speech: SpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("gambarCocok", MODE_PRIVATE)
        speech = SpeechHelper(this)

        val maxLevel = prefs.getInt("maxLevel", 1)
        findViewById<TextView>(R.id.txtLevelInfo).text = getString(R.string.level_unlocked, maxLevel)

        findViewById<Button>(R.id.btnOffline).setOnClickListener {
            speech.speak("Mode offline")
            startGame("offline")
        }
        findViewById<Button>(R.id.btnOnline).setOnClickListener {
            speech.speak("Mode online")
            startGame("online")
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnHelp).setOnClickListener { showHelp() }
    }

    override fun onResume() {
        super.onResume()
        val maxLevel = prefs.getInt("maxLevel", 1)
        findViewById<TextView>(R.id.txtLevelInfo).text = getString(R.string.level_unlocked, maxLevel)
    }

    private fun startGame(mode: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("mode", mode)
        startActivity(intent)
    }

    private fun showHelp() {
        val view = layoutInflater.inflate(R.layout.dialog_help, null)
        val dlg = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<Button>(R.id.btnUnderstand).setOnClickListener { dlg.dismiss() }
        dlg.show()
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}