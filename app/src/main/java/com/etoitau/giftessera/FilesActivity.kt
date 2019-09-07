package com.etoitau.giftessera

import android.content.Intent
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etoitau.giftessera.helpers.DBHelper
import com.etoitau.giftessera.domain.DatabaseFile
import com.etoitau.giftessera.helpers.EnterListener
import com.etoitau.giftessera.helpers.FilesAdapter
import kotlinx.android.synthetic.main.activity_files.*

/**
 * Here user can see all current saved projects
 * Can be opened in two modes:
 * SAVING - create new save file or save over existing
 * LOADING - load existing save file for editing/viewing
 */
class FilesActivity : AppCompatActivity() {
    // adapter for showing saved files in RecyclerView
    private lateinit var filesAdapter: FilesAdapter
    // list of found save files
    private val fileList = mutableListOf<DatabaseFile>()
    // RecyclerView for viewing files
    private lateinit var rvFiles: RecyclerView
    // FilesActivity mode, intent from MainActivity indicates what mode should be
    private var mode: Int = FilesAdapter.SAVING
    // file data as ByteArray provided by intent from MainActivity if appropriate
    private var toSave: ByteArray? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)

        // get mode from intent
        if (intent.extras == null) {
            finish()
        } else {
            mode = intent.extras!!.getInt("mode")
        }

        // if LOADING mode, don't need UI for new save, and title should update
        if (mode == FilesAdapter.LOADING) {
            newSaveTitle.visibility = View.GONE
            fileNameField.visibility = View.GONE
            saveButton.visibility = View.GONE
            pastSaveTitle.text = resources.getString(R.string.load_past_save)
        } else {
            // if SAVING get file to save from intent
            toSave = intent.extras!!.getByteArray("file")
        }

        // set up RecyclerView and add dividing lines
        rvFiles = findViewById(R.id.recycleFileView)
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        rvFiles.addItemDecoration(itemDecoration)
        rvFiles.layoutManager = LinearLayoutManager(this)

        // get files from database
        getFiles()

        // show them in RecyclerView
        inflateRecyclerView()

        // set enter key listener on file name
        if (mode == FilesAdapter.SAVING) {
            fileNameField.setOnKeyListener(EnterListener(this))
        }
    }

    /**
     * Retrieve all save files from database and store in list
     */
    private fun getFiles() {
        val dbHelper = DBHelper(this, null)
        val cursor: Cursor? = dbHelper.getFiles()
        if (cursor == null) {
            fileMessageView.text = resources.getString(R.string.no_files_found)
            fileMessageView.visibility = View.VISIBLE
        } else {
            try {
                // fill fileList with all files found in database
                fileList.clear()
                cursor.moveToFirst()
                var id = cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_ID))
                var name = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NAME))
                var blob = cursor.getBlob(cursor.getColumnIndex(DBHelper.COLUMN_FILE))
                fileList.add(DatabaseFile(id, name, blob))
                while (cursor.moveToNext()) {
                    id = cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_ID))
                    name = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NAME))
                    blob = cursor.getBlob(cursor.getColumnIndex(DBHelper.COLUMN_FILE))
                    fileList.add(DatabaseFile(id, name, blob))
                }
            } catch (e: CursorIndexOutOfBoundsException) {
                if (fileList.isEmpty()) {
                    fileMessageView.text = resources.getString(R.string.no_files_found)
                    fileMessageView.visibility = View.VISIBLE
                }
            } finally {
                cursor.close()
            }

        }
    }

    /**
     * Fill RecyclerView with found files
     */
    private fun inflateRecyclerView() {
        filesAdapter = FilesAdapter(this, fileList, mode)
        rvFiles.adapter = filesAdapter
    }

    /**
     * If user clicks save button
     * - check they entered a name
     * - check name isn't already used
     * then save to database
     */
    fun clickSave(view: View) {
        val enteredName: String? = fileNameField.text.toString()
        if (enteredName == null || enteredName.isEmpty()) {
            fileMessageView.text = resources.getString(R.string.missing_name)
            fileMessageView.visibility = View.VISIBLE
        } else if (fileList.any {x -> x.name == enteredName }) {
            fileMessageView.text = resources.getString(R.string.existing_name)
            fileMessageView.visibility = View.VISIBLE
        } else {
            newSaveToDB(enteredName, toSave!!)
        }
    }

    /**
     * New save to database, not overwriting existing
     * Note we get id for created database entry so we can update file later if desired
     */
    private fun newSaveToDB(name: String, byteArray: ByteArray) {
        val dbHelper = DBHelper(this, null)
        val databaseFile = DatabaseFile(null, name, byteArray)
        val id = dbHelper.addFile(databaseFile)
        returnToMainWithIntent(DatabaseFile(id, name, byteArray))
    }

    /**
     * If user clicks one of the delete Xs, confirm they want to delete file from database,
     * then delete
     */
    fun deleteFileAlert(databaseFile: DatabaseFile) {
        // delete save confirmation
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.alert_delete_save_title)
        val message: String = resources.getString(R.string.alert_delete_save_message) + " ${databaseFile.name}?"
        builder.setMessage(message)
        builder.setPositiveButton(R.string.yes_delete) { _, _ ->
            // use helper to delete from database
            val dbHelper = DBHelper(this, null)
            dbHelper.deleteFile(databaseFile)
            // update file list and RecyclerView
            val index = fileList.indexOf(databaseFile)
            fileList.removeAt(index)
            filesAdapter.notifyItemRemoved(index)
        }
        builder.setNegativeButton(R.string.no_never_mind) { _, _ ->
            // nothing
        }
        builder.create().show()
    }

    /**
     * If user clicks on an existing file in save mode,
     * confirm they want to overwrite that file,
     * then do so
     */
    fun overwriteFileAlert(databaseFile: DatabaseFile) {
        // overwrite save confirmation
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.alert_overwrite_save_title)
        val message: String = resources.getString(R.string.alert_overwrite_save_message) + " ${databaseFile.name}?"
        builder.setMessage(message)
        builder.setPositiveButton(R.string.yes_save_over) { _, _ ->
            // use helper to update file in database
            val dbHelper = DBHelper(this, null)
            dbHelper.updateFile(databaseFile)
            returnToMainWithIntent(databaseFile)
        }
        builder.setNegativeButton(R.string.no_never_mind) { _, _ ->
            // nothing
        }
        builder.create().show()
    }

    /**
     * if user clicks on one of the existing files in RecyclerView,
     * confirm they want to load it and lose current project,
     * then do so
     */
    fun loadFileAlert(databaseFile: DatabaseFile) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.alert_load_save_title)
        val message: String = String.format(resources.getString(R.string.alert_load_save_message, databaseFile.name))
        builder.setMessage(message)
        builder.setPositiveButton(R.string.yes_load) { _, _ ->
            returnToMainWithIntent(databaseFile)
        }
        builder.setNegativeButton(R.string.no_never_mind) { _, _ ->
            // nothing
        }
        builder.create().show()
    }

    /**
     * Send file back to MainActivity so it can update DrawSession
     * If from Save As, this is same info sent here + database id
     * If from Load, this is file info from database
     */
    private fun returnToMainWithIntent(databaseFile: DatabaseFile) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("id", databaseFile.id)
        intent.putExtra("name", databaseFile.name)
        intent.putExtra("file", databaseFile.blob)
        setResult(1, intent)
        finish()
    }
}
