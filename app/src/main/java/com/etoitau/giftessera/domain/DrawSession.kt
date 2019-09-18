package com.etoitau.giftessera.domain

import android.app.AlertDialog
import android.graphics.Bitmap
import android.widget.ImageButton
import androidx.core.content.res.ResourcesCompat

import com.etoitau.giftessera.MainActivity
import com.etoitau.giftessera.R
import com.etoitau.giftessera.helpers.rotateFilmstrip
import com.etoitau.giftessera.helpers.toFilmstrip
import com.etoitau.giftessera.helpers.whiteBitmap
import java.util.*
import kotlin.concurrent.fixedRateTimer

import kotlin.math.max
import kotlin.math.min


/**
 * Object managing current animation project
 * Holds collection of bitmaps
 * navigates through it
 * tells drawingBoard what frame to display
 * runs animation
 * returns current animation as byte array
 */
class DrawSession constructor(private val mainActivity: MainActivity, private val drawingBoard: DrawingBoard){
    companion object {
        private const val DELAY = 250L          // delay between press play and animation start: 1/4 sec
        const val FRAME_RATE: Int = 12          // frames per second
        const val FRAME_DELAY = 1000L / FRAME_RATE  // delay between each frame in milliseconds
    }

    // current animation: list of bitmaps
    var filmStrip = mutableListOf<Bitmap>()
    // current index in that list
    var filmIndex = 0

    private val deleteAlertDialog: AlertDialog
    private val newProjectAlertDialog: AlertDialog

    // if this animation is named/saved, the database key and save name
    var saveId: Int? = 0
    var saveName: String? = null

    // animation parameters
    var animationRunning = false        // is it currently running
    var animationTimer: Timer? = null   // Timer object governing animation

    init {
        filmStrip.add(drawingBoard.getBitmap()) // will be plain white to start
        deleteAlertDialog = buildDeleteAlert()
        newProjectAlertDialog = buildNewProjectAlert()
    }

    /**
     * Scan forward in film strip
     */
    fun getNext() {
        if (filmIndex < filmStrip.size - 1) {
            filmIndex++
            drawingBoard.setBitmap(filmStrip[filmIndex])
        }
    }

    /**
     * scan backward in film strip
     */
    fun getPrev() {
        if (filmIndex > 0) {
            filmIndex--
            drawingBoard.setBitmap(filmStrip[filmIndex])
        }
    }

    /**
     * Insert new frame after current frame
     */
    fun addInsertFrame() {
        val insBitmap: Bitmap = Bitmap.createBitmap(filmStrip[filmIndex])
        filmIndex++
        filmStrip.add(filmIndex, insBitmap)
        drawingBoard.setBitmap(filmStrip[filmIndex])
    }

    /**
     * confirm delete frame
     */
    fun confirmDelete() {
        deleteAlertDialog.show()
    }

    /**
     * Delete current frame
     */
    private fun deleteFrame() {
        if (filmStrip.size == 1) {
            // if only one frame left, just replace it with all white
            filmStrip[0] = whiteBitmap(filmStrip[0].width, filmStrip[0].height)
        } else {
            // if more then 1, remove as requested
            filmStrip.removeAt(filmIndex)
        }
        // index should normally decrement, but make sure doesn't fall outside filmstrip range
        filmIndex = min(max(filmIndex - 1, 0), filmStrip.lastIndex)
        // update board and title
        drawingBoard.setBitmap(filmStrip[filmIndex])
        mainActivity.updateTitle()
    }

    private fun buildDeleteAlert(): AlertDialog {
        val builder = AlertDialog.Builder(mainActivity)
        builder.setTitle(R.string.alert_delete_frame_title)
        builder.setMessage(R.string.alert_delete_frame_message)
        builder.setPositiveButton(R.string.yes_delete) { _, _ ->
            deleteFrame()
        }
        builder.setNegativeButton(R.string.no_never_mind) { _, _ ->
            // nothing
        }
        return builder.create()
    }

    private fun buildNewProjectAlert(): AlertDialog {
        val builder = AlertDialog.Builder(mainActivity)
        builder.setTitle(R.string.alert_new_session_title)
        builder.setMessage(R.string.alert_new_session_message)
        builder.setPositiveButton(R.string.yes_new_proj) { _, _ ->
            newProject()
        }
        builder.setNegativeButton(R.string.no_never_mind) { _, _ ->
            // nothing
        }
        return builder.create()
    }


    fun runAnimation(view: ImageButton) {
        drawingBoard.setBitmap(filmStrip[filmIndex])
        animationTimer = fixedRateTimer(null, false, DELAY, FRAME_DELAY) {
            (mainActivity as MainActivity).runOnUiThread {
                if (filmIndex < filmStrip.lastIndex) {
                    // if not at end of animation yet
                    animationRunning = true
                    getNext()
                } else {
                    // if at end of animation
                    animationRunning = false
                    view.setImageDrawable(ResourcesCompat.getDrawable(
                        mainActivity.resources,
                        android.R.drawable.ic_media_play,
                        null))
                    drawingBoard.editable = true
                    // reactivate buttons
                    mainActivity.setButtonsActivated(true)

                    cancel()
                }
            }
        }
    }

    fun confirmNewProject() {
        newProjectAlertDialog.show()
    }

    // reset to starting point
    private fun newProject() {
        drawingBoard.clearBoard()
        filmIndex = 0
        filmStrip.clear()
        filmStrip.add(drawingBoard.getBitmap())
        saveName = null
        mainActivity.updateTitle()
    }

    // load a save file
    fun loadDataBaseFile(databaseFile: DatabaseFile) {
        // use FilmstripToByte helper function to get filmstrip (mutable list of Bitmap) from ByteArray
        var gotFilmstrip = toFilmstrip(databaseFile.blob)
        var filmstripToLoad = gotFilmstrip
        if (!drawingBoard.isPortrait) {
            filmstripToLoad = rotateFilmstrip(gotFilmstrip, true)
        }

        filmStrip = filmstripToLoad
        filmIndex = 0
        drawingBoard.layout(filmStrip[filmIndex])
        saveId = databaseFile.id
        saveName = databaseFile.name

    }

    fun loadSaveState(saveFilmstrip: MutableList<Bitmap>, frameNumber: Int, saveId: Int?, saveName: String?, isPortrait: Boolean) {
        this.saveId = saveId
        this.saveName = saveName
        filmStrip = saveFilmstrip
        filmIndex = frameNumber
        drawingBoard.setBitmap(filmStrip[filmIndex])
    }
}
