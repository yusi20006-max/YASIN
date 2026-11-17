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
        OperationLogEntity::class,
        TelegramChannelEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class YasinDatabase : RoomDatabase() {
    abstract fun yasinDao(): YasinDao

    companion object {
        @Volatile
        private var INSTANCE: YasinDatabase? = null

        val defaultTelegramChannels: List<TelegramChannelEntity> = listOf(
            TelegramChannelEntity("akhbare_fouri", "اخبار فوری", isDefault = true, isEnabled = true),
            TelegramChannelEntity("khabarfouri", "خبرفوری", isDefault = true, isEnabled = true),
            TelegramChannelEntity("zoomit", "زومیت (فناوری)", isDefault = true, isEnabled = true),
            TelegramChannelEntity("digiato", "دیجیاتو (فناوری)", isDefault = true, isEnabled = true),
            TelegramChannelEntity("irna_1313", "خبرگزاری ایرنا", isDefault = true, isEnabled = true),
            TelegramChannelEntity("isna94", "خبرگزاری ایسنا", isDefault = true, isEnabled = true)
        )

        private fun seedDefaultChannelsSql(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            defaultTelegramChannels.forEachIndexed { index, ch ->
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO telegram_channels (username, titleFa, isDefault, isEnabled, addedAt)
                    VALUES (?, ?, 1, 1, ?)
                    """.trimIndent(),
                    arrayOf<Any>(ch.username, ch.titleFa, now + index)
                )
            }
        }

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE press_articles ADD COLUMN sourceChannel TEXT")
                db.execSQL("ALTER TABLE press_articles ADD COLUMN sourcePostKey TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS telegram_channels (
                        username TEXT PRIMARY KEY NOT NULL,
                        titleFa TEXT NOT NULL,
                        isDefault INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                seedDefaultChannelsSql(db)
            }
        }

        fun getInstance(context: Context): YasinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YasinDatabase::class.java,
                    "yasin_hub.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaultChannelsSql(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
