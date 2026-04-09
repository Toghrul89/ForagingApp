package com.example.foragingapp.data

import androidx.lifecycle.LiveData
import com.example.foragingapp.model.LogEntry

class LogRepository(private val dao: LogDao) {

    val allLogs: LiveData<List<LogEntry>> = dao.getAllLogs()

    suspend fun getAllLogsOnce(): List<LogEntry> = dao.getAllLogsOnce()

    suspend fun getLogById(id: Long): LogEntry? = dao.getLogById(id)

    suspend fun insert(entry: LogEntry): Long = dao.insert(entry)

    suspend fun update(entry: LogEntry) = dao.update(entry)

    suspend fun delete(entry: LogEntry) = dao.delete(entry)
}