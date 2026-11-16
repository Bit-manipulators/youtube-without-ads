package com.adfree.youtube

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.HashSet

object AdBlocker {
    @Volatile
    private var adHosts: Set<String> = emptySet()
    
    @Volatile
    private var cachedScript: String? = null

    fun init(context: Context) {
        if (adHosts.isNotEmpty()) return

        try {
            val set = HashSet<String>()
            context.assets.open("ad_hosts.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        set.add(trimmed.lowercase())
                    }
                }
            }
            adHosts = set
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAdHost(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val uri = try { Uri.parse(url) } catch (e: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false
        val hosts = adHosts

        for (adHost in hosts) {
            if (host == adHost || host.endsWith(".$adHost") || url.contains(adHost)) {
                return true
            }
        }
        return false
    }

    fun getAdBlockScript(context: Context): String {
        cachedScript?.let { return it }

        return try {
            val stream = context.assets.open("adblock.js")
            val script = stream.bufferedReader().use { it.readText() }
            cachedScript = script
            script
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
