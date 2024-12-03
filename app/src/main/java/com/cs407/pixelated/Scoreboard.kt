package com.cs407.pixelated

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Scoreboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scoreboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //add bar at bottom (top) to exit scoreboard
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = "Scoreboard"

        val recentScore = intent.getIntExtra("recentScore", 0)
        val recentScoreText = findViewById<TextView>(R.id.recentScorePacman)
        recentScoreText.text = getString(R.string.pacman_recent, recentScore)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button press, typically finishing the activity
                onBackPressedDispatcher.onBackPressed()  // This finishes the current activity and navigates back
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}