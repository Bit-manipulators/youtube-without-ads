package com.adfree.youtube

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.Collections
import java.util.HashSet

object AdBlocker {
    private val adHosts: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private var cachedScript: String? = null

    fun init(context: Context) {
        if (adHosts.isNotEmpty()) return

        Thread {
            try {
                val inputStream = context.assets.open("ad_hosts.txt")
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim()
                    if (!trimmed.isNullOrEmpty() && !trimmed.startsWith("#")) {
                        adHosts.add(trimmed.lowercase())
                    }
                }
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun isAdHost(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val uri = try { Uri.parse(url) } catch (e: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false

        // Check exact match or subdomain match
        for (adHost in adHosts) {
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
