package com.adfree.youtube

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.adfree.youtube.databinding.ActivityMainBinding
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: GeckoSession
    private var runtime: GeckoRuntime? = null
    private var isFullScreen: Boolean = false
    private var canGoBack: Boolean = false

    private val youtubeHomeUrl = "https://m.youtube.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGeckoView()
        setupSwipeRefresh()
        setupBackPressHandling()

        val targetUrl = intent?.data?.toString() ?: youtubeHomeUrl
        session.loadUri(targetUrl)
    }

    private fun setupGeckoView() {
        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .aboutConfigEnabled(false)
            .build()

        runtime = GeckoRuntime.create(this, runtimeSettings)

        val sessionSettings = GeckoSessionSettings.Builder()
            .useTrackingProtection(true)
            .fullScreenPictureInPicture(true)
            .build()

        session = GeckoSession(sessionSettings)
        session.open(runtime!!)
        binding.geckoView.setSession(session)

        // Install genuine uBlock Origin directly from app assets
        runtime?.webExtensionController?.installBuiltIn("resource://android/assets/extensions/ublock/")
            ?.accept(
                { extension ->
                    Log.i("AdFreeYT", "uBlock Origin loaded: ${extension?.id}")
                },
                { error ->
                    Log.e("AdFreeYT", "Error loading uBlock Origin: $error")
                }
            )

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

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<GeckoSession.NavigationDelegate.LoadRequest.Response>? {
                val uri = request.uri.lowercase()

                // Keep YouTube, Google login, and auth inside GeckoView
                val isInternal = uri.contains("youtube.com") ||
                        uri.contains("youtu.be") ||
                        uri.contains("google.com") ||
                        uri.contains("gstatic.com") ||
                        uri.contains("googleusercontent.com")

                if (isInternal) {
                    return null // Allow normal load in GeckoView
                }

                // External links open in browser
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(request.uri))
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return GeckoResult.fromValue(
                    GeckoSession.NavigationDelegate.LoadRequest.Response.DENY
                )
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
