package com.etoitau.giftessera.domain

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageButton

import com.etoitau.giftessera.MainActivity
import com.etoitau.giftessera.R
import com.etoitau.giftessera.helpers.filmStripToByteArray
import com.etoitau.giftessera.helpers.scaleFilmStrip
import com.etoitau.giftessera.helpers.toFilmstrip
import java.util.*
import kotlin.concurrent.fixedRateTimer

import kotlin.math.max
import kotlin.math.min



class DrawSession constructor(val context: Context, val drawingBoard: DrawingBoard){
    var filmStrip = mutableListOf<Bitmap>()
    var filmIndex = 0
    val deleteAlertDialog: AlertDialog
    val clearAlertDialog: AlertDialog

    var saveId: Int? = 0
    var saveName: String? = null

    var animationRunning = false
    private val DELAY = 250L // 1/4 sec
    private val FRAME_RATE = 83L // 12 fps
    var animationTimer: Timer? = null

    init {
        filmStrip.add(drawingBoard.getBitmap())
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
                    view.setImageDrawable(context.resources.getDrawable(android.R.drawable.ic_media_play))
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

    private fun clearSession() {
        drawingBoard.clearBoard()
        filmIndex = 0
        filmStrip.clear()
        filmStrip.add(drawingBoard.getBitmap())
        saveName = null
        if (context is MainActivity)
            context.updateTitle()
    }

    fun loadDataBaseFile(databaseFile: DatabaseFile) {
        saveId = databaseFile.id
        saveName = databaseFile.name
        filmStrip = toFilmstrip(databaseFile.blob)
        drawingBoard.setBitmap(filmStrip[0])
        filmIndex = 0
    }

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