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
import com.etoitau.giftessera.helpers.FilesAdapter
import kotlinx.android.synthetic.main.activity_files.*

class FilesActivity : AppCompatActivity() {
    lateinit var filesAdapter: FilesAdapter
    val fileList = mutableListOf<DatabaseFile>()
    lateinit var rvFiles: RecyclerView
    var mode: Int = FilesAdapter.SAVING
    var toSave: ByteArray? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files)


        // get mode from intent
        if (intent.extras == null) {
            finish()
        } else {
            mode = intent.extras!!.getInt("mode")
        }

        if (mode == FilesAdapter.LOADING) {
            newSaveTitle.visibility = View.GONE
            fileNameField.visibility = View.GONE
            saveButton.visibility = View.GONE
            pastSaveTitle.text = resources.getString(R.string.load_past_save)
        } else {
            toSave = intent.extras!!.getByteArray("file")
        }

        // set up recyclerview and add dividing lines
        rvFiles = findViewById(R.id.recycleFileView)
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        rvFiles.addItemDecoration(itemDecoration)
        rvFiles.setLayoutManager(LinearLayoutManager(this))

        getFiles()

        inflateRecyclerView()

    }

    /**
     * Retrieve all save files from database and store in list
     */
    fun getFiles() {
        val dbHelper = DBHelper(this, null)
        val cursor: Cursor? = dbHelper.getFiles()
        if (cursor == null) {
            fileMessageView.text = resources.getString(R.string.no_files_found)
            fileMessageView.visibility = View.VISIBLE
        } else {
            try {
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
     * Fill recyclerview with found files
     */
    fun inflateRecyclerView() {
        filesAdapter = FilesAdapter(this, fileList, mode)
        rvFiles.adapter = filesAdapter
    }

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

    fun newSaveToDB(name: String, byteArray: ByteArray) {
        val dbHelper = DBHelper(this, null)
        val databaseFile = DatabaseFile(null, name, byteArray)
        val id = dbHelper.addFile(databaseFile)
        returnToMainWithIntent(DatabaseFile(id, name, byteArray))
    }

    fun deleteFileAlert(databaseFile: DatabaseFile) {
        // delete save confirmation
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.alert_delete_save_title)
        val message: String = resources.getString(R.string.alert_delete_save_message) + " ${databaseFile.name}?"
        builder.setMessage(message)
        builder.setPositiveButton(R.string.yes_delete) {dialog, which ->
            val dbHelper = DBHelper(this, null)
            dbHelper.deleteFile(databaseFile)
            val index = fileList.indexOf(databaseFile)
            fileList.removeAt(index)
            filesAdapter.notifyItemRemoved(index)
        }
        builder.setNegativeButton(R.string.no_never_mind) {dialog, which ->
            // nothing
        }
        builder.create().show()
    }

    fun overwriteFileAlert(databaseFile: DatabaseFile) {
        // overwrite save confirmation
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.alert_overwrite_save_title)
        val message: String = resources.getString(R.string.alert_overwrite_save_message) + " ${databaseFile.name}?"
        builder.setMessage(message)
        builder.setPositiveButton(R.string.yes_save_over) {dialog, which ->
            val dbHelper = DBHelper(this, null)
            dbHelper.updateFile(databaseFile)
            returnToMainWithIntent(databaseFile)
        }
        builder.setNegativeButton(R.string.no_never_mind) {dialog, which ->
            // nothing
        }
        builder.create().show()
    }

    fun loadFileAlert(databaseFile: DatabaseFile) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.alert_load_save_title)
        val message: String = String.format(resources.getString(R.string.alert_load_save_message, databaseFile.name))
        builder.setMessage(message)
        builder.setPositiveButton(R.string.yes_load) {dialog, which ->
            returnToMainWithIntent(databaseFile)
        }
        builder.setNegativeButton(R.string.no_never_mind) {dialog, which ->
            // nothing
        }
        builder.create().show()
    }

    fun returnToMainWithIntent(databaseFile: DatabaseFile) {
        var intent: Intent = Intent(this, MainActivity::class.java)
        intent.putExtra("id", databaseFile.id)
        intent.putExtra("name", databaseFile.name)
        intent.putExtra("file", databaseFile.blob)
        setResult(1, intent)
        finish()
    }




}
