package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AdminUserDao
import com.example.data.dao.AuditLogDao
import com.example.data.dao.BannerDao
import com.example.data.dao.DeviceDao
import com.example.data.dao.ScheduleDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.TicketDao
import com.example.data.dao.UserDao
import com.example.data.model.AdminUserEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.DeviceEntity
import com.example.data.model.ScheduleEntity
import com.example.data.model.SettingsEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import com.example.data.security.SecurityUtils
import com.example.domain.schedule.ScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        TicketEntity::class,
        BannerEntity::class,
        SettingsEntity::class,
        ScheduleEntity::class,
        AdminUserEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PeddiLotteryDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun ticketDao(): TicketDao
    abstract fun bannerDao(): BannerDao
    abstract fun settingsDao(): SettingsDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun adminUserDao(): AdminUserDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: PeddiLotteryDatabase? = null

        fun getInstance(context: Context): PeddiLotteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PeddiLotteryDatabase::class.java,
                    "peddi_lottery_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default configuration on first launch
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).prepopulateInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun prepopulateInitialData() {
        // Pre-populate Default Admin
        val adminDao = adminUserDao()
        if (adminDao.getAdminCount() == 0) {
            val adminSalt = SecurityUtils.generateSalt()
            val adminHash = SecurityUtils.hashPassword("PeddiAdmin@2026", adminSalt)
            adminDao.insertAdmin(
                AdminUserEntity(
                    username = "admin",
                    passwordHash = adminHash,
                    salt = adminSalt,
                    role = "SUPER_ADMIN"
                )
            )
        }

        // Pre-populate Default Banner
        val bannerDao = bannerDao()
        if (bannerDao.getBanner() == null) {
            bannerDao.upsertBanner(
                BannerEntity(
                    id = 1,
                    imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=1000&q=80",
                    title = "Grand Daily Jackpot Draw!",
                    description = "Claim your exclusive free daily PeddiLottery ticket today and stand a chance to win grand prizes!",
                    buttonText = "Buy Ticket Now",
                    buttonAction = "BUY_TICKET",
                    enabled = true
                )
            )
        }

        // Pre-populate Default Settings
        val settingsDao = settingsDao()
        if (settingsDao.getSettings() == null) {
            settingsDao.upsertSettings(
                SettingsEntity(
                    id = 1,
                    ticketPurchaseEnabled = true,
                    activeScheduleId = 1
                )
            )
        }

        // Pre-populate Today's Active Schedule (10:00 AM to 6:00 PM, result 8:00 PM)
        val scheduleDao = scheduleDao()
        if (scheduleDao.getActiveSchedule() == null) {
            val todayStr = ScheduleEngine.getCurrentDateStr("Asia/Kolkata")
            val scheduleId = scheduleDao.insertSchedule(
                ScheduleEntity(
                    name = "Daily Super Draw",
                    dateStr = todayStr,
                    openTime = "10:00",
                    closeTime = "18:00",
                    resultTime = "20:00",
                    timezoneId = "Asia/Kolkata",
                    isActive = true
                )
            )
            settingsDao.upsertSettings(
                SettingsEntity(
                    id = 1,
                    ticketPurchaseEnabled = true,
                    activeScheduleId = scheduleId
                )
            )
        }

        // Pre-populate Demo Customer User for instant demonstration
        val userDao = userDao()
        if (userDao.getUserByEmailOrPhone("customer@peddilottery.com") == null) {
            val userSalt = SecurityUtils.generateSalt()
            val userHash = SecurityUtils.hashPassword("Customer@123", userSalt)
            userDao.insertUser(
                UserEntity(
                    name = "Suresh Peddireddy",
                    emailOrPhone = "customer@peddilottery.com",
                    passwordHash = userHash,
                    salt = userSalt,
                    status = "ACTIVE"
                )
            )
        }

        // Initial Audit Log
        val auditDao = auditLogDao()
        auditDao.insertLog(
            AuditLogEntity(
                adminId = "SYSTEM",
                action = "INITIALIZE_DATABASE",
                details = "PeddiLottery database initialized with default configurations and schedules."
            )
        )
    }
}
