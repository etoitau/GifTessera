package com.etoitau.giftessera.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.view.View
import com.etoitau.giftessera.R
import com.etoitau.giftessera.domain.DatabaseFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLite Database Helper Class
 * thanks to https://blog.mindorks.com/android-sqlite-database-in-kotlin
 */
class DBHelper(context: Context, factory: SQLiteDatabase.CursorFactory?):
    SQLiteOpenHelper(context,
        DATABASE_NAME, factory,
        DATABASE_VERSION
    ) {

    // table and column names for reference
    companion object {
        private const val DATABASE_VERSION = 3
        private const val DATABASE_NAME = "giftesseraFiles.db"
        private const val TABLE_NAME = "filmstrip"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "filename"
        const val COLUMN_FILE = "file"
    }

    // create table
    override fun onCreate(db: SQLiteDatabase) {
        val createProductsTable = ("CREATE TABLE " + TABLE_NAME +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_FILE + " BLOB " + ")")
        db.execSQL(createProductsTable)
    }

    // upgrade is just drop previous table and create new one
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // add row from DatabaseFile object
    // return primary key for later use in updating entry
    fun addFile(databaseFile: DatabaseFile): Int {
        val values = ContentValues()
        values.put(COLUMN_NAME, databaseFile.name)
        values.put(COLUMN_FILE, databaseFile.blob)
        val db = this.writableDatabase
        // note insert returns pk
        val id: Int = db.insert(TABLE_NAME, null, values).toInt()
        db.close()
        return id
    }

    // get table contents
    fun getFiles(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    // delete table entry by name
    // note we check on new file creation in FilesActivity that name does not already exist in database
    fun deleteFile(databaseFile: DatabaseFile) {
        val db = this.writableDatabase
        val selection = "$COLUMN_NAME LIKE ?"
        db.delete(TABLE_NAME, selection, arrayOf(databaseFile.name))
        db.close()
    }

    // update table entry by primary key
    fun updateFile(databaseFile: DatabaseFile) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_NAME, databaseFile.name)
        values.put(COLUMN_FILE, databaseFile.blob)
        db.update(TABLE_NAME, values, COLUMN_ID + "=${databaseFile.id}", null)
        db.close()
    }

    fun dbToFileList(): MutableList<DatabaseFile> {
        val cursor: Cursor? = getFiles()
        val fileList = mutableListOf<DatabaseFile>()
        if (cursor == null) {
            throw DBException("No files found")
        } else {
            try {
                // fill fileList with all files found in database
                cursor.moveToFirst()
                val idIdx = cursor.getColumnIndex(COLUMN_ID)
                val nameIdx = cursor.getColumnIndex(COLUMN_NAME)
                val fileIdx = cursor.getColumnIndex(COLUMN_FILE)
                if (idIdx == -1 || nameIdx == -1 || fileIdx == -1) {
                    // Won't happen, this is to satisfy compiler
                    throw Exception()
                }
                var id = cursor.getInt(idIdx)
                var name = cursor.getString(nameIdx)
                var blob = cursor.getBlob(fileIdx)
                fileList.add(DatabaseFile(id, name, blob))
                while (cursor.moveToNext()) {
                    id = cursor.getInt(idIdx)
                    name = cursor.getString(nameIdx)
                    blob = cursor.getBlob(fileIdx)
                    fileList.add(DatabaseFile(id, name, blob))
                }
            } catch (e: CursorIndexOutOfBoundsException) {
                if (fileList.isEmpty()) {
                    throw DBException("No files found", e)
                }
            } finally {
                cursor.close()
            }
        }
        return fileList;
    }

    fun dbToString(): String {
        val dbFileList = dbToFileList()
        return Json.encodeToString(dbFileList)
    }

    fun updateDbFromString(string: String) {
        val existingFileList = dbToFileList()
        val existingNameCount = HashMap<String, Int>()
        val nameToBlob = HashMap<String, ByteArray>()
        existingFileList.forEach {
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
            addFile(DatabaseFile(null, saveName, it.blob))
        }
    }
}

class DBException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {}
