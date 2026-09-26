package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email_or_phone"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "email_or_phone")
    val emailOrPhone: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    val salt: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE" // "ACTIVE", "SUSPENDED"
)

@Entity(
    tableName = "devices",
    indices = [Index(value = ["device_identifier_hash"])]
)
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "device_identifier_hash")
    val deviceIdentifierHash: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tickets",
    indices = [
        Index(value = ["ticket_number"], unique = true),
        Index(value = ["device_id", "purchase_date"]),
        Index(value = ["user_id", "purchase_date"])
    ]
)
data class TicketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "device_id")
    val deviceId: Long,
    @ColumnInfo(name = "ticket_number")
    val ticketNumber: String, // e.g. "97G90763"
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: String, // e.g. "2026-09-25"
    @ColumnInfo(name = "purchase_time")
    val purchaseTime: String, // e.g. "10:15:30 AM"
    val status: String = "PENDING", // "PENDING", "WON", "DRAWN", "CANCELLED"
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "draw_notes")
    val drawNotes: String? = null
)

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey
    val id: Long = 1,
    @ColumnInfo(name = "image_url")
    val imageUrl: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "button_text")
    val buttonText: String,
    @ColumnInfo(name = "button_action")
    val buttonAction: String, // "BUY_TICKET", "RULES", "SCHEDULE", "MY_TICKETS"
    val enabled: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Long = 1,
    @ColumnInfo(name = "ticket_purchase_enabled")
    val ticketPurchaseEnabled: Boolean = true,
    @ColumnInfo(name = "active_schedule_id")
    val activeScheduleId: Long? = 1,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "date_str")
    val dateStr: String, // "2026-09-25"
    @ColumnInfo(name = "open_time")
    val openTime: String, // "10:00" in 24h
    @ColumnInfo(name = "close_time")
    val closeTime: String, // "18:00" in 24h
    @ColumnInfo(name = "result_time")
    val resultTime: String, // "20:00" in 24h
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String = "Asia/Kolkata",
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "admin_users",
    indices = [Index(value = ["username"], unique = true)]
)
data class AdminUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    val salt: String,
    val role: String = "SUPER_ADMIN",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "admin_id")
    val adminId: String,
    val action: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
