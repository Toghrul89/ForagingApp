package com.example.foragingapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.foragingapp.model.LogEntry

@Dao
interface LogDao {

    @Query("SELECT * FROM logs ORDER BY isFavorite DESC, id DESC")
    fun getAllLogs(): LiveData<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY isFavorite DESC, id DESC")
    suspend fun getAllLogsOnce(): List<LogEntry>

    @Query("SELECT * FROM logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): LogEntry?

    @Query("""SELECT * FROM logs WHERE
        name LIKE '%' || :query || '%' OR
        treeType LIKE '%' || :query || '%' OR
        location LIKE '%' || :query || '%' OR
        notes LIKE '%' || :query || '%'
        ORDER BY isFavorite DESC, id DESC""")
    fun search(query: String): LiveData<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE treeType = :type ORDER BY isFavorite DESC, id DESC")
    fun getByType(type: String): LiveData<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE isFavorite = 1 ORDER BY id DESC")
    fun getFavorites(): LiveData<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE ripeness = 'Peak' ORDER BY id DESC")
    fun getPeakRipeness(): LiveData<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry): Long

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)
}
