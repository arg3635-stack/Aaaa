package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.database.PeddiLotteryDatabase
import com.example.data.model.AdminUserEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.DeviceEntity
import com.example.data.model.ScheduleEntity
import com.example.data.model.SettingsEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import com.example.data.security.LotteryNumberGenerator
import com.example.data.security.SecurityUtils
import com.example.domain.schedule.ScheduleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class PurchaseResult {
    data class Success(val ticket: TicketEntity) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

class LotteryRepository(private val context: Context) {

    private val db = PeddiLotteryDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val deviceDao = db.deviceDao()
    private val ticketDao = db.ticketDao()
    private val bannerDao = db.bannerDao()
    private val settingsDao = db.settingsDao()
    private val scheduleDao = db.scheduleDao()
    private val adminUserDao = db.adminUserDao()
    private val auditLogDao = db.auditLogDao()

    // Concurrency mutex to guarantee single-flight purchase per device/user
    private val purchaseMutex = Mutex()

    suspend fun initializeDefaultsIfNeeded() {
        db.prepopulateInitialData()
    }

    // -------------------------------------------------------------
    // Reactive Streams
    // -------------------------------------------------------------
    val bannerFlow: Flow<BannerEntity?> = bannerDao.getBannerFlow()
    val settingsFlow: Flow<SettingsEntity?> = settingsDao.getSettingsFlow()
    val activeScheduleFlow: Flow<ScheduleEntity?> = scheduleDao.getActiveScheduleFlow()
    val allSchedulesFlow: Flow<List<ScheduleEntity>> = scheduleDao.getAllSchedulesFlow()
    val allTicketsFlow: Flow<List<TicketEntity>> = ticketDao.getAllTicketsFlow()
    val allUsersFlow: Flow<List<UserEntity>> = userDao.getAllUsersFlow()
    val totalTicketsCountFlow: Flow<Int> = ticketDao.countTotalTicketsFlow()
    val totalUsersCountFlow: Flow<Int> = userDao.countUsersFlow()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogsFlow()

    fun getUserTicketsFlow(userId: Long): Flow<List<TicketEntity>> {
        return ticketDao.getTicketsByUserFlow(userId)
    }

    fun getTodayTicketsCountFlow(dateStr: String): Flow<Int> {
        return ticketDao.countTodayTicketsFlow(dateStr)
    }

    // -------------------------------------------------------------
    // Authentication: Customer
    // -------------------------------------------------------------
    suspend fun registerUser(
        name: String,
        emailOrPhone: String,
        password: String,
        deviceHash: String
    ): AuthResult<UserEntity> {
        val trimmedName = name.trim()
        val trimmedIdentifier = emailOrPhone.trim().lowercase()

        if (trimmedName.length < 2) {
            return AuthResult.Error("Please enter a valid full name.")
        }
        if (trimmedIdentifier.length < 5) {
            return AuthResult.Error("Please enter a valid email or phone number.")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters long.")
        }

        val existingUser = userDao.getUserByEmailOrPhone(trimmedIdentifier)
        if (existingUser != null) {
            return AuthResult.Error("An account with this email/phone already exists.")
        }

        val salt = SecurityUtils.generateSalt()
        val passwordHash = SecurityUtils.hashPassword(password, salt)
        val user = UserEntity(
            name = trimmedName,
            emailOrPhone = trimmedIdentifier,
            passwordHash = passwordHash,
            salt = salt,
            status = "ACTIVE"
        )

        val userId = userDao.insertUser(user)
        val createdUser = user.copy(id = userId)

        // Register device
        deviceDao.insertDevice(
            DeviceEntity(
                userId = userId,
                deviceIdentifierHash = deviceHash
            )
        )

        return AuthResult.Success(createdUser)
    }

    suspend fun loginUser(
        emailOrPhone: String,
        password: String,
        deviceHash: String
    ): AuthResult<UserEntity> {
        val trimmedIdentifier = emailOrPhone.trim().lowercase()
        val user = userDao.getUserByEmailOrPhone(trimmedIdentifier)
            ?: return AuthResult.Error("No account found with this email or phone.")

        if (user.status == "SUSPENDED") {
            return AuthResult.Error("Your account has been suspended. Please contact administrator.")
        }

        val isValid = SecurityUtils.verifyPassword(password, user.salt, user.passwordHash)
        if (!isValid) {
            return AuthResult.Error("Incorrect password. Please try again.")
        }

        // Link device if not linked
        val existingDevice = deviceDao.getDeviceByHashAndUser(deviceHash, user.id)
        if (existingDevice == null) {
            deviceDao.insertDevice(
                DeviceEntity(
                    userId = user.id,
                    deviceIdentifierHash = deviceHash
                )
            )
        }

        return AuthResult.Success(user)
    }

    // -------------------------------------------------------------
    // Authentication: Admin
    // -------------------------------------------------------------
    suspend fun loginAdmin(
        username: String,
        password: String
    ): AuthResult<AdminUserEntity> {
        val trimmed = username.trim()
        val admin = adminUserDao.getAdminByUsername(trimmed)
            ?: return AuthResult.Error("Admin account not found.")

        val isValid = SecurityUtils.verifyPassword(password, admin.salt, admin.passwordHash)
        if (!isValid) {
            return AuthResult.Error("Invalid admin credentials.")
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = admin.username,
                action = "ADMIN_LOGIN",
                details = "Admin logged in successfully."
            )
        )

        return AuthResult.Success(admin)
    }

    // -------------------------------------------------------------
    // Ticket Operations: ONE TICKET PER DEVICE PER DAY (Backend Enforced)
    // -------------------------------------------------------------
    suspend fun buyTicket(
        userId: Long,
        deviceHash: String
    ): PurchaseResult = purchaseMutex.withLock {
        try {
            // 1. Check user validity
            val user = userDao.getUserById(userId)
                ?: return PurchaseResult.Error("User not found.")
            if (user.status == "SUSPENDED") {
                return PurchaseResult.Error("Your account has been suspended.")
            }

            // 2. Check Lottery Time Window & Master Enabled Status
            val settings = settingsDao.getSettings()
            val activeSchedule = scheduleDao.getActiveSchedule()
            val evaluation = ScheduleEngine.evaluate(settings, activeSchedule)

            if (!evaluation.isOpen) {
                val reason = evaluation.closureReason ?: "Ticket purchasing is currently closed."
                return PurchaseResult.Error(reason)
            }

            // 3. Resolve timezone & current calendar date in schedule's timezone
            val tzId = activeSchedule?.timezoneId ?: ScheduleEngine.DEFAULT_TIMEZONE
            val currentDateStr = ScheduleEngine.getCurrentDateStr(tzId)
            val currentTimeFormatted = ScheduleEngine.getCurrentTimeFormatted(tzId)

            // 4. Ensure Device record exists
            var device = deviceDao.getDeviceByHashAndUser(deviceHash, userId)
            if (device == null) {
                val deviceId = deviceDao.insertDevice(
                    DeviceEntity(
                        userId = userId,
                        deviceIdentifierHash = deviceHash
                    )
                )
                device = DeviceEntity(id = deviceId, userId = userId, deviceIdentifierHash = deviceHash)
            }

            // 5. Backend verification: Check if ticket already purchased on this device OR user today
            val deviceTicketToday = ticketDao.getTicketForDeviceOnDate(device.id, currentDateStr)
            if (deviceTicketToday != null) {
                return PurchaseResult.Error("You have already purchased today’s ticket. Please try again tomorrow.")
            }

            val userTicketToday = ticketDao.getTicketForUserOnDate(userId, currentDateStr)
            if (userTicketToday != null) {
                return PurchaseResult.Error("You have already purchased today’s ticket. Please try again tomorrow.")
            }

            // 6. Generate guaranteed unique 8-character lottery number (e.g. "97G90763")
            val uniqueNumber = LotteryNumberGenerator.generateUniqueNumber(ticketDao)

            val newTicket = TicketEntity(
                userId = userId,
                deviceId = device.id,
                ticketNumber = uniqueNumber,
                purchaseDate = currentDateStr,
                purchaseTime = currentTimeFormatted,
                status = "PENDING"
            )

            // 7. Atomic database insert
            val ticketId = ticketDao.insertTicket(newTicket)
            val savedTicket = newTicket.copy(id = ticketId)

            return PurchaseResult.Success(savedTicket)
        } catch (e: Exception) {
            return PurchaseResult.Error("Failed to purchase ticket: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun getTodayTicketForUser(userId: Long, timezoneId: String = ScheduleEngine.DEFAULT_TIMEZONE): TicketEntity? {
        val todayStr = ScheduleEngine.getCurrentDateStr(timezoneId)
        return ticketDao.getTicketForUserOnDate(userId, todayStr)
    }

    // -------------------------------------------------------------
    // Admin Controls & Audit Logging
    // -------------------------------------------------------------
    suspend fun setTicketPurchasingMaster(adminUsername: String, enabled: Boolean) {
        settingsDao.setTicketPurchaseEnabled(enabled)
        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = adminUsername,
                action = "TOGGLE_PURCHASING",
                details = "Master ticket purchasing set to ${if (enabled) "ENABLED" else "DISABLED"}."
            )
        )
    }

    suspend fun updateBanner(adminUsername: String, banner: BannerEntity) {
        bannerDao.upsertBanner(banner.copy(updatedAt = System.currentTimeMillis()))
        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = adminUsername,
                action = "UPDATE_BANNER",
                details = "Banner '${banner.title}' updated. Enabled: ${banner.enabled}."
            )
        )
    }

    suspend fun saveSchedule(adminUsername: String, schedule: ScheduleEntity): Long {
        val isNew = schedule.id == 0L
        val savedId = if (isNew) {
            val id = scheduleDao.insertSchedule(schedule)
            if (schedule.isActive) {
                scheduleDao.setActiveSchedule(id)
            }
            id
        } else {
            scheduleDao.updateSchedule(schedule)
            if (schedule.isActive) {
                scheduleDao.setActiveSchedule(schedule.id)
            }
            schedule.id
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = adminUsername,
                action = if (isNew) "CREATE_SCHEDULE" else "UPDATE_SCHEDULE",
                details = "Schedule '${schedule.name}' (${schedule.dateStr}, ${schedule.openTime}-${schedule.closeTime}) saved."
            )
        )
        return savedId
    }

    suspend fun deleteSchedule(adminUsername: String, scheduleId: Long) {
        val sched = scheduleDao.getScheduleById(scheduleId)
        scheduleDao.deleteScheduleById(scheduleId)
        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = adminUsername,
                action = "DELETE_SCHEDULE",
                details = "Schedule #$scheduleId (${sched?.name ?: "Unknown"}) deleted."
            )
        )
    }

    suspend fun setActiveSchedule(adminUsername: String, scheduleId: Long) {
        scheduleDao.setActiveSchedule(scheduleId)
        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = adminUsername,
                action = "SET_ACTIVE_SCHEDULE",
                details = "Schedule #$scheduleId set as currently active schedule."
            )
        )
    }

    suspend fun updateTicketStatus(
        adminUsername: String,
        ticketId: Long,
        newStatus: String,
        notes: String?
    ) {
        ticketDao.updateTicketStatus(ticketId, newStatus, notes)
        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = adminUsername,
                action = "UPDATE_TICKET_STATUS",
                details = "Ticket #$ticketId marked as $newStatus. Notes: ${notes ?: "None"}."
            )
        )
    }

    suspend fun updateUserStatus(
        adminUsername: String,
        userId: Long,
        newStatus: String
    ) {
        userDao.updateUserStatus(userId, newStatus)
        auditLogDao.insertLog(
            AuditLogEntity(
                adminId = adminUsername,
                action = "UPDATE_USER_STATUS",
                details = "User #$userId status changed to $newStatus."
            )
        )
    }

    suspend fun getUserById(userId: Long): UserEntity? {
        return userDao.getUserById(userId)
    }

    suspend fun getUserTickets(userId: Long): List<TicketEntity> {
        return ticketDao.getTicketsByUser(userId)
    }
}
