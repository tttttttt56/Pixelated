package com.cs407.pixelated

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var appDB: PixelDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // set toolbar
        supportActionBar?.hide()
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = "Pixelated"

        // get user id
        val userId = intent.getIntExtra("userId", -1)

        val profileButton = findViewById<ImageButton>(R.id.profile_button)
        profileButton.setOnClickListener { view ->
            showProfileMenu(view)
        }

        findViewById<ImageButton>(R.id.pacman).setOnClickListener(){
            val userIntent = Intent(this, PacmanActivity::class.java)
            intent = userIntent
            intent.putExtra("userId", userId)
            //start activity
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.arcade_map).setOnClickListener(){
            val intent = Intent(this,ArcadeMap::class.java)
            //Give input if needed
            //intent.putExtra("EXTRA_MESSAGE",userInput)
            //start activity
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.scoreboard).setOnClickListener(){
            val userIntent = Intent(this, Scoreboard::class.java)
            intent = userIntent
            intent.putExtra("userId", userId)
            //start activity
            startActivity(intent)
        }

    }

    // popup menu anchored to the profile button
    private fun showProfileMenu(view: View) {
        val userId = intent.getIntExtra("userId", -1)
        appDB = PixelDatabase.getDatabase(this)
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.top_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    Log.d("Menu", "Profile item clicked")
                    // handle the profile item action here
                    val userIntent = Intent(this,ProfileActivity::class.java)
                    intent = userIntent
                    intent.putExtra("userId", userId)
                    //start activity
                    startActivity(intent)
                    true
                }
                R.id.logout -> {
                    Log.d("Menu", "Logout item clicked")
                    val intent = Intent(this,LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.delete -> {
                    Log.d("Menu", "Logout item clicked")
                    val intent = Intent(this,LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    // delete user's information from database
                    lifecycleScope.launch {
                        val scoreboardId = appDB.userDao().getScoreboardIdByUserId(userId)
                        appDB.deleteDao().deleteScoreboard(scoreboardId)
                        appDB.deleteDao().deleteUserRelations(userId)
                        // and shared preferences
                        withContext(Dispatchers.IO) {
                            val userPasswdKV =
                                this@MainActivity.getSharedPreferences("com.cs407.pixelated.userPasswdKV", Context.MODE_PRIVATE)
                            val editor = userPasswdKV.edit()
                            // get username
                            val username = appDB.userDao().getById(userId.toString())
                            editor.remove(username.userName)
                            editor.apply()
                        }
                        appDB.deleteDao().deleteUser(userId)
                    }
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile -> {
                return true
            }
            R.id.logout -> {
                return true
            }
            R.id.delete -> {
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu,menu);
        return super.onCreateOptionsMenu(menu)
    }
}