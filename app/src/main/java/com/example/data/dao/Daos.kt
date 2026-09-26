package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AdminUserEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.DeviceEntity
import com.example.data.model.ScheduleEntity
import com.example.data.model.SettingsEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email_or_phone = :emailOrPhone LIMIT 1")
    suspend fun getUserByEmailOrPhone(emailOrPhone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    fun countUsersFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users WHERE created_at >= :sinceTimestamp")
    fun countUsersSinceFlow(sinceTimestamp: Long): Flow<Int>

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: String)
}

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    @Query("SELECT * FROM devices WHERE device_identifier_hash = :hash AND user_id = :userId LIMIT 1")
    suspend fun getDeviceByHashAndUser(hash: String, userId: Long): DeviceEntity?

    @Query("SELECT * FROM devices WHERE device_identifier_hash = :hash LIMIT 1")
    suspend fun getDeviceByHash(hash: String): DeviceEntity?
}

@Dao
interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Query("SELECT * FROM tickets WHERE ticket_number = :ticketNumber LIMIT 1")
    suspend fun getTicketByNumber(ticketNumber: String): TicketEntity?

    @Query("SELECT * FROM tickets ORDER BY created_at DESC")
    fun getAllTicketsFlow(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE user_id = :userId ORDER BY created_at DESC")
    fun getTicketsByUserFlow(userId: Long): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getTicketsByUser(userId: Long): List<TicketEntity>

    @Query("SELECT * FROM tickets WHERE device_id = :deviceId AND purchase_date = :dateStr LIMIT 1")
    suspend fun getTicketForDeviceOnDate(deviceId: Long, dateStr: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE user_id = :userId AND purchase_date = :dateStr LIMIT 1")
    suspend fun getTicketForUserOnDate(userId: Long, dateStr: String): TicketEntity?

    @Query("SELECT COUNT(*) FROM tickets")
    fun countTotalTicketsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tickets WHERE purchase_date = :dateStr")
    fun countTodayTicketsFlow(dateStr: String): Flow<Int>

    @Query("SELECT * FROM tickets ORDER BY created_at DESC LIMIT :limit")
    fun getRecentTicketsFlow(limit: Int): Flow<List<TicketEntity>>

    @Query("UPDATE tickets SET status = :status, draw_notes = :notes WHERE id = :ticketId")
    suspend fun updateTicketStatus(ticketId: Long, status: String, notes: String?)
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners WHERE id = 1 LIMIT 1")
    fun getBannerFlow(): Flow<BannerEntity?>

    @Query("SELECT * FROM banners WHERE id = 1 LIMIT 1")
    suspend fun getBanner(): BannerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBanner(banner: BannerEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)

    @Query("UPDATE settings SET ticket_purchase_enabled = :enabled, updated_at = :updatedAt WHERE id = 1")
    suspend fun setTicketPurchaseEnabled(enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY created_at DESC")
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE is_active = 1 LIMIT 1")
    fun getActiveScheduleFlow(): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveSchedule(): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("UPDATE schedules SET is_active = CASE WHEN id = :activeId THEN 1 ELSE 0 END")
    suspend fun setActiveSchedule(activeId: Long)
}

@Dao
interface AdminUserDao {
    @Query("SELECT * FROM admin_users WHERE username = :username LIMIT 1")
    suspend fun getAdminByUsername(username: String): AdminUserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAdmin(admin: AdminUserEntity): Long

    @Query("SELECT COUNT(*) FROM admin_users")
    suspend fun getAdminCount(): Int
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}
