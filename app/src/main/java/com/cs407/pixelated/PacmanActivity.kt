package com.cs407.pixelated;

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PacmanActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private lateinit var recentScoreText: TextView
    private lateinit var highestScoreText: TextView
    private lateinit var gameOverText: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var youWonText: TextView
    private lateinit var playAgainButton: Button
    private lateinit var appDB: PixelDatabase
    private var userId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Set up the custom GameView as the content view for this activity
        setContentView(R.layout.activity_pacman)

        // Now get the reference to the GameView
        gameView = findViewById(R.id.gameView)
        // Find the views from the layout
        recentScoreText = findViewById(R.id.currentScorePacman)
        highestScoreText = findViewById(R.id.highscorePacman)
        gameOverText = findViewById(R.id.overText)
        tryAgainButton = findViewById(R.id.tryAgainButton)
        youWonText = findViewById(R.id.wonText)
        playAgainButton = findViewById(R.id.playAgainButton)

        // Ensure the GameView is initialized and set up correctly
        gameView = findViewById(R.id.gameView)
        gameView.setScoreTextView(recentScoreText)
        gameView.setHighestTextView(highestScoreText)
        gameView.setOverTextView(gameOverText)
        gameView.setAgainButton(tryAgainButton)
        gameView.setWonTextView(youWonText)
        gameView.setPlayButton(playAgainButton)

        gameView.startGame()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //add bar at bottom (top) to exit game
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = "Pacman"

        appDB = PixelDatabase.getDatabase(this)
        userId = intent.getIntExtra("userId", -1)
        gameView.setUserId(userId)

        tryAgainButton.setOnClickListener() {
            gameView.restartGame()
        }
        playAgainButton.setOnClickListener() {
            gameView.restartGame()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        appDB = PixelDatabase.getDatabase(this)
        when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button press, typically finishing the activity
                // still updating top scores
                gameView.handleCollision()
                onBackPressedDispatcher.onBackPressed()  // This finishes the current activity and navigates back
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        // Pauses the game loop when the activity is paused
        gameView.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        // Resumes the game loop when the activity is resumed
        gameView.resumeGame()
    }

    override fun onStop() {
        super.onStop()
        // TODO when game stops (player wins or loses) update scores
    }
}

class GameView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), Runnable {
    private var isRunning = false
    private var gameThread: Thread? = null
    private val surfaceHolder: SurfaceHolder = holder
    private val paint: Paint = Paint()
    private var mazeBitmap: Bitmap
    private var currScore = 0
    private var scoreTextView: TextView? = null  // Declare the TextView variable
    private var userId: Int? = null  // Add the userId property
    private lateinit var appDB: PixelDatabase
    private var highestTextView: TextView? = null
    private var gameOverTextView: TextView? = null
    private var tryAgainButton: Button? = null
    private var youWonTextView: TextView? = null
    private var playAgainButton: Button? = null

    // Setter method for userId
    fun setUserId(userId: Int?) {
        this.userId = userId
    }

    // method to set the TextView references
    fun setScoreTextView(scoreText: TextView) {
        this.scoreTextView = scoreText
    }

    fun setHighestTextView(highestScoreText: TextView) {
        this.highestTextView = highestScoreText
    }

    fun setOverTextView(gameOverText: TextView) {
        this.gameOverTextView = gameOverText
    }

    fun setAgainButton(tryAgainButton: Button) {
        this.tryAgainButton = tryAgainButton
    }

    fun setWonTextView(youWonText: TextView) {
        this.youWonTextView = youWonText
    }

    fun setPlayButton(playAgainButton: Button) {
        this.playAgainButton = playAgainButton
    }

    // Variables to keep track of PacMan Animation
    private var pacmanOpen: Bitmap
    private var pacmanClosed: Bitmap
    private var ghostBitmapBlue: Bitmap
    private var ghostBitmapRed: Bitmap
    private var ghostBitmapPink: Bitmap
    private var ghostBitmapOrange: Bitmap
    private var mouthOpen = true
    private var frameCounter = 0

    // Ghost
    private val ghosts: MutableList<Ghost> = mutableListOf() // List to store ghosts

    init {
        // Load the maze image (ensure it's in the drawable folder)
        pacmanOpen = BitmapFactory.decodeResource(context.resources, R.drawable.pacman_open)
        pacmanClosed = BitmapFactory.decodeResource(context.resources, R.drawable.pacman_closed)
        mazeBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.maze)
        mazeBitmap = Bitmap.createScaledBitmap(mazeBitmap, 1089, 1188, true)

        // Resize Pac-Man
        pacmanOpen = Bitmap.createScaledBitmap(pacmanOpen, 70, 70, true)
        pacmanClosed = Bitmap.createScaledBitmap(pacmanClosed, 70, 70, true)

        // Handle the Ghosts
        // blue
        ghostBitmapBlue = BitmapFactory.decodeResource(context.resources, R.drawable.ghost_blue)
        ghostBitmapBlue = Bitmap.createScaledBitmap(ghostBitmapBlue, 70, 70, true)
        // red
        ghostBitmapRed = BitmapFactory.decodeResource(context.resources, R.drawable.ghost_red)
        ghostBitmapRed = Bitmap.createScaledBitmap(ghostBitmapRed, 70, 70, true)
        // pink
        ghostBitmapPink = BitmapFactory.decodeResource(context.resources, R.drawable.ghost_pink)
        ghostBitmapPink = Bitmap.createScaledBitmap(ghostBitmapPink, 70, 70, true)
        // orange
        ghostBitmapOrange = BitmapFactory.decodeResource(context.resources, R.drawable.ghost_orange)
        ghostBitmapOrange = Bitmap.createScaledBitmap(ghostBitmapOrange, 70, 70, true)
    }

    private val ghostBitmaps: MutableList<Bitmap> = mutableListOf()
    init {
        ghostBitmaps.add(ghostBitmapBlue)
        ghostBitmaps.add(ghostBitmapRed)
        ghostBitmaps.add(ghostBitmapPink)
        ghostBitmaps.add(ghostBitmapOrange)
    }


    private val mazeMap = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 0, 1, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 2, 0, 1, 1, 1, 1, 2, 0, 1, 1, 1, 1, 1, 2, 0, 1, 0, 2, 1, 1, 1, 1, 1, 0, 2, 1, 1, 1, 1, 0, 2, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 2, 1),
        intArrayOf(1, 0, 0, 2, 0, 2, 0, 0, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 0, 0, 2, 0, 2, 0, 0, 1),
        intArrayOf(1, 2, 0, 1, 1, 1, 1, 2, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 2, 1, 1, 1, 1, 0, 2, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 2, 0, 2, 0, 2, 0, 2, 0, 1, 0, 0, 2, 0, 2, 0, 1, 0, 2, 0, 2, 0, 0, 1, 0, 2, 0, 2, 0, 2, 0, 2, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 1, 1, 1, 1, 2, 0, 1, 0, 2, 1, 1, 1, 1, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 2, 0, 2, 0, 2, 0, 0, 0, 2, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 2, 0, 0, 0, 2, 0, 2, 0, 2, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 0, 2, 0, 2, 0, 0, 0, 2, 0, 2, 0, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 0, 1, 0, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 2, 0, 1, 0, 2, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 2, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1, 0, 0, 0, 2, 1),
        intArrayOf(1, 0, 2, 0, 2, 1, 0, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 0, 1, 2, 0, 2, 0, 1),
        intArrayOf(1, 1, 1, 0, 0, 1, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, 1, 0, 0, 1, 1, 1),
        intArrayOf(1, 1, 1, 0, 2, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 2, 0, 1, 1, 1),
        intArrayOf(1, 2, 0, 2, 0, 2, 0, 2, 0, 1, 0, 2, 0, 2, 0, 0, 1, 0, 0, 2, 0, 2, 0, 1, 0, 2, 0, 2, 0, 2, 0, 2, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 2, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 1, 0, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 2, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    private val gridCellWidth = 33f // Adjust based on actual scaling
    private val gridCellHeight = 34f // Adjust based on actual scaling

    private var pacMan = PacMan(550f, 665f, 3f, 25f,
        mazeMap, gridCellWidth, gridCellHeight)

    private var ghost = Ghost(550f, 665f, 3f, 25f,
        mazeMap, gridCellWidth, gridCellHeight)

    // Init ghosts positions
    init {
        ghosts.add(
            Ghost(
                100f,
                100f,
                radius = 25f,
                mazeMap = mazeMap,
                gridCellWidth = 33f,
                gridCellHeight = 34f,
                speed = 5f
            )
        ) // First ghost at (100, 100)
        ghosts.add(
            Ghost(
                200f,
                200f,
                radius = 25f,
                mazeMap = mazeMap,
                gridCellWidth = 33f,
                gridCellHeight = 34f,
                speed = 5f
            )
        ) // Second ghost at (200, 200)
        ghosts.add(
            Ghost(
                300f,
                300f,
                radius = 25f,
                mazeMap = mazeMap,
                gridCellWidth = 33f,
                gridCellHeight = 34f,
                speed = 5f
            )
        ) // Third ghost at (300, 300)
        ghosts.add(
            Ghost(
                400f,
                400f,
                radius = 25f,
                mazeMap = mazeMap,
                gridCellWidth = 33f,
                gridCellHeight = 34f,
                speed = 5f
            )
        ) // fourth ghost at (400, 400)

        // Set initial movement directions for ghosts (for example, moving to the right)
        ghosts[0].direction = 5f // Moving right
        ghosts[1].direction = 10f// Moving down
        ghosts[2].direction = -5f // Moving left
        ghosts[3].direction = -5f // Moving left
    }

    init {
        paint.color = Color.YELLOW
    }

    private val pellets = mutableListOf<Pellet>()

    init {
        for (y in mazeMap.indices) {
            for (x in mazeMap[y].indices) {
                if (mazeMap[y][x] == 2) {
                    pellets.add(Pellet(x, y))
                }
            }
        }
    }

    // Start the game loop
    fun startGame() {
        isRunning = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    // Pause the game loop
    fun pauseGame() {
        isRunning = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // Resume the game loop
    fun resumeGame() {
        isRunning = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    // The game loop that runs in a separate thread
    override fun run() {
        while (isRunning) {
            if (!surfaceHolder.surface.isValid) {
                continue
            }

            val canvas = surfaceHolder.lockCanvas()
            update()  // Update game state (this includes moving Pac-Man)
            draw(canvas)
            // Animate Pac-Man’s mouth every few frames
            frameCounter++
            if (frameCounter % 20 == 0) { // Animation speed
                mouthOpen = !mouthOpen
            }
            // Draw Pac-Man with the current frame

            // Calculate offsets to center the image
            val pacManCenterX = pacMan.x - pacmanOpen.width / 2
            val pacManCenterY = pacMan.y - pacmanOpen.height / 2
            if (mouthOpen) {
                canvas.drawBitmap(pacmanOpen, pacManCenterX, pacManCenterY, paint)
            } else {
                canvas.drawBitmap(pacmanClosed, pacManCenterX, pacManCenterY, paint)
            }

            var count = 0

            for (ghost in ghosts) {
                // todo which ghost it is shouldnt matter bc theyre all the same size
                val ghostCenterX = ghost.x - ghostBitmapBlue.width / 2
                val ghostCenterY = ghost.y - ghostBitmapBlue.height / 2
                ghost.updatePosition()  // Update ghost position based on its direction
                canvas.drawBitmap(ghostBitmaps[count], ghostCenterX, ghostCenterY, paint)
                count = count + 1
            }
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun update() {
        // Move Pac-Man only if the direction has been set (after touch)
        if (pacMan.isDirectionSet) {
            // Ensure collision detection happens before moving
            pacMan.movePacMan()
        }
        // Check if Pac-Man eats any pellets
        checkForPelletConsumption()
        checkForCollisions()
        ghost.updatePosition()
        checkIfGameWon()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.BLACK)  // Clear the screen to black for the maze background

        // Draw maze (walls as black rectangles)
        for (y in mazeMap.indices) {
            for (x in mazeMap[y].indices) {
                if (mazeMap[y][x] == 1) {
                    canvas.drawRect(
                        x * gridCellWidth,
                        y * gridCellHeight,
                        (x + 1) * gridCellWidth,
                        (y + 1) * gridCellHeight,
                        paint.apply { color = Color.BLUE }
                    )
                }
            }
        }

        canvas.drawBitmap(mazeBitmap, 0f, 0f, null)

        // Draw pellets (dots) that are not eaten
        paint.color = Color.WHITE
        for (pellet in pellets) {
            if (!pellet.isEaten) {
                canvas.drawCircle(
                    pellet.x * gridCellWidth + gridCellWidth / 2,
                    pellet.y * gridCellHeight + gridCellHeight / 2,
                    8f,  // Size of the dot
                    paint
                )
            }
        }

        // Draw Pac-Man
        //paint.color = Color.YELLOW
        //canvas.drawCircle(pacMan.x, pacMan.y, 30f, paint)  // Pac-Man at its current position
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE) {
            // Calculate the direction from Pac-Man to the touch point
            val dx = event.x - pacMan.x
            val dy = event.y - pacMan.y
            val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()

            // Update Pac-Man's direction to the closest 90-degree direction
            pacMan.updateDirection(angle)

            // Mark that the direction has been set
            pacMan.isDirectionSet = true
        }
        return true
    }

    fun checkForCollisions() {
        for (ghost in ghosts) {
            // Calculate the distance between Pac-Man and the ghost
            val dx = pacMan.x - ghost.x
            val dy = pacMan.y - ghost.y
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

            // Check if the distance is less than the sum of their radii (collision threshold)
            if (distance < pacMan.radius + ghost.radius) {
                // Handle collision (e.g., end the game or reduce life)
                handleCollision()
                break // Stop checking further if a collision has occurred
            }
        }
    }

    fun handleCollision() {
        // Example action: reset the game or end it
        // You can add any behavior you'd like here (e.g., reduce lives, show game over message, etc.)
        appDB = PixelDatabase.getDatabase(context)

        // Stop the game or handle life decrement
        isRunning = false
        // Show a "Game Over" screen or reset Pac-Man’s position

        // Optionally, restart the game or show a game over message
        // For example, you could trigger a game over screen here
        (context as? PacmanActivity)?.runOnUiThread {
            // show game over
            val gameOverText = gameOverTextView
            gameOverText?.visibility = View.VISIBLE
            // show try again
            var tryAgainButton = tryAgainButton
            tryAgainButton?.visibility = View.VISIBLE
            // ubdate top high score in database if necessary
            CoroutineScope(Dispatchers.Default).launch {
                val scoreboardId = appDB.userDao().getScoreboardIdByUserId(userId!!).toString()
                appDB.scoreboardDao().updateRecentPacman(scoreboardId, currScore.toString())
                // gets most recent score not high score
                var recentScore =
                    appDB.scoreboardDao().getRecentScoreByScoreboardId(scoreboardId.toInt())
                if (currScore > recentScore!!) {
                    recentScore = currScore
                    appDB.scoreboardDao().updateHighestPacman(scoreboardId, recentScore.toString())
                }
                val currentFirstHighest =
                    appDB.scoreboardDao().getFirstHighestByScoreboardId(scoreboardId.toInt())
                val currentSecondHighest =
                    appDB.scoreboardDao().getSecondHighestByScoreboardId(scoreboardId.toInt())
                val currentThirdHighest =
                    appDB.scoreboardDao().getThirdHighestByScoreboardId(scoreboardId.toInt())
                if (recentScore == currentFirstHighest!! || recentScore == currentSecondHighest!!
                    || recentScore == currentThirdHighest!!
                ) {
                    // do nothing?
                } else if (recentScore > currentFirstHighest) {
                    // update all scores
                    appDB.scoreboardDao().updateFirstHighest(scoreboardId, recentScore.toString())
                    appDB.scoreboardDao()
                        .updateSecondHighest(scoreboardId, currentFirstHighest.toString())
                    appDB.scoreboardDao()
                        .updateThirdHighest(scoreboardId, currentSecondHighest.toString())
                } else if (recentScore > currentSecondHighest) {
                    // update 2nd and 3rd scores
                    appDB.scoreboardDao().updateSecondHighest(scoreboardId, recentScore.toString())
                    appDB.scoreboardDao()
                        .updateThirdHighest(scoreboardId, currentSecondHighest.toString())
                } else if (recentScore > currentThirdHighest) {
                    // update 3rd score
                    appDB.scoreboardDao().updateThirdHighest(scoreboardId, recentScore.toString())
                }
            }
        }
    }

    fun checkIfGameWon() {
        // Stop the game or handle life decrement
        var allPelletsEaten = true
        for (pellet in pellets) {
            if (pellet.isEaten == false) {
                allPelletsEaten = false
            }
        }

        if (allPelletsEaten) {
            isRunning = false
            // Show a "Game Over" screen or reset Pac-Man’s position

            // Optionally, restart the game or show a game over message
            // For example, you could trigger a game over screen here
            (context as? PacmanActivity)?.runOnUiThread {
                // show game won
                val youWonText = youWonTextView
                youWonText?.visibility = View.VISIBLE
                // show play again
                var playAgainButton = playAgainButton
                playAgainButton?.visibility = View.VISIBLE
            }
        }
    }

    fun restartGame() {
        // Reset Pac-Man's position and score
        pacMan.x = 550f
        pacMan.y = 665f
        pacMan.isDirectionSet = false

        var initialX = 100f
        var initialY = 100f

        // Reset ghosts' positions and directions
        ghosts.forEach { ghost ->
            ghost.x = initialX // store initial positions in the Ghost class
            ghost.y = initialY
            ghost.direction = Ghost.RIGHT // or any other initial direction
            initialX = initialX + 100
            initialY = initialY + 100
        }

        for (i in mazeMap.indices) {           // Iterating over rows
            for (j in mazeMap[i].indices) {    // Iterating over columns in row i
                for (pellet in pellets) {
                    if (pellet.calculateX() == j && pellet.calculateY() == i) {
                        pellet.isEaten = false
                    }
                }
            }
        }

        val gameOverText = gameOverTextView

        // reset current score if game lost
        if (gameOverText?.visibility == VISIBLE) {
            currScore = 0
            scoreTextView?.text = currScore.toString()
        }

        // clear text
        gameOverText?.visibility = View.GONE
        // clear try again
        var tryAgainButton = tryAgainButton
        tryAgainButton?.visibility = View.GONE
        // clear game won
        val youWonText = youWonTextView
        youWonText?.visibility = View.GONE
        // clear play again
        var playAgainButton = playAgainButton
        playAgainButton?.visibility = View.GONE

        resumeGame()
    }

    fun checkForPelletConsumption() {
        appDB = PixelDatabase.getDatabase(context)
        for (pellet in pellets) {
            // Convert the grid positions of the pellet to screen coordinates
            val pelletX = pellet.x * gridCellWidth + gridCellWidth / 2
            val pelletY = pellet.y * gridCellHeight + gridCellHeight / 2

            // Check if Pac-Man's position is close to the pellet's position
            val distance = Math.sqrt(Math.pow(pacMan.x.toDouble() - pelletX, 2.0)
                    + Math.pow(pacMan.y.toDouble() - pelletY, 2.0))
            if (distance < pacMan.radius) {
                if (!pellet.isEaten) {
                    pellet.isEaten = true
                    //Use runOnUiThread to update the UI element
                    (context as? PacmanActivity)?.runOnUiThread {
                        // increase current score, set updated score
                        currScore += 10
                        scoreTextView?.text = currScore.toString()
                        // update high score when necessary
                        CoroutineScope(Dispatchers.Default).launch {
                            val currentId = userId
                            val scoreboardId = appDB.userDao().getScoreboardIdByUserId(currentId!!).toString()
                            // update current score
                            appDB.scoreboardDao().updateRecentPacman(scoreboardId, currScore.toString())
                            var highScore = appDB.scoreboardDao().getHighscoreByScoreboardId(scoreboardId.toInt())
                            if (currScore > highScore!!) {
                                highScore = currScore
                                appDB.scoreboardDao().updateHighestPacman(scoreboardId, highScore.toString())
                            }
                            //update all textviews
                            (context as? PacmanActivity)?.runOnUiThread {
                                highestTextView?.text = highScore.toString()
                            }
                        }
                    }
                }
            }
        }
    }
}

class PacMan(var x: Float, var y: Float, val speed: Float, val radius: Float,
             val mazeMap: Array<IntArray>, val gridCellWidth: Float, val gridCellHeight: Float) {

    companion object {
        const val RIGHT = 0f
        const val DOWN = Math.PI.toFloat() / 2f
        const val LEFT = Math.PI.toFloat()
        const val UP = 3 * Math.PI.toFloat() / 2f
    }

    var direction: Float = RIGHT
    var isDirectionSet: Boolean = false // Flag to track if direction has been set by touch

    fun updateDirection(newDirection: Float) {
        direction = when {
            newDirection >= -Math.PI / 4 && newDirection < Math.PI / 4 -> RIGHT
            newDirection >= Math.PI / 4 && newDirection < 3 * Math.PI / 4 -> DOWN
            newDirection >= -3 * Math.PI / 4 && newDirection < -Math.PI / 4 -> UP
            else -> LEFT
        }
    }

    fun isNextPositionBlocked(): Boolean {
        // Predict the next position based on direction and speed
        val moveX = Math.cos(direction.toDouble()).toFloat() * speed
        val moveY = Math.sin(direction.toDouble()).toFloat() * speed

        val nextX = x + moveX
        val nextY = y + moveY

        Log.d("Next Position", "Next position predicted to be at ($nextX, $nextY)")

        // We calculate the grid cell where the predicted position would land
        val nextGridX: Int
        val nextGridY: Int
        if (direction == RIGHT) {
            nextGridX = ((nextX + radius) / gridCellWidth).toInt()
            nextGridY = (nextY / gridCellHeight).toInt()
        } else if (direction == LEFT) {
            nextGridX = ((nextX - radius) / gridCellWidth).toInt()
            nextGridY = (nextY / gridCellHeight).toInt()
        } else if (direction == UP) {
            nextGridY = ((nextY - radius) / gridCellHeight).toInt()
            nextGridX = (nextX / gridCellWidth).toInt()
        } else {
            nextGridY = ((nextY + radius) / gridCellHeight).toInt()
            nextGridX = (nextX / gridCellWidth).toInt()
        }

        Log.d("Next Grids", "Next grid predicted to be ($nextGridX, $nextGridY)")

        // Ensure the new position is within bounds of the maze
        if (nextGridX in 0 until mazeMap[0].size && nextGridY in 0 until mazeMap.size) {
            // Check if the predicted position would land on a wall (1 = wall)
            if (mazeMap[nextGridY][nextGridX] == 1) {
                //if(this.isBoundaryInRadius()) {
                Log.e("Wall?", "boundary in radius")
                this.isDirectionSet = false
                return true // The next position is blocked
                //}
            }
        }

        return false // The next position is not blocked
    }

    fun movePacMan() {
        // Only move Pac-Man if the next position is not blocked
        if (!isNextPositionBlocked()) {
            // Predict the next position based on direction and speed
            val moveX = Math.cos(direction.toDouble()).toFloat() * speed
            val moveY = Math.sin(direction.toDouble()).toFloat() * speed

            x += moveX
            y += moveY // Move Pac-Man to the new position
        }
    }
}

class Pellet(val x: Int, val y: Int) {
    var isEaten: Boolean = false

    fun calculateX(): Int {
        return x
    }

    fun calculateY(): Int {
        return y
    }
}
class Ghost(
    var x: Float, var y: Float, val speed: Float, val radius: Float,
    val mazeMap: Array<IntArray>, val gridCellWidth: Float, val gridCellHeight: Float
) {
    companion object {
        const val RIGHT = 0f
        const val DOWN = Math.PI.toFloat() / 2f
        const val LEFT = Math.PI.toFloat()
        const val UP = 3 * Math.PI.toFloat() / 2f

        private val directions = arrayOf(RIGHT, DOWN, LEFT, UP)
    }

    var direction: Float = directions.random()

    fun updatePosition() {
        // Move ghost based on the current direction and speed
        val moveX = Math.cos(direction.toDouble()).toFloat() * speed
        val moveY = Math.sin(direction.toDouble()).toFloat() * speed

        x += moveX
        y += moveY

        if (isNextPositionBlocked()) {
            switchDirection()
        }
    }

    private fun switchDirection() {
        // Find current direction's index and switch to the next one in the array
        val currentIndex = directions.indexOf(direction)
        val randomAngle = (1..3).random()
        direction = directions[(currentIndex + randomAngle) % directions.size] // Rotate 90 degrees clockwise
    }

    fun isNextPositionBlocked(): Boolean {
        val moveX = Math.cos(direction.toDouble()).toFloat() * speed
        val moveY = Math.sin(direction.toDouble()).toFloat() * speed

        val nextX = x + moveX
        val nextY = y + moveY

        return isBlocked(nextX, nextY)
    }

    private fun isBlocked(nextX: Float, nextY: Float): Boolean {
        val nextGridX: Int = ((nextX + if (direction == RIGHT) radius else if (direction == LEFT) -radius else 0f) / gridCellWidth).toInt()
        val nextGridY: Int = ((nextY + if (direction == DOWN) radius else if (direction == UP) -radius else 0f) / gridCellHeight).toInt()

        return if (nextGridX in 0 until mazeMap[0].size && nextGridY in 0 until mazeMap.size) {
            mazeMap[nextGridY][nextGridX] == 1 // Hit a wall
        } else {
            true // Out of bounds
        }
    }
}


