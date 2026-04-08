package com.baobao.fatloss.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.baobao.fatloss.data.local.dao.*
import com.baobao.fatloss.data.local.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        DailyLedgerEntity::class,
        FoodLogEntity::class,
        WeightRecordEntity::class,
        ChatMessageEntity::class,
        UserMemoryEntity::class,
        DailyNoteEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun dailyLedgerDao(): DailyLedgerDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun weightRecordDao(): WeightRecordDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun dailyNoteDao(): DailyNoteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN gender INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_memory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        content TEXT NOT NULL,
                        source TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_note (
                        date INTEGER PRIMARY KEY NOT NULL,
                        summary TEXT NOT NULL,
                        calorieSummary TEXT NOT NULL,
                        aiComment TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN aiPersona TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_memory ADD COLUMN embedding TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_message ADD COLUMN imagePath TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_ledger ADD COLUMN waterMl INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
