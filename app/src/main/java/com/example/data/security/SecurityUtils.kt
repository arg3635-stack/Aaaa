package com.example.data.security

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object SecurityUtils {
    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val input = "$salt:$password"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val calculated = hashPassword(password, salt)
        return calculated.equals(expectedHash, ignoreCase = true)
    }

    fun getDeviceIdentifierHash(context: Context): String {
        val prefs = context.getSharedPreferences("peddi_lottery_device", Context.MODE_PRIVATE)
        var cachedHash = prefs.getString("device_hash", null)
        if (!cachedHash.isNullOrBlank()) {
            return cachedHash
        }

        // Try getting android_id or fallback to persistent UUID
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Exception) {
            ""
        }
        val rawSeed = if (androidId.isNotBlank() && androidId != "9774d56d682e549c") {
            "device_seed_$androidId"
        } else {
            "device_uuid_${UUID.randomUUID()}"
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawSeed.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16) // 16-hex character compact hash

        prefs.edit().putString("device_hash", hash).apply()
        return hash
    }
}
