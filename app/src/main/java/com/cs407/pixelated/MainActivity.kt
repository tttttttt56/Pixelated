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


        findViewById<Button>(R.id.galaga).setOnClickListener(){
            val intent = Intent(this,GalagaActivity::class.java)
            //Give input if needed
            //intent.putExtra("EXTRA_MESSAGE",userInput)
            //start activity
            startActivity(intent)
        }

        findViewById<Button>(R.id.pacman).setOnClickListener(){
            val intent = Intent(this,PacmanActivity::class.java)
            //Give input if needed
            //intent.putExtra("EXTRA_MESSAGE",userInput)
            //start activity
            startActivity(intent)
        }

        findViewById<Button>(R.id.centipede).setOnClickListener(){
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
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //TODO: finish menu
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //TODO
        menuInflater.inflate(R.menu.top_menu,menu);
        return super.onCreateOptionsMenu(menu)
    }
}