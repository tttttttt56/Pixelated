package com.cs407.pixelated

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

        /*val onlineArcButton = findViewById<Button>(R.id.Scoreboard);
        onlineArcButton.setOnClickListener{
            val intent = Intent(this,Scoreboard::class.java)

            startActivity(intent)
        }*/

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //TODO: finish
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //TODO
        menuInflater.inflate(R.menu.top_menu,menu);
        return super.onCreateOptionsMenu(menu)
    }
}