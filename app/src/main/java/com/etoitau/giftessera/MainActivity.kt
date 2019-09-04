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
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.etoitau.giftessera.domain.*
import com.etoitau.giftessera.helpers.DBHelper
import com.etoitau.giftessera.helpers.FilesAdapter
import com.etoitau.giftessera.helpers.toByte
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    lateinit var drawSession: DrawSession
    var isPeeking = false
    lateinit var toast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toast = Toast.makeText(this, "", Toast.LENGTH_LONG)

        drawingBoard.post(Runnable {
            var metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)

            Log.i("width", drawingBoard.width.toString())
            Log.i("height", drawingBoard.height.toString())
            Log.i("dpi", metrics.densityDpi.toString())
            drawingBoard.init(drawingBoard.width, drawingBoard.height, metrics.densityDpi)
            drawSession = DrawSession(this@MainActivity, drawingBoard)
        })

        setPeekListener()
    }

    fun colorClick(view: View) {
        if (view is PalatteButton) {
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

    fun setPeekListener() {
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

    fun updateTitle() {
        updateTitle(drawSession.filmIndex + 1)
    }

    fun updateTitle(number: Int) {
        val frame = resources.getString(R.string.frame)
        var sessionName = drawSession.saveName
        var showName = sessionName
        if (sessionName == null) {
            showName = resources.getString(R.string.app_name)
//            setTitle("GifTessera - $frame $number")
        } else if (sessionName.length > 7) {
            showName = sessionName.slice(IntRange(0, 6)) + "..."
        }
        setTitle("$showName - $frame $number")
    }

    fun clickPlay(view: View) {
        if (view is ImageButton) {
            // if already running, stop
            if (drawSession.animationRunning) {
                drawSession.animationTimer!!.cancel()
                drawSession.animationRunning = false
                drawingBoard.editable = true
                setButtonsActivated(true)
                playButton.setImageDrawable(resources.getDrawable(android.R.drawable.ic_media_play))
                updateTitle()
            } else {
                // start
                drawingBoard.editable = false
                setButtonsActivated(false)
                playButton.setImageDrawable(resources.getDrawable(android.R.drawable.ic_menu_close_clear_cancel))
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
        var status = super.onOptionsItemSelected(item)
        // show appropriate confirmation
        when (item.itemId) {
            R.id.menuSaveGif -> saveToDB()
            R.id.menuSaveAs -> saveAsToDB()
            R.id.menuLoadGif -> loadFromDB()
            R.id.menuClearGif -> clearCurrentSession()
            R.id.menuExportGif -> exportGif()
            R.id.menuHelp -> helpDisplay.visibility = View.VISIBLE
            else -> {
                showToast(resources.getString(R.string.invalid_selection))
            }
        }
        return status
    }

    /**
     * makes and shows a toast with provided message
     * @param message
     */
    fun showToast(message: String) {
        toast.setText(message)
        toast.show()
    }

    fun saveToDB() {
        if (drawSession.saveName == null) {
            saveAsToDB()
        } else {
            try {
                var dbFile = DatabaseFile(drawSession.saveId, drawSession.saveName!!, toByte(
                    drawSession.filmStrip
                )!!)
                val dbHelper = DBHelper(this, null)
                dbHelper.updateFile(dbFile)
                showToast(getString(R.string.saved_as) + " " + drawSession.saveName)
            } catch (e: Exception) {
                showToast(resources.getString(R.string.error_saving))
            }
        }
    }

    fun saveAsToDB() {
        val intent = Intent(this, FilesActivity::class.java)
        intent.putExtra("mode", FilesAdapter.SAVING)
        intent.putExtra("file", toByte(drawSession.filmStrip))
        startActivityForResult(intent, 1)
    }

    fun loadFromDB() {
        val intent = Intent(this, FilesActivity::class.java)
        intent.putExtra("mode", FilesAdapter.LOADING)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult (requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 1 && resultCode == 1 && data != null) {
            // if saved or loaded filmstrip from sqlite
            val id = data.getIntExtra("id", 0)
            val name = data.getStringExtra("name")
            val byteArray = data.getByteArrayExtra("file")
            drawSession.loadDataBaseFile(DatabaseFile(id, name, byteArray))
            updateTitle(1)
        } else if (requestCode == 3 && resultCode == Activity.RESULT_OK) {
            // if saving gif to phone
            if (data != null && data.data != null) {
                // get uri and send to asynctask to save in background
                val uri: Uri = data.data!!
                val fName = drawSession.saveName
                // let user know save is starting in background
                showToast(String.format(getString(R.string.saving_to_gif), fName))
                var writeGifFile = WriteGifFile()
                writeGifFile.execute(uri)
            }
        }
    }

    // clear and start over
    fun clearCurrentSession() {
        drawSession.confirmClearSession()
    }


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
        // see activity result
    }

    inner class WriteGifFile : AsyncTask<Uri, Void, Int>() {
        override fun doInBackground(vararg uri: Uri?): Int {
            try {
                var fos = contentResolver.openOutputStream(uri[0]!!)
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
    fun isReadyToSave(): Boolean {
        val extStorageState: String = Environment.getExternalStorageState()
        return extStorageState.equals(Environment.MEDIA_MOUNTED) &&
                !extStorageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    fun setButtonsActivated(isActivated: Boolean) {
        val buttons =
            mutableListOf<ImageButton>(prevButton, nextButton, deleteButton, addButton, peekButton)
        for (button in buttons) {
            button.isEnabled = isActivated
            button.isClickable = isActivated
        }
    }

    fun dismissHelp(view: View) {
        helpDisplay.visibility = View.GONE
    }
}
