package org.schabi.newpipe

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonParserException
import java.io.IOException
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.util.ReleaseVersionUtil

class NewVersionWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    /**
     * Method to compare the current and latest available app version.
     * If a newer version is available, we show the update notification.
     *
     * @param versionName    Name of new version
     * @param apkLocationUrl Url with the new apk
     * @param versionCode    Code of new version
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
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

    private fun compareAppVersionAndShowNotification(
        versionName: String,
        apkLocationUrl: String?
    ) {
        if (!isNewerVersion(versionName, BuildConfig.VERSION_NAME)) {
            if (inputData.getBoolean(IS_MANUAL, false)) {
                // Show toast stating that the app is up-to-date if the update check was manual.
                ContextCompat.getMainExecutor(applicationContext).execute {
                    Toast.makeText(
                        applicationContext,
                        R.string.app_update_unavailable_toast,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return
        }

        // A pending intent to open the apk location url in the browser.
        val intent = Intent(Intent.ACTION_VIEW, (apkLocationUrl ?: GITHUB_RELEASES_LATEST_PAGE).toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntentCompat.getActivity(
            applicationContext,
            0,
            intent,
            0,
            false
        )
        val channelId = applicationContext.getString(R.string.app_update_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_newpipe_update)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setContentTitle("AdFreeYT Update Available")
            .setContentText("Version $versionName is available. Tap to update with zero data loss.")

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(2000, notificationBuilder.build())
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    private fun checkNewVersion() {
        // Check if the current apk is a github one or not.
        if (!ReleaseVersionUtil.isReleaseApk) {
            return
        }

        if (!inputData.getBoolean(IS_MANUAL, false)) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            // Check if the last request has happened a certain time ago
            // to reduce the number of API requests.
            val expiry = prefs.getLong(applicationContext.getString(R.string.update_expiry_key), 0)
            if (!ReleaseVersionUtil.isLastUpdateCheckExpired(expiry)) {
                return
            }
        }

        // Make a network request to get latest AdFreeYT release data from GitHub.
        val response = DownloaderImpl.getInstance().get(GITHUB_RELEASES_API_URL)
        handleResponse(response)
    }

    private fun handleResponse(response: Response) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        try {
            val newExpiry = ReleaseVersionUtil.coerceUpdateCheckExpiry(response.getHeader("expires"))
            prefs.edit {
                putLong(applicationContext.getString(R.string.update_expiry_key), newExpiry)
            }
        } catch (e: Exception) {
            if (DEBUG) {
                Log.w(TAG, "Could not extract and save new expiry date", e)
            }
        }

        // Parse the GitHub Releases JSON response
        try {
            val releaseObj = JsonParser.`object`().from(response.responseBody())
            val tagName = releaseObj.getString("tag_name") ?: return
            val cleanVersion = tagName.removePrefix("v").trim()

            var apkUrl: String? = null
            val assets = releaseObj.getArray("assets")
            if (assets != null) {
                for (i in 0 until assets.size) {
                    val asset = assets.getObject(i)
                    val name = asset.getString("name") ?: ""
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        if (name.contains(cleanVersion)) {
                            break
                        }
                    }
                }
            }
            compareAppVersionAndShowNotification(cleanVersion, apkUrl)
        } catch (e: Exception) {
            if (DEBUG) {
                Log.w(TAG, "Could not parse GitHub release API: invalid json", e)
            }
        }
    }

    override fun doWork(): Result {
        return try {
            checkNewVersion()
            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "Could not fetch GitHub API: probably network problem", e)
            Result.failure()
        } catch (e: ReCaptchaException) {
            Log.e(TAG, "ReCaptchaException should never happen here.", e)
            Result.failure()
        }
    }

    companion object {
        private val DEBUG = MainActivity.DEBUG
        private val TAG = NewVersionWorker::class.java.simpleName
        private const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/Bit-manipulators/youtube-without-ads/releases/latest"
        private const val GITHUB_RELEASES_LATEST_PAGE = "https://github.com/Bit-manipulators/youtube-without-ads/releases/latest"
        private const val IS_MANUAL = "isManual"

        /**
         * Start a new worker which checks if all conditions for performing a version check are met,
         * fetches the API endpoint [.NEWPIPE_API_URL] containing info about the latest NewPipe
         * version and displays a notification about an available update if one is available.
         * <br></br>
         * Following conditions need to be met, before data is requested from the server:
         *
         *  *  The app is signed with the correct signing key (by TeamNewPipe / schabi).
         * If the signing key differs from the one used upstream, the update cannot be installed.
         *  * The user enabled searching for and notifying about updates in the settings.
         *  * The app did not recently check for updates.
         * We do not want to make unnecessary connections and DOS our servers.
         */
        @JvmStatic
        fun enqueueNewVersionCheckingWork(context: Context, isManual: Boolean) {
            val workRequest = OneTimeWorkRequestBuilder<NewVersionWorker>()
                .setInputData(workDataOf(IS_MANUAL to isManual))
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
