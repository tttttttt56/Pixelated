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
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cs407.pixelated.R

class PacmanActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Set up the custom GameView as the content view for this activity
        setContentView(R.layout.activity_pacman)

        // Now get the reference to the GameView
        gameView = findViewById(R.id.gameView)

        // Ensure the GameView is initialized and set up correctly
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
}

class GameView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), Runnable {
    private var isRunning = false
    private var gameThread: Thread? = null
    private val surfaceHolder: SurfaceHolder = holder
    private val paint: Paint = Paint()
    private var mazeBitmap: Bitmap

    init {
        // Load the maze image (ensure it's in the drawable folder)
        mazeBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.maze)
        mazeBitmap = Bitmap.createScaledBitmap(mazeBitmap, 1089, 1188, true)
    }

    private val mazeMap = arrayOf(
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1),
        intArrayOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    private val gridCellWidth = 33f // Adjust based on actual scaling
    private val gridCellHeight = 34f // Adjust based on actual scaling

    private var pacMan = PacMan(550f, 665f, 3f, 25f,
        mazeMap, gridCellWidth, gridCellHeight)

    init {
        paint.color = Color.YELLOW
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
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun update() {
        // Move Pac-Man only if the direction has been set (after touch)
        if (pacMan.isDirectionSet) {
            // Ensure collision detection happens before moving
            pacMan.movePacMan()
        }
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

        // Draw Pac-Man
        paint.color = Color.YELLOW
        canvas.drawCircle(pacMan.x, pacMan.y, 30f, paint)  // Pac-Man at its current position
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
        } else if (direction == LEFT){
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



