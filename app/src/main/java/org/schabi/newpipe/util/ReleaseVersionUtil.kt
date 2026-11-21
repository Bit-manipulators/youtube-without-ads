package org.schabi.newpipe.util

import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.schabi.newpipe.App
import org.schabi.newpipe.error.ErrorInfo
import org.schabi.newpipe.error.ErrorUtil.Companion.createNotification
import org.schabi.newpipe.error.UserAction

object ReleaseVersionUtil {
    // Public key of the certificate that is used in NewPipe release versions
    private const val RELEASE_CERT_PUBLIC_KEY_SHA256 =
        "cb84069bd68116bafae5ee4ee5b08a567aa6d898404e7cb12f9e756df5cf5cab"

    val isReleaseApk = true

    @JvmStatic
    fun isNewerVersion(remote: String, local: String): Boolean {
        val rParts = remote.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun isLastUpdateCheckExpired(expiry: Long): Boolean {
        return Instant.ofEpochSecond(expiry) < Instant.now()
    }

    /**
     * Coerce expiry date time in between 1 hour and 6 hours from now
     *
     * @return Epoch second of expiry date time
     */
    fun coerceUpdateCheckExpiry(expiryString: String?): Long {
        val nowPlus1Hour = ZonedDateTime.now().plusHours(1)
        val expiry = expiryString?.let {
            try {
                ZonedDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(it))
                    .coerceIn(nowPlus1Hour, nowPlus1Hour.plusHours(5))
            } catch (e: Exception) {
                nowPlus1Hour
            }
        } ?: nowPlus1Hour
        return expiry.toEpochSecond()
    }
}
