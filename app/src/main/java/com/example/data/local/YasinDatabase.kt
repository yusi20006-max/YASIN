package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PressArticleEntity::class,
        ChatMessageEntity::class,
        OperationLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class YasinDatabase : RoomDatabase() {
    abstract fun yasinDao(): YasinDao

    companion object {
        @Volatile
        private var INSTANCE: YasinDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE press_articles ADD COLUMN eitaaMessageId TEXT")
                db.execSQL("ALTER TABLE press_articles ADD COLUMN eitaaPublishedAt INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS operation_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        module TEXT NOT NULL,
                        level TEXT NOT NULL,
                        title TEXT NOT NULL,
                        details TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): YasinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YasinDatabase::class.java,
                    "yasin_hub.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
