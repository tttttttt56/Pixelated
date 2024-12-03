package com.cs407.pixelated

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider

class ProfileActivity(private val injectedUserViewModel: UserViewModel? = null) : AppCompatActivity() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var userPasswdKV: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // get shared preferences from using R.string.userPasswdKV as the name
        userPasswdKV =
            this.getSharedPreferences(getString(R.string.userPasswdKV), Context.MODE_PRIVATE)!!

        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // use ViewModelProvider to init UserViewModel
            ViewModelProvider(this).get(UserViewModel::class.java)
        }

        // add bar at bottom (top) to exit game
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = "Profile"

        // getting username
        // val usernameSP = userPasswdKV.getString("username", "defaultUsername")
        val username = intent.getStringExtra("username")
        val userId = intent.getIntExtra("userId", -1)
        // val userState = userViewModel.userState.value
        val usernameTextView = findViewById<TextView>(R.id.usernameInProfile)
        usernameTextView.text = getString(R.string.username_text, username)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // handle the back button press
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}