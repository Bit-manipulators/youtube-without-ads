package com.adfree.youtube

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.adfree.youtube.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var isPlayingVideo: Boolean = false

    @Volatile
    private var currentMainUrl: String = ""

    // Standard Chrome Mobile User-Agent matching genuine Chrome on Android
    private val customUserAgent =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val youtubeHomeUrl = "https://m.youtube.com"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ad block list safely before any requests start
        AdBlocker.init(applicationContext)

        setupWebView()
        setupSwipeRefresh()
        setupBackPressHandling()

        // Handle intent if opened via deep link or standard launch
        val targetUrl = intent?.data?.toString() ?: youtubeHomeUrl
        currentMainUrl = targetUrl
        if (savedInstanceState == null) {
            binding.webView.loadUrl(targetUrl)
        } else {
            binding.webView.restoreState(savedInstanceState)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = binding.webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.userAgentString = customUserAgent

        // Keep all navigations inside single window to prevent about:blank white screen
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)

        // Allow mixed content redirects (essential for Google SetSID cookies)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Disable X-Requested-With header to stop Google from blocking WebView login
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Crucial for autoplay and background audio playback
        settings.mediaPlaybackRequiresUserGesture = false

        // Cache configuration for fast loading
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Cookie configuration (keeps user logged in)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                try {
                    val url = request?.url?.toString() ?: return null
                    val mainUrl = currentMainUrl

                    // During Google Authentication, do NOT block any requests
                    if (mainUrl.contains("accounts.google") ||
                        mainUrl.contains("google.com/accounts") ||
                        mainUrl.contains("consent.google") ||
                        mainUrl.contains("/signin/")
                    ) {
                        return super.shouldInterceptRequest(view, request)
                    }

                    val host = request.url?.host?.lowercase() ?: ""
                    // Never intercept or block Google Authentication or security verification endpoints
                    if (host.contains("accounts.google") ||
                        host.contains("myaccount.google") ||
                        host.contains("consent.google") ||
                        host.contains("googleapis.com") ||
                        host.contains("gstatic.com") ||
                        host.contains("googleusercontent.com") ||
                        url.contains("/signin/") ||
                        url.contains("/ServiceLogin") ||
                        url.contains("/CheckCookie")
                    ) {
                        return super.shouldInterceptRequest(view, request)
                    }

                    if (AdBlocker.isAdHost(url)) {
                        return AdBlocker.createEmptyResource()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                currentMainUrl = url ?: ""
                binding.progressBar.visibility = View.VISIBLE
                injectAdBlocker()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                currentMainUrl = url ?: ""
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
                // Persist session cookies to disk
                try {
                    CookieManager.getInstance().flush()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                injectAdBlocker()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val url = uri.toString()
                val host = uri.host?.lowercase() ?: ""
                val scheme = uri.scheme?.lowercase() ?: ""

                // Handle non-HTTP schemes (e.g. intent:// or android-app://) safely
                if (scheme != "http" && scheme != "https") {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent != null && intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            return true
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    return true
                }

                // Allow all Google authentication, consent, security, and YouTube domains to stay in-app
                val isInternalAuthOrMedia = host.endsWith("youtube.com") ||
                        host == "youtu.be" ||
                        host.endsWith("ytimg.com") ||
                        host.endsWith("google.com") ||
                        host.contains(".google.") ||
                        host.endsWith("gstatic.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("g.co") ||
                        host.endsWith("googleapis.com")

                return if (isInternalAuthOrMedia) {
                    false
                } else {
                    // Open external third-party web links in device browser
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(browserIntent)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    true
                }
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
                if (newProgress >= 100) {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    onHideCustomView()
                    return
                }
                customView = view
                customViewCallback = callback

                binding.webView.visibility = View.GONE
                binding.customViewContainer.visibility = View.VISIBLE
                binding.customViewContainer.addView(
                    view,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                // Set landscape for full screen video
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                hideSystemUI()
                isPlayingVideo = true
            }

            override fun onHideCustomView() {
                binding.customViewContainer.visibility = View.GONE
                binding.customViewContainer.removeAllViews()
                customView = null
                customViewCallback?.onCustomViewHidden()

                binding.webView.visibility = View.VISIBLE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                showSystemUI()
                isPlayingVideo = false
            }
        }
    }

    private fun injectAdBlocker() {
        val currentUrl = currentMainUrl
        // ONLY inject ad blocker scripts on YouTube domain!
        // NEVER inject into Google login / account verification pages!
        val isYouTube = currentUrl.contains("youtube.com") || currentUrl.contains("youtu.be")
        val isGoogleAuth = currentUrl.contains("accounts.google") ||
                currentUrl.contains("google.com/accounts") ||
                currentUrl.contains("consent.google") ||
                currentUrl.contains("/signin/")

        if (!isYouTube || isGoogleAuth) {
            return
        }

        val script = AdBlocker.getAdBlockScript(applicationContext)
        if (script.isNotEmpty()) {
            binding.webView.evaluateJavascript(script, null)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_red_dark)
        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.webView.reload()
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    binding.webView.webChromeClient?.onHideCustomView()
                } else if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter Picture-in-Picture mode on Android 8.0+ when minimized
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val aspectRatio = Rational(16, 9)
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Throwable) {
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

    override fun onPause() {
        super.onPause()
        try {
            CookieManager.getInstance().flush()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
