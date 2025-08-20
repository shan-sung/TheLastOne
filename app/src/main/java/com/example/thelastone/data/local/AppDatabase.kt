package com.example.thelastone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MessageEntity::class, SavedPlaceEntity::class],
    version = 2,                 // ← bump 1 -> 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        // 1 -> 2: 新增 saved_places
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_places(
                        placeId TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        address TEXT,
                        lat REAL NOT NULL,
                        lng REAL NOT NULL,
                        rating REAL,
                        userRatingsTotal INTEGER,
                        photoUrl TEXT,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_saved_places_placeId ON saved_places(placeId)")
            }
        }
    }
}