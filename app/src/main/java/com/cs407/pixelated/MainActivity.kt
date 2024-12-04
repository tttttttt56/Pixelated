package com.cs407.pixelated

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

class MainActivity : AppCompatActivity() {
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

        findViewById<ImageButton>(R.id.galaga).setOnClickListener(){
            val intent = Intent(this,GalagaActivity::class.java)
            //Give input if needed
            //intent.putExtra("EXTRA_MESSAGE",userInput)
            //start activity
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.pacman).setOnClickListener(){
            val userIntent = Intent(this, PacmanActivity::class.java)
            intent = userIntent
            intent.putExtra("userId", userId)
            //start activity
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.centipede).setOnClickListener(){
            val intent = Intent(this,CentipedeActivity::class.java)
            //Give input if needed
            //intent.putExtra("EXTRA_MESSAGE",userInput)
            //start activity
            startActivity(intent)
        }

        findViewById<Button>(R.id.arcade_map).setOnClickListener(){
            val intent = Intent(this,ArcadeMap::class.java)
            //Give input if needed
            //intent.putExtra("EXTRA_MESSAGE",userInput)
            //start activity
            startActivity(intent)
        }

        findViewById<Button>(R.id.scoreboard).setOnClickListener(){
            val userIntent = Intent(this, Scoreboard::class.java)
            intent = userIntent
            intent.putExtra("userId", userId)
            //start activity
            startActivity(intent)
        }

    }

    // popup menu anchored to the profile button
    private fun showProfileMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.top_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    Log.d("Menu", "Profile item clicked")
                    // handle the profile item action here
                    val intent = Intent(this,ProfileActivity::class.java)
                    //Give input if needed
                    //intent.putExtra("EXTRA_MESSAGE",userInput)
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
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu,menu);
        return super.onCreateOptionsMenu(menu)
    }
}