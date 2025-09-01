package com.etoitau.giftessera

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.etoitau.giftessera.databinding.AboutDisplayBinding
import com.etoitau.giftessera.databinding.ActivityMainBinding
import com.etoitau.giftessera.domain.BreadBox
import com.etoitau.giftessera.domain.ColorVal
import com.etoitau.giftessera.domain.DatabaseFile
import com.etoitau.giftessera.domain.DrawSession
import com.etoitau.giftessera.domain.PaletteButton
import com.etoitau.giftessera.domain.PaletteManager
import com.etoitau.giftessera.gifencoder.AnimatedGifEncoder
import com.etoitau.giftessera.helpers.DBHelper
import com.etoitau.giftessera.helpers.FilesAdapter
import com.etoitau.giftessera.helpers.rotateFilmstrip
import com.etoitau.giftessera.helpers.toByte
import com.etoitau.giftessera.helpers.toFilmstrip
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date


/**
 * Main Activity
 * manages most of UI
 */
class MainActivity : AppCompatActivity() {
    lateinit var drawSession: DrawSession   // object managing current animation project
    var isPeeking = false                   // currently in peek view mode
    private lateinit var paletteManager: PaletteManager // object for managing library of palettes
    private lateinit var breadBox: BreadBox
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var aboutBinding: AboutDisplayBinding
    private var savedInstanceState: Bundle? = null

    companion object {
        // recovering save state
        const val SAVE_STRIP = "filmstrip"
        const val SAVE_FRAME_NUMBER = "frameNumber"
        const val SAVE_ID = "id"
        const val SAVE_NAME = "name"
        const val SAVE_IS_PORTRAIT = "isPortrait"
        const val SAVE_CUSTOM_PALETTE = "customPalette"
        const val SAVE_DRAWING_PALETTE = "drawingPalette"
        const val SAVE_SELECTED_COLOR = "selectedColor"
        const val SAVE_PEN_SIZE = "penSize"

        // codes
        const val CODE_SAVE = 3
        const val CODE_LOAD = 5
        const val CODE_FILE = 7
        const val CODE_EXPORT = 9
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        aboutBinding = AboutDisplayBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        val drawingBoard = mainBinding.drawingBoard
        val bread = mainBinding.bread
        val versionTextView = aboutBinding.versionTextView
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // recover save state
        var recoveredIndex: Int? = null
        var recoveredFilmstrip: MutableList<Bitmap>? = null
        var recoveredFrame: Bitmap? = null
        var recoveredID: Int? = null
        var recoveredName: String? = null
        var wasPortrait = true
        var savedCustomPalette: String? = null
        var savedDrawingPalette: String? = null
        var savedSelectedColor: Int? = null
        var savedPenSize: Int? = null

        try {
            if (savedInstanceState != null) {
                recoveredID = savedInstanceState.getInt(SAVE_ID)
                recoveredName = savedInstanceState.getString(SAVE_NAME)
                wasPortrait = savedInstanceState.getBoolean(SAVE_IS_PORTRAIT)
                val savedStripString = savedInstanceState.getString(SAVE_STRIP)
                recoveredIndex = savedInstanceState.getInt(SAVE_FRAME_NUMBER)
                if (savedStripString != null) {
                    recoveredFilmstrip = toFilmstrip(savedStripString.toByteArray(StandardCharsets.ISO_8859_1))
                    recoveredFrame = recoveredFilmstrip[recoveredIndex]
                }
                savedCustomPalette = savedInstanceState.getString(SAVE_CUSTOM_PALETTE)
                savedDrawingPalette = savedInstanceState.getString(SAVE_DRAWING_PALETTE)
                savedSelectedColor = savedInstanceState.getInt(SAVE_SELECTED_COLOR)
                savedPenSize = savedInstanceState.getInt(SAVE_PEN_SIZE)
            }
        } catch (e: Exception) {
            Log.i("onCreate","Error getting saved instance state")
            e.printStackTrace()
        }

        // get an object for managing the notification textbox (DIY Toast)
        breadBox = BreadBox(bread)

        // initialize drawingBoard with display metrics
        // while working with as-laid-out dimensions, determine if recovered filmstrip needs to be rotated
        // then set up drawSession with created drawingBoard and processed recovered data if exists
        drawingBoard.post(Runnable {
            val metrics = DisplayMetrics()

            val isPortrait = drawingBoard.width < drawingBoard.height

            windowManager.defaultDisplay.getMetrics(metrics)

            var newFilmstrip: MutableList<Bitmap>? = recoveredFilmstrip
            var startFrame: Bitmap? = null

            if (recoveredFilmstrip != null && isPortrait && !wasPortrait && recoveredFrame != null) {
                // if now portrait, but was landscape
                newFilmstrip = rotateFilmstrip(recoveredFilmstrip, false)
                startFrame = newFilmstrip[recoveredIndex!!]
            } else if (recoveredFilmstrip != null && !isPortrait && wasPortrait && recoveredFrame != null) {
                // if now landscape, but was portrait
                newFilmstrip = rotateFilmstrip(recoveredFilmstrip, true)
                startFrame = newFilmstrip[recoveredIndex!!]
            }

            drawingBoard.init(drawingBoard.width, drawingBoard.height, metrics.densityDpi, startFrame)

            drawSession = DrawSession(this@MainActivity, drawingBoard)

            if (newFilmstrip != null && recoveredIndex != null) {
                drawSession.loadSaveState(newFilmstrip, recoveredIndex, recoveredID, recoveredName)
                this.updateTitle()
            }

            if (savedPenSize != null) {
                drawingBoard.penSize = savedPenSize
                mainBinding.penSizeNumber.text = savedPenSize.toString()
            }

            if (savedCustomPalette != null) {
                paletteManager.loadCustomPalette(savedCustomPalette)
            }
            if (savedDrawingPalette != null) {
                paletteManager.loadDrawingPalette(savedDrawingPalette)
            }
            if (savedSelectedColor != null) {
                drawingBoard.color = paletteManager.loadSelectedColorIndex(savedSelectedColor)
            }
        })

        // add listener for peek button
        setPeekListener()

        // put current app version in About display
        versionTextView.text = BuildConfig.VERSION_NAME

        // create PaletteManager
        paletteManager = PaletteManager()
        paletteManager.initialize(this)
    }

    /**
     * Save drawSession info so not lost on screen rotate or other event
     */
    override fun onSaveInstanceState(outState: Bundle) {
        try{
            outState.run {
                putString(SAVE_STRIP, String(toByte(drawSession.filmStrip)!!, StandardCharsets.ISO_8859_1))
                putInt(SAVE_FRAME_NUMBER, drawSession.filmIndex)
                putBoolean(SAVE_IS_PORTRAIT, mainBinding.drawingBoard.isPortrait)
                if (drawSession.saveId != null && drawSession.saveName != null) {
                    putInt(SAVE_ID, drawSession.saveId!!)
                    putString(SAVE_NAME, drawSession.saveName)
                }
                putString(SAVE_CUSTOM_PALETTE, paletteManager.customPaletteString())
                putString(SAVE_DRAWING_PALETTE, paletteManager.drawingPaletteString())
                putInt(SAVE_SELECTED_COLOR, paletteManager.getSelectedColorIndex())
                putInt(SAVE_PEN_SIZE, mainBinding.drawingBoard.penSize)
            }
        } catch (e: Exception) {
            Log.i("onSaveInstanceState", "saving save state failed")
        }

        super.onSaveInstanceState(outState)
    }

    /**
     * When user clicks a colorVal button, set that as paint colorVal in the DrawingBoard
     * and add border to indicate it is selected
     */
    fun colorClick(view: View) {
        // if click color, pan mode is no longer desired
        disablePan()

        if (view is PaletteButton) {
            mainBinding.drawingBoard.color = view.colorVal
            view.setSelected()
        }

        // dismiss palette selection if active, they changed their mind
        mainBinding.colorLibraryView.root.visibility = View.GONE
    }

    fun clickNext(view: View) {
        drawSession.getNext()
        updateTitle()
    }

    fun clickPrev(view: View) {
        drawSession.getPrev()
        updateTitle()
    }

    /**
     * button to engage pan edit mode
     * set drawing board to pan mode and shade button to indicate mode is active
     */
    fun clickPan(view: View) {
        if (! mainBinding.drawingBoard.panMode) {
            mainBinding.panButton.setColorFilter(ColorVal.GRAY219.value, PorterDuff.Mode.DARKEN)
            mainBinding.drawingBoard.panMode = true
        } else {
            disablePan()
        }
    }

    fun clickPenSize(view: View) {
        if (view !is Button) {
            return
        }
        val currentSize = view.text.toString().toInt()
        mainBinding.drawingBoard.penSize = (currentSize + 2) % 10
        view.text = mainBinding.drawingBoard.penSize.toString()
    }

    fun clickDelete(view: View) {
        drawSession.confirmDelete()
        updateTitle()
    }

    fun clickAdd(view: View) {
        drawSession.addInsertFrame()
        updateTitle()
        breadBox.setMessage(String.format(getString(R.string.frame_added_after), drawSession.filmIndex))
            .showFor(BreadBox.MEDIUM)
    }

    /**
     * Show a slightly grayed-out peek at the previous frame as long as peek button
     * is depressed. Quick reference to the previous frame is important when composing animation
     */
    private fun setPeekListener() {
        mainBinding.peekButton.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN && drawSession.filmIndex > 0) {
                    isPeeking = true
                    drawSession.getPrev()
                    mainBinding.drawingBoard.alpha = 0.7f
                    updateTitle()
                } else if (event.action == MotionEvent.ACTION_UP && isPeeking) {
                    mainBinding.drawingBoard.alpha = 1.0f
                    drawSession.getNext()
                    updateTitle()
                    isPeeking = false
                }
                return true
            }
        })
    }

    /**
     * Update title bar with current frame
     */
    fun updateTitle() {
        updateTitle(drawSession.filmIndex + 1)
    }

    /**
     * Update title bar with a specific frame
     * Will show current animation's save name if exists
     */
    fun updateTitle(number: Int) {
        val frame = resources.getString(R.string.frame)
        val sessionName = drawSession.saveName
        var showName = sessionName
        if (sessionName == null) {
            // if there's no save name for this animation, show app name
            showName = resources.getString(R.string.app_name)
        } else if (sessionName.length > 10) {
            // if save name is long, truncate it
            showName = sessionName.slice(IntRange(0, 9)) + "..."
        }
        title = "$showName - $frame $number"
    }

    /**
     * When user presses play, either start or stop animation
     * note we disable editing during animation through DrawingBoard.editable and setButtonsActivated
     */
    fun clickPlay(view: View) {
        if (view is ImageButton) {
            if (drawSession.animationRunning) {
                // if already running, stop
                // get rid of screen
                mainBinding.screenView.visibility = View.GONE
                // stop animation
                drawSession.animationTimer!!.cancel()
                drawSession.animationRunning = false
                // reset button and title
                mainBinding.playButton.setImageDrawable(ResourcesCompat
                    .getDrawable(resources, android.R.drawable.ic_media_play, null))
                updateTitle()
            } else {
                // start
                // put up transparent screen over UI to catch any click and interpret as stop
                mainBinding.screenView.visibility = View.VISIBLE
                mainBinding.screenView.setOnClickListener { clickPlay(view) }
                // change button to make clear ani is running and click will stop
                mainBinding.playButton.setImageDrawable(ResourcesCompat
                    .getDrawable(resources, android.R.drawable.ic_menu_close_clear_cancel, null))
                // set frame in title to last frame
                updateTitle(drawSession.filmStrip.size)
                // go to start then start animation
                drawSession.filmIndex = 0
                drawSession.runAnimation(view)
            }
        }
    }

    // inflate menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_file, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // when menu item is clicked on
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // stop animation if it's running
        if (drawSession.animationRunning) { clickPlay(mainBinding.playButton) }
      
        // if going into menu, we can can say they're done panning
        disablePan()

        val status = super.onOptionsItemSelected(item)
        // show appropriate confirmation
        when (item.itemId) {
            R.id.menuNewProj -> clearCurrentSession()
            R.id.menuSaveGif -> saveToDB()
            R.id.menuSaveAs -> saveAsToDB()
            R.id.menuLoadGif -> loadFromDB()
            R.id.menuExportGif -> exportGif()
            R.id.menuHelp -> showHelp()
            R.id.menuAbout -> showAbout()
            else -> {
                breadBox.setMessage(resources.getString(R.string.invalid_selection)).showFor(BreadBox.MEDIUM)
            }
        }
        return status
    }

    /**
     * "Save" menu item selected
     * If working on previously saved project, update database with current version
     * else treat as Save As and go to create new save file
     */
    private fun saveToDB() {
        if (drawSession.saveName == null) {
            // if drawSession doesn't have project name, project is not in database yet
            saveAsToDB()
        } else {
            try {
                var copyToSave = drawSession.filmStrip
                // always save to db in portrait
                if (!mainBinding.drawingBoard.isPortrait) {
                    copyToSave = rotateFilmstrip(drawSession.filmStrip, false)
                }
                // new databaseFile
                val dbFile = DatabaseFile(drawSession.saveId,
                    drawSession.saveName!!,
                    toByte(copyToSave)!!)
                // get database helper
                val dbHelper = DBHelper(this, null)
                // update file in database
                dbHelper.updateFile(dbFile)
                breadBox.setMessage(getString(R.string.saved_as) + " " + drawSession.saveName)
                    .showFor(BreadBox.LONG)
            } catch (e: Exception) {
                breadBox.setMessage(resources.getString(R.string.error_saving)).showFor(BreadBox.LONG)
            }
        }
    }

    /**
     * For Save As, send to FilesActivity
     */
    private fun saveAsToDB() {
        var copyToSave = drawSession.filmStrip
        // always save to db in portrait
        if (!mainBinding.drawingBoard.isPortrait) {
            copyToSave = rotateFilmstrip(drawSession.filmStrip, false)
        }
        val intent = Intent(this, FilesActivity::class.java)
        intent.putExtra("mode", FilesAdapter.SAVING)
        intent.putExtra("name", drawSession.saveName)
        intent.putExtra("file", toByte(copyToSave))
        startActivityForResult(intent, CODE_SAVE)
    }

    /**
     * For Load, send to FilesActivity
     */
    private fun loadFromDB() {
        val intent = Intent(this, FilesActivity::class.java)
        intent.putExtra("mode", FilesAdapter.LOADING)
        intent.putExtra("name", drawSession.saveName)
        startActivityForResult(intent, CODE_LOAD)
    }

    /**
     * Either coming back from FilesActivity having saved or loaded a project,
     * or coming back from picking file location and name for gif export,
     * or coming back from picking file location for db export
     */
    override fun onActivityResult (requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == CODE_SAVE || requestCode == CODE_LOAD) && resultCode == RESULT_OK && data != null) {
            // if saved or loaded filmstrip from SQLite
            // unpack and send to drawSession
            val id = data.getIntExtra("id", 0)
            val name = data.getStringExtra("name")
            val byteArray = data.getByteArrayExtra("file")
            if (name == null || byteArray == null) {
                breadBox.setMessage(getString(R.string.error_saving)).showFor(BreadBox.LONG)
            } else {
                drawSession.loadDataBaseFile(DatabaseFile(id, name, byteArray))
                updateTitle(1)
            }
        } else if (requestCode == CODE_FILE && resultCode == RESULT_OK) {
            // if saving gif to phone
            if (data != null && data.data != null) {
                // get uri and send to AsyncTask to save in background
                val uri: Uri = data.data!!
                // get and start AsyncTask
                val writeGifFile = WriteGifFile()
                writeGifFile.execute(uri)
            }
        } else if (requestCode == CODE_EXPORT && resultCode == RESULT_OK) {
            if (data != null && data.data != null) {
                val uri: Uri = data.data!!
                try {
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val textToSave = dbToString()
                        outputStream.write(textToSave.toByteArray())
                        outputStream.close()
                        // File saved successfully
                    }
                } catch (e: IOException) {
                    // Handle error
                }
            }
        }
    }

    // clear and start over
    private fun clearCurrentSession() {
        drawSession.confirmNewProject()
    }

    /**
     * User selects Export Gif in menu
     * check:
     * - we have a name for the project
     * - there are no issues with the file system
     * Then send them to pick file name/location
     */
    private fun exportGif() {
        if (drawSession.saveName == null) {
            // check saved locally first (so it has name)
            breadBox.setMessage(getString(R.string.save_before_export)).showFor(BreadBox.LONG)
            return
        } else if (!isReadyToSave()){
            // check file system is available
            breadBox.setMessage(getString(R.string.fs_not_available)).showFor(BreadBox.LONG)
            return
        }

        // use intent to let user pick save location
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/gif"
            putExtra(Intent.EXTRA_TITLE, drawSession.saveName)
        }

        startActivityForResult(intent, CODE_FILE)
        // see activity result for next step
    }

    private fun exportSaves() {
        if (!isReadyToSave()){
            // check file system is available
            breadBox.setMessage(getString(R.string.fs_not_available)).showFor(BreadBox.LONG)
            return
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        // use intent to let user pick save location
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "export_$timeStamp.txt")
        }

        startActivityForResult(intent, CODE_EXPORT)
        // see activity result for next step todo kyle

    }

    /**
     * Given Uri location of file user selected to save gif to
     * write data to that file in background and notify progress
     */
    @SuppressLint("StaticFieldLeak")
    inner class WriteGifFile : AsyncTask<Uri, Void, Int>() {
        // save name here so user can work on something else
        private val fName = drawSession.saveName

        override fun doInBackground(vararg uri: Uri?): Int {
            // get deep copy of filmstrip so they can keep working
            val copyStrip = mutableListOf<Bitmap>()
            for (frame: Bitmap in drawSession.filmStrip)
                copyStrip.add(frame.copy(frame.config, true))

            // let user know save is starting in background
            runOnUiThread(
                BreadBox.RunBread(breadBox,
                    String.format(getString(R.string.saving_to_gif), fName),
                    BreadBox.MEDIUM))

            // ByteArray to put gif in
            val gifByteArray: ByteArray

            // generate gif
            try {
                val bos = ByteArrayOutputStream()
                val e = AnimatedGifEncoder()
                e.start(bos)
                // configure gif
                e.setDelay(DrawSession.FRAME_DELAY.toInt())
                e.setRepeat(0) // loop indefinitely
                // add each frame scaled up to display size
                val nFrames = copyStrip.size
                for (i in 0 until nFrames) {
                    // notify progress
                    runOnUiThread(BreadBox.RunBread(
                        breadBox,
                        String.format(getString(R.string.processing_frame), i + 1, nFrames),
                        BreadBox.SHORT))
                    e.addFrame(Bitmap.createScaledBitmap(copyStrip[i],
                        copyStrip[i].width * mainBinding.drawingBoard.xScale,
                        copyStrip[i].height * mainBinding.drawingBoard.yScale, false))
                }
                e.finish()
                gifByteArray = bos.toByteArray()
            } catch (e: Exception) {
                e.printStackTrace()
                return 0
            }

            // write gif to file
            try {
                val fos = contentResolver.openOutputStream(uri[0]!!)
                fos!!.write(gifByteArray)
                fos.close()
            } catch (e: Exception) {
                e.printStackTrace()
                return 0
            }
            return 1
        }

        // let user know save is complete
        override fun onPostExecute(result: Int?) {
            var msg: String
            if (result == 1) {
                msg = String.format(getString(R.string.finished_saving_gif), fName)
                Log.i("Async finished", "gif saved")
            } else {
                msg = String.format(getString(R.string.error_saving_gif), fName)
                Log.i("Async finished", "error")
            }
            runOnUiThread(BreadBox.RunBread(breadBox, msg, BreadBox.LONG))
        }
    }

    /**
     * Check external storage is mounted and not read-only
     */
    private fun isReadyToSave(): Boolean {
        val extStorageState: String = Environment.getExternalStorageState()
        return extStorageState.equals(Environment.MEDIA_MOUNTED) &&
                !extStorageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    private fun showHelp() {
        mainBinding.helpDisplay.root.visibility = View.VISIBLE
        title = getString(R.string.app_name) + " - " + getString(R.string.help)
    }

    // Hide help screen when user is done with it, called with an onClick
    fun dismissHelp(view: View) {
        mainBinding.helpDisplay.root.visibility = View.GONE
        updateTitle()
    }

    private fun showAbout() {
        mainBinding.aboutDisplay.root.visibility = View.VISIBLE
        title = getString(R.string.app_name) + " - " + getString(R.string.about)
        aboutBinding.authorTextView.movementMethod = LinkMovementMethod.getInstance()
        aboutBinding.licenseTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    fun dismissAbout(view: View) {
        mainBinding.aboutDisplay.root.visibility = View.GONE
        updateTitle()
    }

    // Toggle visibility of color library
    fun showColorLibraryClick(view: View) {
        showColorLibrary(mainBinding.colorLibraryView.root.visibility == View.GONE)
    }

    fun showColorLibrary(show: Boolean) {
        disablePan()
        mainBinding.colorLibraryView.root.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * When user picks color from library:
     * dismiss library, tell palette manager to put this palette in drawing row,
     * set drawing color in DrawingBoard
     */
    fun libraryClick(view: View) {
        showColorLibrary(false)
        if (view is PaletteButton) {
            paletteManager.onLibraryClick(view)
            mainBinding.drawingBoard.color = view.colorVal
        }
    }

    /**
     * function to ensure pan mode is disabled (putting us back in drawing mode)
     */
    private fun disablePan() {
        mainBinding.drawingBoard.panMode = false
        mainBinding.panButton.colorFilter = null
    }
}
