package com.baobao.fatloss.di

import android.app.Application
import androidx.room.Room
import com.baobao.fatloss.data.local.ApiKeyStore
import com.baobao.fatloss.data.local.AppDatabase
import com.baobao.fatloss.data.local.LanguageStore
import com.baobao.fatloss.data.remote.DoubaoApiService
import com.baobao.fatloss.data.repository.*

class AppContainer(application: Application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "fit_assistant.db"
    ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
     .build()

    // DAOs
    private val userProfileDao = db.userProfileDao()
    private val dailyLedgerDao = db.dailyLedgerDao()
    private val foodLogDao = db.foodLogDao()
    private val weightRecordDao = db.weightRecordDao()
    private val chatMessageDao = db.chatMessageDao()
    val userMemoryDao = db.userMemoryDao()
    val dailyNoteDao = db.dailyNoteDao()

    // API Key 安全存储
    val apiKeyStore = ApiKeyStore(application)

    // 语言偏好
    val languageStore = LanguageStore(application)

    // Network
    private val doubaoApiService = DoubaoApiService()

    // Repositories
    val userProfileRepo = UserProfileRepository(userProfileDao)
    val dailyLedgerRepo = DailyLedgerRepository(dailyLedgerDao)
    val foodLogRepo = FoodLogRepository(foodLogDao)
    val weightRecordRepo = WeightRecordRepository(weightRecordDao)
    val chatMessageRepo = ChatMessageRepository(chatMessageDao)
    val memoryManager = MemoryManager(userProfileDao, chatMessageDao, userMemoryDao)
    val aiRepo = AiRepository(
        chatMessageDao,
        doubaoApiService,
        apiKeyStore,
        userProfileDao,
        dailyLedgerDao,
        foodLogDao,
        userMemoryDao,
        dailyNoteDao,
        memoryManager,
        application
    )
}
