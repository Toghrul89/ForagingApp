package com.example.foragingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.foragingapp.data.AppDatabase
import com.example.foragingapp.data.LogRepository
import com.example.foragingapp.model.LogEntry
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LogRepository =
        LogRepository(AppDatabase.getInstance(application).logDao())   // ← init here

    val allLogs: LiveData<List<LogEntry>>

    private val _searchQuery = MutableLiveData<String>("")
    val searchResults: LiveData<List<LogEntry>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) repository.allLogs   // ✅ now safe
        else repository.search(query)
    }

    init {
        allLogs = repository.allLogs
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
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

    fun toggleFavorite(entry: LogEntry) = viewModelScope.launch {
        repository.update(entry.copy(isFavorite = !entry.isFavorite))
    }

    suspend fun getLogById(id: Long): LogEntry? = repository.getLogById(id)

    suspend fun getAllLogsOnce(): List<LogEntry> = repository.getAllLogsOnce()
}