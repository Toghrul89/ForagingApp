package com.example.foragingapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.foragingapp.model.LogEntry

class LogDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_LOGS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_LOCATION TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_NOTES TEXT,
                $COL_IMAGE_URI TEXT,
                $COL_LAT REAL,
                $COL_LNG REAL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    fun insertLog(entry: LogEntry): Long {
        val values = ContentValues().apply {
            put(COL_NAME, entry.name)
            put(COL_LOCATION, entry.location)
            put(COL_DATE, entry.date)
            put(COL_NOTES, entry.notes)
            put(COL_IMAGE_URI, entry.imageUri)
            put(COL_LAT, entry.lat)
            put(COL_LNG, entry.lng)
        }
        return writableDatabase.insert(TABLE_LOGS, null, values)
    }

    fun getAllLogs(): List<LogEntry> {
        val list = mutableListOf<LogEntry>()
        val cursor: Cursor = readableDatabase.query(
            TABLE_LOGS, null, null, null, null, null, "$COL_ID DESC"
        )
        cursor.use { c ->
            val idIx = c.getColumnIndexOrThrow(COL_ID)
            val nameIx = c.getColumnIndexOrThrow(COL_NAME)
            val locIx = c.getColumnIndexOrThrow(COL_LOCATION)
            val dateIx = c.getColumnIndexOrThrow(COL_DATE)
            val notesIx = c.getColumnIndexOrThrow(COL_NOTES)
            val imgIx = c.getColumnIndexOrThrow(COL_IMAGE_URI)
            val latIx = c.getColumnIndexOrThrow(COL_LAT)
            val lngIx = c.getColumnIndexOrThrow(COL_LNG)
            while (c.moveToNext()) {
                list.add(
                    LogEntry(
                        id = c.getLong(idIx),
                        name = c.getString(nameIx),
                        location = c.getString(locIx),
                        date = c.getString(dateIx),
                        notes = c.getString(notesIx) ?: "",
                        imageUri = c.getString(imgIx) ?: "",
                        lat = if (!c.isNull(latIx)) c.getDouble(latIx) else null,
                        lng = if (!c.isNull(lngIx)) c.getDouble(lngIx) else null,
                    )
                )
            }
        }
        return list
    }

    companion object {
        private const val DB_NAME = "foraging.db"
        private const val DB_VERSION = 1
        const val TABLE_LOGS = "logs"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_LOCATION = "location"
        const val COL_DATE = "date"
        const val COL_NOTES = "notes"
        const val COL_IMAGE_URI = "imageUri"
        const val COL_LAT = "lat"
        const val COL_LNG = "lng"
    }
}