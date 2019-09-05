package com.etoitau.giftessera.domain

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageButton
import androidx.core.content.res.ResourcesCompat

import com.etoitau.giftessera.MainActivity
import com.etoitau.giftessera.R
import com.etoitau.giftessera.helpers.filmStripToByteArray
import com.etoitau.giftessera.helpers.scaleFilmStrip
import com.etoitau.giftessera.helpers.toFilmstrip
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.concurrent.fixedRateTimer

import kotlin.math.max
import kotlin.math.min


/**
 * Object managing current animation project
 * Holds collection of bitmaps
 * navigates through it
 * tells drawingboard what frame to display
 * runs animation
 * returns current animation as byte array
 */
class DrawSession constructor(val context: Context, val drawingBoard: DrawingBoard){
    // current animation: list of bitmaps
    var filmStrip = mutableListOf<Bitmap>()
    // current index in that list
    var filmIndex = 0

    val deleteAlertDialog: AlertDialog
    val clearAlertDialog: AlertDialog

    // if this animation is named/saved, the database key and save name
    var saveId: Int? = 0
    var saveName: String? = null

    // animation parameters
    var animationRunning = false        // is it currently running
    private val DELAY = 250L            // delay between press play and animation start: 1/4 sec
    private val FRAME_RATE = 83L        // delay between each frame: 12 fps
    var animationTimer: Timer? = null   // Timer object governing animation

    init {
        filmStrip.add(drawingBoard.getBitmap()) // will be plain white to start
        deleteAlertDialog = buildDeleteAlert()
        clearAlertDialog = buildClearAlert()
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
    fun deleteFrame() {
        if (filmStrip.size == 1) {
            drawingBoard.clearBoard()
        } else {
            filmStrip.removeAt(filmIndex)
            filmIndex = min(max(filmIndex - 1, 0), filmStrip.lastIndex)
            drawingBoard.setBitmap(filmStrip[filmIndex])
        }
    }

    fun buildDeleteAlert(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.alert_delete_frame_title)
        builder.setMessage(R.string.alert_delete_frame_message)
        builder.setPositiveButton(R.string.yes_delete) {dialog, which ->
            deleteFrame()
        }
        builder.setNegativeButton(R.string.no_never_mind) {dialog, which ->
            // nothing
        }
        return builder.create()
    }

    fun buildClearAlert(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.alert_clear_session_title)
        builder.setMessage(R.string.alert_clear_session_message)
        builder.setPositiveButton(R.string.yes_delete) {dialog, which ->
            clearSession()
        }
        builder.setNegativeButton(R.string.no_never_mind) {dialog, which ->
            // nothing
        }
        return builder.create()
    }


    fun runAnimation(view: ImageButton) {
        drawingBoard.setBitmap(filmStrip[filmIndex])
        animationTimer = fixedRateTimer(null, false, DELAY, FRAME_RATE) {
            (context as MainActivity).runOnUiThread {
                if (filmIndex < filmStrip.lastIndex) {
                    // if not at end of animation yet
                    animationRunning = true
                    getNext()
                } else {
                    // if at end of animation
                    animationRunning = false
                    view.setImageDrawable(ResourcesCompat.getDrawable(
                        context.resources,
                        android.R.drawable.ic_media_play,
                        null))
                    drawingBoard.editable = true
                    // reactivate buttons
                    context.setButtonsActivated(true)

                    cancel()
                }
            }
        }
    }

    fun confirmClearSession() {
        clearAlertDialog.show()
    }

    // reset to starting point
    private fun clearSession() {
        drawingBoard.clearBoard()
        filmIndex = 0
        filmStrip.clear()
        filmStrip.add(drawingBoard.getBitmap())
        saveName = null
        if (context is MainActivity)
            context.updateTitle()
    }

    // load a save file
    fun loadDataBaseFile(databaseFile: DatabaseFile) {
        saveId = databaseFile.id
        saveName = databaseFile.name
        // use FilmstripToByte helper function to get filmstrip (mutable list of Bitmap) from ByteArray
        filmStrip = toFilmstrip(databaseFile.blob)
        drawingBoard.setBitmap(filmStrip[0])
        filmIndex = 0
    }

    // return ByteArray version of project for export to gif using GifHelper methods
    fun getGif(): ByteArray {
        Log.i("getGif", "called")
        val scaledFilmStrip = scaleFilmStrip(
            filmStrip,
            drawingBoard.xScale,
            drawingBoard.yScale
        )
        return filmStripToByteArray(
            scaledFilmStrip,
            FRAME_RATE.toInt(),
            true
        )
    }
}