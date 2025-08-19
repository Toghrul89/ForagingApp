package com.example.foragingapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "foraging.db"
        private const val DB_VERSION = 1
        private const val TABLE_LOGS = "logs"
        private const val COL_ID = "id"
        private const val COL_NAME = "name"
        private const val COL_LOCATION = "location"
        private const val COL_IMAGE_PATH = "image_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_LOGS ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_NAME TEXT, $COL_LOCATION TEXT, $COL_IMAGE_PATH TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    fun insertLog(name: String, location: String, imagePath: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_LOCATION, location)
            put(COL_IMAGE_PATH, imagePath)
        }
        return db.insert(TABLE_LOGS, null, values)
    }

    fun getAllLogs(): List<Triple<String, String, String>> {
        val logs = mutableListOf<Triple<String, String, String>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_LOGS", null)
        if (cursor.moveToFirst()) {
            do {
                logs.add(Triple(cursor.getString(1), cursor.getString(2), cursor.getString(3)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return logs
    }
}