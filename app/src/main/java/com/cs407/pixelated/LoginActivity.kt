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
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
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

        usernameEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        passwordEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        // Set the login button click action
        loginButton.setOnClickListener {

            // get the entered username and password from EditText fields
            val currUsername = usernameEditText.text.toString()
            val currPassword = passwordEditText.text.toString()

            // set the logged-in user in the ViewModel (store user info) (placeholder)
            userViewModel.setUser(UserState(0, currUsername, currPassword))

            // navigate to main activity after successful login
            val loginSuccessful = getUserPasswd(currUsername, currPassword)
            if (loginSuccessful) {
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    //Give input if needed
                    //intent.putExtra("EXTRA_MESSAGE",userInput)
                    //start activity
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

        // add text change listeners to hide error message when user types
        usernameEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }

        passwordEditText.doAfterTextChanged {
            errorTextView.visibility = View.GONE
        }
    }

    private fun getUserPasswd(
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
        } else {
            // if the user doesn't exist in SharedPreferences, create a new user
            val editor = userPasswdKV.edit()
            editor?.putString(name, password)
            editor?.apply()
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