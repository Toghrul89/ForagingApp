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

    @Query("SELECT * FROM logs WHERE name LIKE '%' || :query || '%' OR treeType LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' OR scientificName LIKE '%' || :query || '%' OR sourceLabel LIKE '%' || :query || '%' ORDER BY id DESC")
    fun search(query: String): LiveData<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE treeType = :type ORDER BY id DESC")
    fun getByType(type: String): LiveData<List<LogEntry>>

    @Query("SELECT COUNT(*) FROM logs WHERE dataSource = 'OFFICIAL'")
    suspend fun countOfficialTrees(): Int

    @Query("DELETE FROM logs WHERE dataSource != 'OFFICIAL'")
    suspend fun deleteCommunityAndDemoRows(): Int

    @Query(
        """
        DELETE FROM logs
        WHERE lat IS NOT NULL
        AND lng IS NOT NULL
        AND ABS(lat - :lat) < 0.00001
        AND ABS(lng - :lng) < 0.00001
        AND (
            name LIKE '%Cornelian%'
            OR notes LIKE '%sour fruit%'
            OR season = ''
        )
        """
    )
    suspend fun deleteOldIncorrectCornelianCherry(lat: Double, lng: Double): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<LogEntry>): List<Long>

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)
}
