package com.etoitau.giftessera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.etoitau.giftessera.domain.*
import com.etoitau.giftessera.helpers.DBHelper
import com.etoitau.giftessera.helpers.FilesAdapter
import com.etoitau.giftessera.helpers.toByte
import kotlinx.android.synthetic.main.about_display.*
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Main Activity
 * manages most of UI
 */
class MainActivity : AppCompatActivity() {
    lateinit var drawSession: DrawSession   // object managing current animation project
    var isPeeking = false                   // currently in peek view mode
    lateinit var toast: Toast               // reusable toast object

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize toast for use by showToast
        toast = Toast.makeText(this, "", Toast.LENGTH_LONG)

        // initialize drawingBoard with display metrics
        // then set up drawSession with created drawingBoard
        drawingBoard.post(Runnable {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            Log.i("width", drawingBoard.width.toString())
            Log.i("height", drawingBoard.height.toString())
            Log.i("dpi", metrics.densityDpi.toString())
            drawingBoard.init(drawingBoard.width, drawingBoard.height, metrics.densityDpi)
            drawSession = DrawSession(this@MainActivity, drawingBoard)
        })

        // add listener for peek button
        setPeekListener()

        // put current app version in About display
        versionTextView.text = BuildConfig.VERSION_NAME
    }

    /**
     * When user clicks a color button, set that as paint color in the DrawingBoard
     * and add border to indicate it is selected
     */
    fun colorClick(view: View) {
        if (view is PaletteButton) {
            Log.i("clicked", view.color.toString())
            drawingBoard.color = view.color
            view.setSelected()
        }
    }

    fun clickNext(view: View) {
        drawSession.getNext()
        updateTitle()
    }

    fun clickPrev(view: View) {
        drawSession.getPrev()
        updateTitle()
    }

    fun clickDelete(view: View) {
        drawSession.confirmDelete()
        updateTitle()
    }

    fun clickAdd(view: View) {
        drawSession.addInsertFrame()
        updateTitle()
        showToast(String.format(getString(R.string.frame_added_after), drawSession.filmIndex))
    }

    /**
     * Show a slightly grayed-out peek at the previous frame as long as peek button
     * is depressed. Quick reference to the previous frame is important when composing animation
     */
    private fun setPeekListener() {
        peekButton.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                Log.i("ontouch", "called")
                if (event.action == MotionEvent.ACTION_DOWN && drawSession.filmIndex > 0) {
                    isPeeking = true
                    drawSession.getPrev()
                    drawingBoard.alpha = 0.7f
                    updateTitle()
                } else if (event.action == MotionEvent.ACTION_UP && isPeeking) {
                    Log.i("actionup", "called")
                    drawingBoard.alpha = 1.0f
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
    private fun updateTitle(number: Int) {
        val frame = resources.getString(R.string.frame)
        val sessionName = drawSession.saveName
        var showName = sessionName
        if (sessionName == null) {
            // if there's no save name for this animation, show app name
            showName = resources.getString(R.string.app_name)
        } else if (sessionName.length > 7) {
            // if save name is long, truncate it
            showName = sessionName.slice(IntRange(0, 6)) + "..."
        }
        title = "$showName - $frame $number"
    }

    /**
     * When user presses play, either start or stop animation
     * note we disable editing during animation through DrawingBoard.editable and setButtonsActivated
     */
    fun clickPlay(view: View) {
        if (view is ImageButton) {
            // if already running, stop
            if (drawSession.animationRunning) {
                drawSession.animationTimer!!.cancel()
                drawSession.animationRunning = false
                drawingBoard.editable = true
                setButtonsActivated(true)
                playButton.setImageDrawable(ResourcesCompat
                    .getDrawable(resources, android.R.drawable.ic_media_play, null))
                updateTitle()
            } else {
                // start
                drawingBoard.editable = false
                setButtonsActivated(false)
                playButton.setImageDrawable(ResourcesCompat
                    .getDrawable(resources, android.R.drawable.ic_menu_close_clear_cancel, null))
                updateTitle(drawSession.filmStrip.size)
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
        val status = super.onOptionsItemSelected(item)
        // show appropriate confirmation
        when (item.itemId) {
            R.id.menuSaveGif -> saveToDB()
            R.id.menuSaveAs -> saveAsToDB()
            R.id.menuLoadGif -> loadFromDB()
            R.id.menuClearGif -> clearCurrentSession()
            R.id.menuExportGif -> exportGif()
            R.id.menuHelp -> showHelp()
            R.id.menuAbout -> showAbout()
            else -> {
                showToast(resources.getString(R.string.invalid_selection))
            }
        }
        return status
    }

    /**
     * modifies and shows a premade toast with provided message
     * @param message
     */
    fun showToast(message: String) {
        toast.setText(message)
        toast.show()
    }

    /**
     * "Save" menu item selected
     * If working on previously saved project, update database with current version
     * else treat as Save As and go to create new save file
     */
    fun saveToDB() {
        if (drawSession.saveName == null) {
            // if drawSession doesn't have project name, project is not in database yet
            saveAsToDB()
        } else {
            try {
                // new databaseFile
                val dbFile = DatabaseFile(drawSession.saveId,
                    drawSession.saveName!!,
                    toByte(drawSession.filmStrip)!!)
                // get database helper
                val dbHelper = DBHelper(this, null)
                // update file in database
                dbHelper.updateFile(dbFile)
                showToast(getString(R.string.saved_as) + " " + drawSession.saveName)
            } catch (e: Exception) {
                showToast(resources.getString(R.string.error_saving))
            }
        }
    }

    /**
     * For Save As, send to FilesActivity
     */
    fun saveAsToDB() {
        val intent = Intent(this, FilesActivity::class.java)
        intent.putExtra("mode", FilesAdapter.SAVING)
        intent.putExtra("file", toByte(drawSession.filmStrip))
        startActivityForResult(intent, 1)
    }

    /**
     * For Load, send to FilesActivity
     */
    fun loadFromDB() {
        val intent = Intent(this, FilesActivity::class.java)
        intent.putExtra("mode", FilesAdapter.LOADING)
        startActivityForResult(intent, 1)
    }

    /**
     * Either coming back from FilesActivity having saved or loaded a project
     * or coming back from picking file location and name for gif export
     */
    override fun onActivityResult (requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == 1 && data != null) {
            // if saved or loaded filmstrip from sqlite
            // unpack and send to drawSession
            val id = data.getIntExtra("id", 0)
            val name = data.getStringExtra("name")
            val byteArray = data.getByteArrayExtra("file")
            if (name == null || byteArray == null) {
                showToast(getString(R.string.error_saving))
            } else {
                drawSession.loadDataBaseFile(DatabaseFile(id, name, byteArray))
                updateTitle(1)
            }
        } else if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            // if saving gif to phone
            if (data != null && data.data != null) {
                // get uri and send to asynctask to save in background
                val uri: Uri = data.data!!
                val fName = drawSession.saveName
                // let user know save is starting in background
                showToast(String.format(getString(R.string.saving_to_gif), fName))
                // get and start asynctask
                val writeGifFile = WriteGifFile()
                writeGifFile.execute(uri)
            }
        }
    }

    // clear and start over
    fun clearCurrentSession() {
        drawSession.confirmClearSession()
    }

    /**
     * User selects Export Gif in menu
     * check:
     * - we have a name for the project
     * - there are no issues wit the file system
     * - we have required permissions
     * Then send them to pick file name/location
     */
    fun exportGif() {
        if (drawSession.saveName == null) {
            // check saved locally first (so it has name)
            showToast(getString(R.string.save_before_export))
            return
        } else if (!isReadyToSave()){
            // check file system is available
            showToast(getString(R.string.fs_not_available))
            return
        }

        // check/request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                5)
        }

        // use intent to let user pick save location
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/gif"
            putExtra(Intent.EXTRA_TITLE, drawSession.saveName)
        }

        startActivityForResult(intent, 3)
        // see activity result for next step
    }

    /**
     * Given Uri location of file user selected to save gif to
     * write data to that file in background and notify when done
     */
    inner class WriteGifFile : AsyncTask<Uri, Void, Int>() {
        override fun doInBackground(vararg uri: Uri?): Int {
            try {
                val fos = contentResolver.openOutputStream(uri[0]!!)
                fos!!.write(drawSession.getGif())
                fos.close()
            } catch (e: Exception) {
                e.printStackTrace()
                return 0
            }
            return 1
        }

        override fun onPostExecute(result: Int?) {
            val fName = drawSession.saveName
            if (result == 1) {
                showToast(String.format(getString(R.string.finished_saving_gif), fName))
            } else {
                showToast(String.format(getString(R.string.error_saving_gif), fName))
            }
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

    /**
     * When animation is running we don't want any of these buttons to be functional
     * This sets then activated or not according to isActivated
     */
    fun setButtonsActivated(isActivated: Boolean) {
        val buttons =
            mutableListOf<ImageButton>(prevButton, nextButton, deleteButton, addButton, peekButton)
        for (button in buttons) {
            button.isEnabled = isActivated
            button.isClickable = isActivated
        }
    }

    fun showHelp() {
        helpDisplay.visibility = View.VISIBLE
        title = getString(R.string.app_name) + " - " + getString(R.string.help)
    }

    // Hide help screen when user is done with it, called with an onClick
    fun dismissHelp(view: View) {
        helpDisplay.visibility = View.GONE
        updateTitle()
    }

    fun showAbout() {
        aboutDisplay.visibility = View.VISIBLE
        title = getString(R.string.app_name) + " - " + getString(R.string.about)
        authorTextView.movementMethod = LinkMovementMethod.getInstance()
        licenseTextView.movementMethod = LinkMovementMethod.getInstance()
    }
    fun dismissAbout(view: View) {
        aboutDisplay.visibility = View.GONE
        updateTitle()
    }
}
