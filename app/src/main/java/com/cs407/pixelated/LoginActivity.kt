package com.cs407.pixelated

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest


class LoginActivity(
    private val injectedUserViewModel: UserViewModel? = null // For testing only
) : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var errorTextView: TextView

    private lateinit var userViewModel: UserViewModel

    private lateinit var userPasswdKV: SharedPreferences
    private lateinit var appDB: PixelDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        errorTextView = findViewById(R.id.errorTextView)

        // set action bar title
        supportActionBar?.title = "User Information"

        userViewModel = if (injectedUserViewModel != null) {
            injectedUserViewModel
        } else {
            // use ViewModelProvider to init UserViewModel
            ViewModelProvider(this).get(UserViewModel::class.java)
        }

        // get shared preferences from using R.string.userPasswdKV as the name
        userPasswdKV =
            this.getSharedPreferences(getString(R.string.userPasswdKV), Context.MODE_PRIVATE)!!
        appDB = PixelDatabase.getDatabase(this)

        usernameEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        passwordEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        // welcome text's shadow
        val shadowColor = ContextCompat.getColor(this, R.color.light_blue)
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        welcomeText.setShadowLayer(8f,6f,6f,shadowColor)
        val pixelatedText = findViewById<TextView>(R.id.pixelatedText)
        pixelatedText.setShadowLayer(8f,6f,6f,shadowColor)

        // Set the login button click action
        loginButton.setOnClickListener {

            lifecycleScope.launch {
            // get the entered username and password from EditText fields
            val currUsername = usernameEditText.text.toString()
            val currPassword = passwordEditText.text.toString()

            // set the logged-in user in the ViewModel (store user info) (placeholder)
            userViewModel.setUser(UserState(0, currUsername, currPassword))

            // navigate to main activity after successful login.
            val loginSuccessful = getUserPasswd(currUsername, currPassword)
            if (loginSuccessful) {
                try {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    val username = userViewModel.userState.value.name
                    val userId = appDB.userDao().getByName(username).userId
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("CoroutineError", "Exception occurred: ${e.message}", e)
                }
                // show an error message if either username or password is empty
            } else if (currUsername.isBlank() || currPassword.isBlank()) {
                errorTextView.visibility = View.VISIBLE
            } else {
                errorTextView.visibility = View.VISIBLE
            }
                }
        }

        // add text change listeners to hide error message when user types
        usernameEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        passwordEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }
    }

    private suspend fun getUserPasswd(
        name: String,
        passwdPlain: String
    ): Boolean {
        // hash the plain password using a secure hashing function
        val password = hash(passwdPlain)
        // check if the user exists in SharedPreferences (using the username as the key)
        if (userPasswdKV.contains(name)) {
            // retrieve the stored password from SharedPreferences
            val storedPasswd = userPasswdKV.getString(name, null)
            // compare the hashed password with the stored one and return false if they don't match
            if (password != storedPasswd) {
                return false
            }
            // TODO else, update userid in user model, check 5th lab
        } else {
            // if the user doesn't exist in SharedPreferences, create a new user
            val editor = userPasswdKV.edit()
            editor?.putString(name, password)
            editor?.apply()
            // insert the new user into the Room database (implement this in your User DAO)
            val currUser = User(0, name)
            appDB.userDao().insert(currUser)
            // insert user's scoreboard info (declared, not initialized) into Room database
            val currScoreboardInfo = ScoreboardInfo(0,0,0,
                0,0,0)
            appDB.scoreboardDao().insert(currScoreboardInfo)
            // insert relations
            val updatedUserId = appDB.userDao().getByName(name).userId
            appDB.scoreboardDao().upsertInfo(currScoreboardInfo, updatedUserId)
            // i just dont wanna get rid of this for fear of messing something up
            val updateUserIdInUserViewModel = appDB.userDao().getByName(name).userId
            userViewModel.setUser(UserState(updateUserIdInUserViewModel, name, passwdPlain))

        }
        // store the hashed password in SharedPreferences for future logins
        val editor = userPasswdKV.edit()
        editor?.putString(name, password)
        editor?.apply()

        // return true if the user login is successful or the user was newly created
        return true
    }

    private fun hash(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}