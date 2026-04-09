package com.example.foragingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.foragingapp.data.AppDatabase
import com.example.foragingapp.data.LogRepository
import com.example.foragingapp.model.LogEntry
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LogRepository
    val allLogs: LiveData<List<LogEntry>>

    init {
        val dao = AppDatabase.getInstance(application).logDao()
        repository = LogRepository(dao)
        allLogs = repository.allLogs
    }

    fun insert(entry: LogEntry) = viewModelScope.launch {
        repository.insert(entry)
    }

    fun update(entry: LogEntry) = viewModelScope.launch {
        repository.update(entry)
    }

    fun delete(entry: LogEntry) = viewModelScope.launch {
        repository.delete(entry)
    }

    suspend fun getLogById(id: Long): LogEntry? = repository.getLogById(id)

    suspend fun getAllLogsOnce(): List<LogEntry> = repository.getAllLogsOnce()
} 