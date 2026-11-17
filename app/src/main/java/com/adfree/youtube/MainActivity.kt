package com.adfree.youtube

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.adfree.youtube.databinding.ActivityMainBinding
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: GeckoSession
    private var runtime: GeckoRuntime? = null
    private var isFullScreen: Boolean = false
    private var canGoBack: Boolean = false
    private var pageLoaded: Boolean = false

    private val youtubeHomeUrl = "https://m.youtube.com"
    private val extensionId = "uBlock0@raymondhill.net"
    private val extensionUri = "resource://android/assets/extensions/ublock/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGeckoView()
        setupSwipeRefresh()
        setupBackPressHandling()

        val targetUrl = intent?.data?.toString() ?: youtubeHomeUrl
        initializeAndLoad(targetUrl)
    }

    private fun initializeAndLoad(targetUrl: String) {
        binding.progressBar.visibility = View.VISIBLE

        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            loadInitialPage(targetUrl)
        }
        // Safety timeout so user is never stuck waiting if extension verification is delayed
        handler.postDelayed(timeoutRunnable, 2500)

        runtime?.webExtensionController?.ensureBuiltIn(extensionUri, extensionId)
            ?.accept(
                { extension ->
                    Log.i("AdFreeYT", "uBlock Origin verified & active: ${extension?.id}")
                    handler.removeCallbacks(timeoutRunnable)
                    runOnUiThread {
                        loadInitialPage(targetUrl)
                    }
                },
                { error ->
                    Log.e("AdFreeYT", "Error ensuring uBlock Origin: $error")
                    handler.removeCallbacks(timeoutRunnable)
                    runOnUiThread {
                        loadInitialPage(targetUrl)
                    }
                }
            )
    }

    private fun loadInitialPage(url: String) {
        if (!pageLoaded) {
            pageLoaded = true
            session.loadUri(url)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = intent?.data?.toString()
        if (!url.isNullOrEmpty()) {
            session.loadUri(url)
        }
    }

    private fun setupGeckoView() {
        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .aboutConfigEnabled(false)
            .consoleOutput(true)
            .build()

        runtime = GeckoRuntime.create(this, runtimeSettings)

        // Set prompt delegate to auto-grant all extension permissions
        runtime?.webExtensionController?.promptDelegate = object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<out String>,
                origins: Array<out String>
            ): GeckoResult<WebExtension.PermissionPromptResponse>? {
                Log.i("AdFreeYT", "PromptDelegate: auto-allowing extension ${extension.id}")
                return GeckoResult.fromValue(
                    WebExtension.PermissionPromptResponse(
                        true, // isPermissionsGranted
                        true  // isPrivateModeGranted
                    )
                )
            }

            override fun onOptionalPrompt(
                extension: WebExtension,
                permissions: Array<out String>,
                origins: Array<out String>
            ): GeckoResult<AllowOrDeny>? {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onUpdatePrompt(
                extension: WebExtension,
                newPermissions: Array<out String>,
                newOrigins: Array<out String>
            ): GeckoResult<AllowOrDeny>? {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
        }

        val sessionSettings = GeckoSessionSettings.Builder()
            .useTrackingProtection(true)
            .build()

        session = GeckoSession(sessionSettings)
        session.open(runtime!!)
        binding.geckoView.setSession(session)

        // Navigation & Progress handling
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                runOnUiThread {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                runOnUiThread {
                    binding.progressBar.progress = progress
                    if (progress >= 100) {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBackValue: Boolean) {
                canGoBack = canGoBackValue
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                isFullScreen = fullScreen
                runOnUiThread {
                    if (fullScreen) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        hideSystemUI()
                    } else {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        showSystemUI()
                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_red_dark)
        binding.swipeRefreshLayout.setOnRefreshListener {
            session.reload()
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullScreen) {
                    session.exitFullScreen()
                } else if (canGoBack) {
                    session.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onDestroy() {
        session.close()
        runtime?.shutdown()
        super.onDestroy()
    }
}
