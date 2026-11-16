/**
 * YouTube Ad Blocker & Enhancer Script
 * Handles CSS ad hiding, video ad auto-skip/speedup, anti-adblock bypass, and background playback.
 */
(function() {
    'use strict';

    if (window.__yt_adblock_injected) return;
    window.__yt_adblock_injected = true;

    // 1. Inject CSS to hide static ads, banners, promos, and popup overlays
    const adStyles = `
        /* Banner & Feed Ads */
        ytd-ad-slot-renderer,
        ytd-banner-promo-renderer,
        ytd-in-feed-ad-layout-renderer,
        ytd-promoted-sparkles-web-renderer,
        ytd-promoted-video-renderer,
        ytd-display-ad-renderer,
        #masthead-ad,
        #player-ads,
        .ytp-ad-overlay-container,
        .ytp-ad-message-container,
        .ytp-ad-progress-list,
        .ytp-ad-player-overlay,
        .ytp-ad-player-overlay-layout,
        /* Mobile specific selectors */
        ytm-promoted-sparkles-web-renderer,
        ytm-ad-slot-renderer,
        ytm-companion-ad-renderer,
        ytm-promoted-sparkles-text-search-renderer,
        /* Anti-adblock dialogs */
        ytd-enforcement-message-view-model,
        tp-yt-paper-dialog[modern-dialog-layout],
        tp-yt-iron-overlay-backdrop {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
    `;

    function injectCSS() {
        if (document.getElementById('yt-adblock-styles')) return;
        const style = document.createElement('style');
        style.id = 'yt-adblock-styles';
        style.type = 'text/css';
        style.appendChild(document.createTextNode(adStyles));
        (document.head || document.documentElement).appendChild(style);
    }
    injectCSS();

    // 2. Prevent YouTube from pausing playback when minimized (Background Audio)
    try {
        Object.defineProperty(document, 'hidden', {
            get: function() { return false; },
            configurable: true
        });
        Object.defineProperty(document, 'visibilityState', {
            get: function() { return 'visible'; },
            configurable: true
        });
        document.addEventListener('visibilitychange', function(e) {
            e.stopImmediatePropagation();
        }, true);
    } catch (e) {
        console.error("Background play override error:", e);
    }

    // 3. Skip / Fast-forward Video Ads
    function handleVideoAds() {
        // Detect mobile and desktop video player
        const moviePlayer = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
        const video = document.querySelector('video');

        if (moviePlayer && moviePlayer.classList) {
            const isAdShowing = moviePlayer.classList.contains('ad-showing') ||
                                moviePlayer.classList.contains('ad-interrupting');

            if (isAdShowing && video) {
                // Mute and fast forward to the end of the ad instantly
                video.muted = true;
                video.playbackRate = 16.0;
                if (!isNaN(video.duration) && isFinite(video.duration) && video.duration > 0) {
                    video.currentTime = video.duration;
                }
            }
        }

        // Click any visible skip ad buttons immediately
        const skipSelectors = [
            '.ytp-ad-skip-button',
            '.ytp-ad-skip-button-modern',
            '.ytp-skip-ad-button',
            '.videoAdUiSkipButton',
            '.ytp-ad-skip-button-slot button',
            'button.ytp-ad-skip-button-modern'
        ];

        for (const selector of skipSelectors) {
            const skipButton = document.querySelector(selector);
            if (skipButton && typeof skipButton.click === 'function') {
                skipButton.click();
                break;
            }
        }

        // Dismiss Anti-Adblock warning modal if present
        const antiAdblockModal = document.querySelector('ytd-enforcement-message-view-model');
        if (antiAdblockModal) {
            const dialog = antiAdblockModal.closest('tp-yt-paper-dialog');
            if (dialog) dialog.remove();
            antiAdblockModal.remove();

            // Resume video if it was paused by YouTube modal
            if (video && video.paused) {
                video.play();
            }
        }
    }

    // Run periodically to catch ad state changes seamlessly
    setInterval(function() {
        injectCSS();
        handleVideoAds();
    }, 250);

    // Also observe DOM changes
    const observer = new MutationObserver(function() {
        handleVideoAds();
    });

    observer.observe(document.body || document.documentElement, {
        childList: true,
        subtree: true
    });

    console.log("YouTube AdBlocker initialized successfully.");
})();
