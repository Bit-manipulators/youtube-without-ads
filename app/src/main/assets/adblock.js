/**
 * Clean & High-Performance YouTube Ad Blocker & Background Player
 * - Zero prototype tampering (Preserves video streaming data with ZERO buffering)
 * - Pure CSS ad hiding for all mobile & desktop banners and promoted items
 * - Fast-forward video ad skipping (16x speed + mute + auto-skip click)
 * - Background audio playback
 */
(function() {
    'use strict';

    if (window.__yt_adblock_clean_installed) return;
    window.__yt_adblock_clean_installed = true;

    // =========================================================================
    // 1. CSS RULES: Hide all banners, sponsored items, and ad containers
    // =========================================================================
    const adStyles = `
        /* Mobile Web YouTube (m.youtube.com) */
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
        /* Desktop & Hybrid elements */
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

    // =========================================================================
    // 2. BACKGROUND AUDIO PLAYBACK: Spoof visibilityState on YouTube
    // =========================================================================
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

    // =========================================================================
    // 3. ZERO-BUFFERING VIDEO AD SKIPPING
    // =========================================================================
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

    function isAdPlaying() {
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

    function checkAndSkipAds() {
        injectCSS();

        // 1. Auto-click skip buttons immediately if available
        for (const selector of skipSelectors) {
            const buttons = document.querySelectorAll(selector);
            buttons.forEach(function(btn) {
                if (btn && typeof btn.click === 'function') {
                    btn.click();
                }
            });
        }

        // 2. Try native YouTube player API skip
        const moviePlayer = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
        if (moviePlayer && typeof moviePlayer.skipAd === 'function') {
            try { moviePlayer.skipAd(); } catch (e) {}
        }

        // 3. Fast-forward ad at 16x speed and mute WITHOUT seeking (Zero buffering)
        const adActive = isAdPlaying();
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

    // Run every 200ms
    setInterval(checkAndSkipAds, 200);

    // Observe DOM mutations
    const observer = new MutationObserver(checkAndSkipAds);
    observer.observe(document.body || document.documentElement, {
        childList: true,
        subtree: true
    });
})();
