package com.example.foragingapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.foragingapp.model.LogEntry

@Dao
interface LogDao {

    @Query("SELECT * FROM logs ORDER BY id DESC")
    fun getAllLogs(): LiveData<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY id DESC")
    suspend fun getAllLogsOnce(): List<LogEntry>

    @Query("SELECT * FROM logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): LogEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry): Long

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)
}