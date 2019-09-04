package com.etoitau.giftessera.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.etoitau.giftessera.domain.DatabaseFile

/**
 * SQLite Database Helper Class
 * thanks to https://blog.mindorks.com/android-sqlite-database-in-kotlin
 */
class DBHelper(context: Context, factory: SQLiteDatabase.CursorFactory?):
    SQLiteOpenHelper(context,
        DATABASE_NAME, factory,
        DATABASE_VERSION
    ) {

    companion object {
        private val DATABASE_VERSION = 3
        private val DATABASE_NAME = "giftesseraFiles.db"
        val TABLE_NAME = "filmstrip"
        val COLUMN_ID = "_id"
        val COLUMN_NAME = "filename"
        val COLUMN_FILE = "file"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_PRODUCTS_TABLE = ("CREATE TABLE " + TABLE_NAME +
                "(" + COLUMN_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_FILE + " BLOB " + ")")
        db.execSQL(CREATE_PRODUCTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

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

    fun getFiles(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    fun deleteFile(databaseFile: DatabaseFile) {
        val db = this.writableDatabase
        val selection = COLUMN_NAME + " LIKE ?"
        db.delete(TABLE_NAME, selection, arrayOf(databaseFile.name))
        db.close()
    }

    fun updateFile(databaseFile: DatabaseFile) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_NAME, databaseFile.name)
        values.put(COLUMN_FILE, databaseFile.blob)
        db.update(TABLE_NAME, values, COLUMN_ID + "=${databaseFile.id}", null)
        db.close()
    }
}