/**
 * Clean & High-Performance YouTube Ad Blocker & Background Player
 * Bundled inside WebExtension for 100% ad elimination
 * - Zero DASH tampering (zero buffering / playback glitch)
 * - Pure CSS ad hiding for all mobile banners, promoted items, mastheads
 * - Fast-forward 16x skip + mute + auto-skip click for video ads
 * - Seamless background audio playback
 */
(function() {
    'use strict';

    if (window.__yt_adfree_enhancer_installed) return;
    window.__yt_adfree_enhancer_installed = true;

    const adStyles = `
        /* YouTube Mobile (m.youtube.com) and Desktop ad elements */
        ytm-promoted-sparkles-web-renderer,
        ytm-ad-slot-renderer,
        ytm-companion-ad-renderer,
        ytm-promoted-sparkles-text-search-renderer,
        ytm-promoted-video-renderer,
        ytm-compact-promoted-item-renderer,
        ytm-feed-ad-metadata-renderer,
        ytm-inline-ad-renderer,
        ytm-statement-banner-renderer,
        ytm-search-ad-renderer,
        ytm-masthead,
        .ytm-ad-badge,
        div[ad-slot],
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
        .ytp-ad-image-overlay,
        .ytp-ad-module,
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
        if (document.getElementById('yt-adfree-styles')) return;
        const style = document.createElement('style');
        style.id = 'yt-adfree-styles';
        style.textContent = adStyles;
        (document.head || document.documentElement).appendChild(style);
    }

    if (document.head || document.documentElement) {
        injectCSS();
    } else {
        document.addEventListener('DOMContentLoaded', injectCSS);
    }

    // Keep background audio active
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
    } catch (e) {}

    // Auto-skip video ads smoothly without buffering
    const skipSelectors = [
        '.ytp-ad-skip-button',
        '.ytp-ad-skip-button-modern',
        '.ytp-skip-ad-button',
        '.videoAdUiSkipButton',
        '.ytp-ad-skip-button-slot button',
        'button.ytp-ad-skip-button-modern',
        '.ytm-skip-ad-button',
        '.ytp-ad-skip-button-container button',
        'button[class*="skip-button"]',
        'button[class*="ytp-ad-skip"]',
        '.ytp-ad-overlay-close-button',
        '[aria-label*="Skip ad" i]',
        '[aria-label*="Skip ads" i]',
        'button.ytp-ad-skip-button-text'
    ];

    function isAdShowing() {
        const moviePlayer = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
        if (moviePlayer && moviePlayer.classList) {
            if (moviePlayer.classList.contains('ad-showing') ||
                moviePlayer.classList.contains('ad-interrupting')) {
                return true;
            }
        }
        return !!document.querySelector(
            '.ad-showing, .ad-interrupting, .video-ads, .ytp-ad-player-overlay, ' +
            '.ytp-ad-preview-container, .ytp-ad-text, ytm-ad-slot-renderer, [class*="ad-showing"]'
        );
    }

    function checkAndClean() {
        injectCSS();

        // 1. Click skip buttons
        for (const selector of skipSelectors) {
            const buttons = document.querySelectorAll(selector);
            buttons.forEach(function(btn) {
                if (btn && typeof btn.click === 'function') {
                    btn.click();
                }
            });
        }

        // 2. Player skip API
        const moviePlayer = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
        if (moviePlayer && typeof moviePlayer.skipAd === 'function') {
            try { moviePlayer.skipAd(); } catch (e) {}
        }

        // 3. Fast-forward video ad at 16x speed and mute
        const adActive = isAdShowing();
        const videos = document.querySelectorAll('video');

        videos.forEach(function(video) {
            if (adActive) {
                video.muted = true;
                video.playbackRate = 16.0;
            } else {
                if (video.playbackRate > 2.0) {
                    video.playbackRate = 1.0;
                }
            }
        });

        // 4. Dismiss anti-adblock modal if any
        const antiAdblockModal = document.querySelector('ytd-enforcement-message-view-model');
        if (antiAdblockModal) {
            const dialog = antiAdblockModal.closest('tp-yt-paper-dialog');
            if (dialog) dialog.remove();
            antiAdblockModal.remove();

            videos.forEach(function(v) {
                if (v && v.paused) v.play();
            });
        }
    }

    setInterval(checkAndClean, 200);

    const observer = new MutationObserver(checkAndClean);
    if (document.body || document.documentElement) {
        observer.observe(document.body || document.documentElement, {
            childList: true,
            subtree: true
        });
    } else {
        document.addEventListener('DOMContentLoaded', function() {
            observer.observe(document.body || document.documentElement, {
                childList: true,
                subtree: true
            });
        });
    }
})();
