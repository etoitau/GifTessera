package com.etoitau.giftessera

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.etoitau.giftessera.databinding.ActivityFilesBinding
import com.etoitau.giftessera.helpers.DBHelper
import com.etoitau.giftessera.domain.DatabaseFile
import com.etoitau.giftessera.helpers.DBException
import com.etoitau.giftessera.helpers.EnterListener
import com.etoitau.giftessera.helpers.FilesAdapter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    // name of current project, if any
    private var name: String? = null
    // file data as ByteArray provided by intent from MainActivity if appropriate
    private var toSave: ByteArray? = null
    private lateinit var binding: ActivityFilesBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get mode from intent
        if (intent.extras == null) {
            finish()
        } else {
            mode = intent.extras!!.getInt("mode")
            name = intent.extras!!.getString("name")
        }

        // if LOADING mode, don't need UI for new save, and title should update
        if (mode == FilesAdapter.LOADING) {
            binding.newSaveTitle.visibility = View.GONE
            binding.fileNameField.visibility = View.GONE
            binding.saveButton.visibility = View.GONE
            binding.pastSaveTitle.text = resources.getString(R.string.load_past_save)
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
            binding.fileNameField.setOnKeyListener(EnterListener(this))
        }
    }

    private fun dbToFileList(): MutableList<DatabaseFile> {
        var foundFileList = mutableListOf<DatabaseFile>()
        try {
            val dbHelper = DBHelper(this, null)
            foundFileList = dbHelper.dbToFileList()
        } catch (e: DBException) {
            binding.fileMessageView.text = resources.getString(R.string.no_files_found)
            binding.fileMessageView.visibility = View.VISIBLE
        }
        return foundFileList
    }

    /**
     * Retrieve all save files from database and store in list
     */
    private fun getFiles() {
        fileList.clear()
        try {
            val dbHelper = DBHelper(this, null)
            fileList.addAll(dbHelper.dbToFileList())
        } catch (e: DBException) {
            binding.fileMessageView.text = resources.getString(R.string.no_files_found)
            binding.fileMessageView.visibility = View.VISIBLE
        }
    }

    fun updateDbFromString(string: String) {
        getFiles()
        val existingNameCount = HashMap<String, Int>()
        val nameToBlob = HashMap<String, ByteArray>()
        fileList.forEach {
            existingNameCount[it.name] = 1
            nameToBlob[it.name] = it.blob
        }
        val importedFileList = Json.decodeFromString<MutableList<DatabaseFile>>(string)
        importedFileList.forEach {
            var saveName: String = it.name
            if (existingNameCount.containsKey(it.name)) {
                if (it.blob.contentEquals(nameToBlob[it.name])) {
                    // This is the same file, don't add it again
                    return
                }
                saveName = saveName + " (" + existingNameCount[it.name] + ")"
                existingNameCount[it.name] = existingNameCount[it.name]!! + 1
            } else {
                existingNameCount[it.name] = 1
                nameToBlob[it.name] = it.blob
            }
            newSaveToDB(saveName, it.blob)
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
        val enteredName: String = binding.fileNameField.text.toString()
        if (enteredName.isEmpty()) {
            binding.fileMessageView.text = resources.getString(R.string.missing_name)
            binding.fileMessageView.visibility = View.VISIBLE
        } else if (fileList.any {x -> x.name == enteredName }) {
            binding.fileMessageView.text = resources.getString(R.string.existing_name)
            binding.fileMessageView.visibility = View.VISIBLE
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
        if (databaseFile.name == name) {
            // don't let user delete the current project,
            //   when they go back it would look like it's still there
            binding.fileMessageView.visibility = View.VISIBLE
            binding.fileMessageView.text = getString(R.string.file_in_use)
            return
        }
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
            // update drawing ui

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
     * @param databaseFile is existing file saved here
     */
    fun overwriteFileAlert(databaseFile: DatabaseFile) {
        // overwrite save confirmation
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.alert_overwrite_save_title)
        val message: String = resources.getString(R.string.alert_overwrite_save_message) + " ${databaseFile.name}?"
        builder.setMessage(message)
        builder.setPositiveButton(R.string.yes_save_over) { _, _ ->
            if (toSave == null) {
                binding.fileMessageView.text = getString(R.string.no_data)
            } else {
                val newDBFile = DatabaseFile(databaseFile.id, databaseFile.name, toSave!!)
                // use helper to update file in database
                val dbHelper = DBHelper(this, null)
                dbHelper.updateFile(newDBFile)
                returnToMainWithIntent(newDBFile)
            }
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
        val message: String = String.format(resources.getString(R.string.alert_load_save_message), databaseFile.name)
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
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
