package com.example.foragingapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foragingapp.model.LogEntry

@Database(entities = [LogEntry::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Migrate from v2 to v3. Keeps existing logs and adds publishing-era fields. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN treeType TEXT NOT NULL DEFAULT 'Other'")
                db.execSQL("ALTER TABLE logs ADD COLUMN season TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE logs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN wikipediaUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN creatorUserId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE logs ADD COLUMN creatorName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE logs ADD COLUMN createdAt TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE logs ADD COLUMN verificationStatus TEXT NOT NULL DEFAULT 'Needs verification'")
                db.execSQL("ALTER TABLE logs ADD COLUMN isReported INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN dataSource TEXT NOT NULL DEFAULT 'COMMUNITY'")
                db.execSQL("ALTER TABLE logs ADD COLUMN scientificName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE logs ADD COLUMN accessType TEXT NOT NULL DEFAULT 'Unknown'")
                db.execSQL("ALTER TABLE logs ADD COLUMN fruitCategory TEXT NOT NULL DEFAULT 'Fruit'")
                db.execSQL("ALTER TABLE logs ADD COLUMN sourceLabel TEXT NOT NULL DEFAULT ''")
                // Publish-ready reset: remove old manual/demo entries from previous local builds.
                // Official rows are imported separately after startup.
                db.execSQL("DELETE FROM logs WHERE dataSource != 'OFFICIAL'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "foraging.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
