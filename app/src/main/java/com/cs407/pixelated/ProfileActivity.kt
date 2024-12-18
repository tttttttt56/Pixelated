package com.cs407.pixelated

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity(private val injectedUserViewModel: UserViewModel? = null) : AppCompatActivity() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var userPasswdKV: SharedPreferences
    private lateinit var appDB: PixelDatabase
    private lateinit var profileImageButton: ImageButton
    private lateinit var editFavoriteAchievementOne: ImageButton
    private lateinit var editFavoriteAchievementTwo: ImageButton
    private lateinit var editFavoriteAchievementThree: ImageButton
    private lateinit var achievementDescriptionOne: String
    private lateinit var achievementDescriptionTwo: String
    private lateinit var achievementDescriptionThree: String
    private lateinit var favoriteGameImage: ImageView
    private lateinit var favoriteGameText: TextView
    private lateinit var editFavoriteButton: ImageButton
    private var level = 1

    private lateinit var sharedPref: SharedPreferences

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
        appDB = PixelDatabase.getDatabase(this)
        val userId = intent.getIntExtra("userId", -1)
        val userPreferencesManager = UserPreferencesManager(this)
        CoroutineScope(Dispatchers.Default).launch {
            val username = appDB.userDao().getById(userId.toString()).userName
            val scoreboardId = appDB.userDao().getScoreboardIdByUserId(userId)
            val highestPacman = appDB.scoreboardDao().getHighscoreByScoreboardId(scoreboardId)
            var usernameProfileTextView = findViewById<TextView>(R.id.usernameInProfile)
            withContext(Dispatchers.Main) {
                usernameProfileTextView?.text = getString(R.string.username_text, username)
                if (highestPacman!! >= 5010) {
                    level = 4
                } else if (highestPacman >= 3340) {
                    level = 3
                } else if (highestPacman >= 1670) {
                    level = 2
                }
                var levelProfileTextView = findViewById<TextView>(R.id.levelInProfile)
                levelProfileTextView?.text = getString(R.string.level, level)
            }
        }

        // setting this user's shared preferences
        sharedPref = userPreferencesManager.getUserSharedPreferences(userId)

        // set profile display achievement
        editFavoriteAchievementOne = findViewById(R.id.displayAchievementOne)
        editFavoriteAchievementTwo = findViewById(R.id.displayAchievementTwo)
        editFavoriteAchievementThree = findViewById(R.id.displayAchievementThree)

        achievementDescriptionOne = sharedPref.getString("achievement_description_one","").toString()
        achievementDescriptionTwo = sharedPref.getString("achievement_description_two","").toString()
        achievementDescriptionThree = sharedPref.getString("achievement_description_three","").toString()
        // on button hold, display toast message describing the displayed achievement
        editFavoriteAchievementOne.setOnLongClickListener {
            Toast.makeText(this, achievementDescriptionOne, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true // Prevent click listener from triggering
        }
        editFavoriteAchievementTwo.setOnLongClickListener {
            Toast.makeText(this, achievementDescriptionTwo, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true // Prevent click listener from triggering
        }
        editFavoriteAchievementThree.setOnLongClickListener {
            Toast.makeText(this, achievementDescriptionThree, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true // Prevent click listener from triggering
        }

        // Change display achievements
        editFavoriteAchievementOne.setOnClickListener {
            showAchievementPicker(1)
        }
        editFavoriteAchievementTwo.setOnClickListener {
            showAchievementPicker(2)
        }
        editFavoriteAchievementThree.setOnClickListener {
            showAchievementPicker(3)
        }

        // set profile avatar
        profileImageButton = findViewById(R.id.profile_blue)
        profileImageButton.setOnClickListener {
            showAvatarPickerDialog()
        }

        // set favorite game image
        favoriteGameImage = findViewById(R.id.favoriteGameImage)
        editFavoriteButton = findViewById(R.id.edit_pencil)
        favoriteGameText = findViewById(R.id.favoriteGame)
        editFavoriteButton.setOnClickListener {
            showFavoriteGameDialog()
        }
    }

    override fun onStart() {
        super.onStart()

        // Retrieve the selected avatar/game/arcade from SharedPreferences
        val selectedAvatar = sharedPref.getInt("selected_avatar", R.drawable.profile_blue)
        val selectedGame = sharedPref.getString("selected_game", "-----")
        val selectedImage = sharedPref.getInt("selected_image", R.drawable.login_text_background)
        val selectedAchievementOne = sharedPref.getInt("selected_achievement_one", R.drawable.custom_achievement_button_background)
        val selectedAchievementTwo = sharedPref.getInt("selected_achievement_two", R.drawable.custom_achievement_button_background)
        val selectedAchievementThree = sharedPref.getInt("selected_achievement_three", R.drawable.custom_achievement_button_background)

        // Set the avatar/game/arcade/achievements
        profileImageButton.setImageResource(selectedAvatar)
        favoriteGameText.text = getString(R.string.favorite_game, selectedGame)
        favoriteGameImage.setImageResource(selectedImage)
        editFavoriteAchievementOne.setImageResource(selectedAchievementOne)
        editFavoriteAchievementTwo.setImageResource(selectedAchievementTwo)
        editFavoriteAchievementThree.setImageResource(selectedAchievementThree)
    }

    private fun showAchievementPicker(selection: Int) {
        // defines the achievements in an array
        val achievementImages = arrayOf(
            R.drawable.edit_pencil,
            R.drawable.golden_ghost,
            R.drawable.arrow_up,
            R.drawable.golden_trophy
        )

        // creates alert dialog to show achievement options
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose your Display Achievement: ")

        // sets the items for the dialog and handle the click event
        builder.setItems(arrayOf("Clear Selection", "Golden Ghost", "Level Up", "Golden Trophy")) { _, which ->
            val selectedAchievement = achievementImages[which]
            if (selection == 1) {
                if (which == 0) {
                    achievementDescriptionOne = "Try selecting an achievement to display!"
                }
                if (which == 1) {
                    achievementDescriptionOne = "Golden Ghost: Earned 100 points in Pac-Man"
                }
                if(which == 2) {
                    achievementDescriptionOne = "Level up: You've reached level $level!"
                }
                if(which == 3) {
                    achievementDescriptionOne = "Golden Trophy: Congratulations, you've beat Pac-man!"
                }
                editFavoriteAchievementOne.setImageResource(selectedAchievement)
                // save selected achievement in shared preferences
                with(sharedPref.edit()) {
                    putInt("selected_achievement_one", selectedAchievement)
                    putString("achievement_description_one", achievementDescriptionOne)
                    apply()
                }
            }
            if (selection == 2) {
                if (which == 0) {
                    achievementDescriptionTwo = "Try selecting an achievement to display!"
                }
                if (which == 1) {
                    achievementDescriptionTwo = "Golden Ghost: Earned 100 points in Pac-Man"
                }
                if(which == 2) {
                    achievementDescriptionTwo = "Level up: You've reached level $level!"
                }
                if(which == 3) {
                    achievementDescriptionTwo = "Golden Trophy: Congratulations, you've beat Pac-man!"
                }
                editFavoriteAchievementTwo.setImageResource(selectedAchievement)
                // save selected achievement in shared preferences
                with(sharedPref.edit()) {
                    putInt("selected_achievement_two", selectedAchievement)
                    putString("achievement_description_two", achievementDescriptionTwo)
                    apply()
                }
            }
            if (selection == 3) {
                if (which == 0) {
                    achievementDescriptionThree = "Try selecting an achievement to display!"
                }
                if (which == 1) {
                    achievementDescriptionThree = "Golden Ghost: Earned 100 points in Pac-Man"
                }
                if(which == 2) {
                    achievementDescriptionThree = "Level up: You've reached level $level!"
                }
                if(which == 3) {
                    achievementDescriptionThree = "Golden Trophy: Congratulations, you've beat Pac-man!"
                }
                editFavoriteAchievementThree.setImageResource(selectedAchievement)
                // save selected achievement in shared preferences
                with(sharedPref.edit()) {
                    putInt("selected_achievement_three", selectedAchievement)
                    putString("achievement_description_three", achievementDescriptionThree)
                    apply()
                }
            }
        }
        builder.show()
    }

    private fun showAvatarPickerDialog() {
        // defines the avatars in an array
        val avatarImages = arrayOf(
            R.drawable.galaga_button_img,
            R.drawable.ghost_red,
            R.drawable.ghost_blue,
            R.drawable.ghost_pink,
            R.drawable.ghost_orange,
            R.drawable.pacman_open,
            R.drawable.centipede_button_img
        )

        // creates alert dialog to show avatar options
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose your Avatar")

        // sets the items for the dialog and handle the click event
        builder.setItems(arrayOf("Spaceship", "Blinky", "Inky", "Pinky",
                "Clyde", "Pacman", "Centipede")) { _, which ->
            val selectedAvatar = avatarImages[which]
            profileImageButton.setImageResource(selectedAvatar)

            // save selected avatar in shared preferences
            // todo save avatar in CORRECT shared prefs
            with(sharedPref.edit()) {
                putInt("selected_avatar", selectedAvatar)
                apply()
            }
        }
        builder.show()
    }

    private fun showFavoriteGameDialog() {
        // favorite game
        val gameImages = arrayOf(
            R.drawable.pacman_logo,
            R.drawable.galaga_logo,
            R.drawable.centipede_logo
        )

        val gameNames = arrayOf(
            "Pacman",
            "Galaga",
            "Centipede"
        )

        // creates alert dialog to choose favorite game
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose your Favorite Game")

        // sets the items for the dialog, handle image visible
        builder.setItems(arrayOf("Pacman", "Galaga", "Centipede")) {_, which ->
            // update image
            val selectedGameImage = gameImages[which]
            favoriteGameImage.setImageResource(selectedGameImage)
            // update text
            val selectedGame = gameNames[which]
            favoriteGameText.text = getString(R.string.favorite_game, selectedGame)
            // update shared preferences
            with(sharedPref.edit()) {
                putString("selected_game", selectedGame)
                putInt("selected_image", selectedGameImage)
                apply()
            }
        }
        builder.show()

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

class UserPreferencesManager(val context: Context) {
    // Get SharedPreferences for a specific user
    fun getUserSharedPreferences(userID: Int): SharedPreferences {
        val prefFileName = "user_prefs_$userID"
        return context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
    }
}