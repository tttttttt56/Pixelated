package com.cs407.pixelated

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Scoreboard : AppCompatActivity() {
    private lateinit var appDB: PixelDatabase

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

        // initialize database
        appDB = PixelDatabase.getDatabase(this)

        // update scoreboard (pacman's scores)
        val userId = intent.getIntExtra("userId", 0)
        CoroutineScope(Dispatchers.Default).launch {
            // get values from database
            val scoreboardId = appDB.userDao().getScoreboardIdByUserId(userId)
            val recentScore = appDB.scoreboardDao().getRecentScoreByScoreboardId(scoreboardId)
            val highScore = appDB.scoreboardDao().getHighscoreByScoreboardId(scoreboardId)
            val firstHighest = appDB.scoreboardDao().getFirstHighestByScoreboardId(scoreboardId)
            val secondHighest = appDB.scoreboardDao().getSecondHighestByScoreboardId(scoreboardId)
            val thirdHighest = appDB.scoreboardDao().getThirdHighestByScoreboardId(scoreboardId)
            // get text views
            val recentScoreText = findViewById<TextView>(R.id.recentScorePacman)
            val highScoreText = findViewById<TextView>(R.id.highScorePacman)
            val firstHighestText = findViewById<TextView>(R.id.firstHighest)
            val secondHighestText = findViewById<TextView>(R.id.secondHighest)
            val thirdHighestText = findViewById<TextView>(R.id.thirdHighest)
            // set text views
            withContext(Dispatchers.Main) {
                recentScoreText.text = getString(R.string.pacman_recent, recentScore)
                highScoreText.text = getString(R.string.pacman_high, highScore)
                firstHighestText.text = getString(R.string.first_high, firstHighest)
                secondHighestText.text = getString(R.string.second_high, secondHighest)
                thirdHighestText.text = getString(R.string.third_high, thirdHighest)
            }
        }
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