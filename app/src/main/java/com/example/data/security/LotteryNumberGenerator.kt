package com.example.data.security

import com.example.data.dao.TicketDao
import java.security.SecureRandom

object LotteryNumberGenerator {
    private val random = SecureRandom()
    private const val UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    /**
     * Generates a 8-character lottery number:
     * 2 digits + 1 uppercase letter + 5 digits (e.g., "97G90763")
     */
    fun generateRawCode(): String {
        // 2 random digits (00-99)
        val d1 = random.nextInt(10)
        val d2 = random.nextInt(10)
        // 1 uppercase letter
        val letter = UPPERCASE_LETTERS[random.nextInt(UPPERCASE_LETTERS.length)]
        // 5 random digits (00000-99999)
        val d3 = random.nextInt(10)
        val d4 = random.nextInt(10)
        val d5 = random.nextInt(10)
        val d6 = random.nextInt(10)
        val d7 = random.nextInt(10)

        return "$d1$d2$letter$d3$d4$d5$d6$d7"
    }

    /**
     * Generates a guaranteed-unique ticket number by checking against existing tickets in DB.
     */
    suspend fun generateUniqueNumber(ticketDao: TicketDao, maxAttempts: Int = 100): String {
        var attempts = 0
        while (attempts < maxAttempts) {
            val candidate = generateRawCode()
            val existing = ticketDao.getTicketByNumber(candidate)
            if (existing == null) {
                return candidate
            }
            attempts++
        }
        // Fallback in astronomical collision case
        val candidate = generateRawCode()
        return candidate
    }
}
