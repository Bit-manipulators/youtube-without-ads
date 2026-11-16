/**
 * Comprehensive YouTube Ad Blocker & Enhancer Script for Mobile & Desktop Web
 * - Intercepts JSON player responses to strip ad placements at the root
 * - Hides all mobile & desktop ad banners, promo cards, and sparkles
 * - Auto-fast-forwards and skips video ads instantly
 * - Preserves background playback
 */
(function() {
    'use strict';

    if (window.__yt_adblock_installed) return;
    window.__yt_adblock_installed = true;

    // =========================================================================
    // 1. ROOT INTERCEPTION: Strip player ads from YouTube Player JSON Responses
    // =========================================================================
    function cleanAdData(obj) {
        if (!obj || typeof obj !== 'object') return obj;
        if (obj.playerAds) delete obj.playerAds;
        if (obj.adPlacements) delete obj.adPlacements;
        if (obj.adSlots) delete obj.adSlots;
        if (obj.adBreakHeartbeatParams) delete obj.adBreakHeartbeatParams;
        return obj;
    }

    // Intercept window.ytInitialPlayerResponse
    let _ytInitialPlayerResponse = window.ytInitialPlayerResponse;
    try {
        Object.defineProperty(window, 'ytInitialPlayerResponse', {
            get: function() { return _ytInitialPlayerResponse; },
            set: function(val) {
                _ytInitialPlayerResponse = cleanAdData(val);
            },
            configurable: true
        });
        if (_ytInitialPlayerResponse) {
            cleanAdData(_ytInitialPlayerResponse);
        }
    } catch (e) {}

    // Intercept JSON.parse
    const originalJSONParse = JSON.parse;
    JSON.parse = function() {
        const result = originalJSONParse.apply(this, arguments);
        if (result && typeof result === 'object') {
            cleanAdData(result);
        }
        return result;
    };

    // Intercept window.fetch for /youtubei/v1/player endpoints
    if (window.fetch) {
        const originalFetch = window.fetch;
        window.fetch = async function(...args) {
            const response = await originalFetch.apply(this, args);
            try {
                const url = typeof args[0] === 'string' ? args[0] : (args[0] && args[0].url ? args[0].url : '');
                if (url.includes('/youtubei/v1/player')) {
                    const clone = response.clone();
                    const text = await clone.text();
                    try {
                        const data = JSON.parse(text);
                        cleanAdData(data);
                        return new Response(JSON.stringify(data), {
                            status: response.status,
                            statusText: response.statusText,
                            headers: response.headers
                        });
                    } catch (err) {
                        return response;
                    }
                }
            } catch (err) {}
            return response;
        };
    }

    // =========================================================================
    // 2. CSS STYLES: Completely hide banner ads, sponsored cards, and overlays
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
        /* Desktop & Video Player elements */
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
    // 3. BACKGROUND AUDIO PLAYBACK: Spoof visibilityState on YouTube
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
    // 4. VIDEO AD AUTO-SKIP & INSTANT FAST-FORWARD
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

    function isAdShowing() {
        const moviePlayer = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
        if (moviePlayer && moviePlayer.classList) {
            if (moviePlayer.classList.contains('ad-showing') ||
                moviePlayer.classList.contains('ad-interrupting')) {
                return true;
            }
        }

        const adElement = document.querySelector(
            '.ad-showing, .ad-interrupting, .video-ads, .ytp-ad-player-overlay, ' +
            '.ytp-ad-preview-container, .ytp-ad-text, ytm-ad-slot-renderer, ' +
            '[class*="ad-showing"], [class*="ad-interrupting"]'
        );
        return !!adElement;
    }

    function handleVideoAds() {
        injectCSS();

        const adActive = isAdShowing();
        const videos = document.querySelectorAll('video');

        if (adActive) {
            videos.forEach(function(video) {
                video.muted = true;
                video.playbackRate = 16.0;
                if (!isNaN(video.duration) && isFinite(video.duration) && video.duration > 0) {
                    video.currentTime = video.duration;
                }
            });
        }

        // Click any visible skip button immediately
        for (const selector of skipSelectors) {
            const buttons = document.querySelectorAll(selector);
            buttons.forEach(function(btn) {
                if (btn && typeof btn.click === 'function') {
                    btn.click();
                }
            });
        }

        // Dismiss anti-adblock modal if any
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

    // Hook HTMLMediaElement.prototype.play to attach per-video listeners
    try {
        const originalPlay = HTMLMediaElement.prototype.play;
        HTMLMediaElement.prototype.play = function() {
            if (!this.__adblock_hooked) {
                this.__adblock_hooked = true;
                this.addEventListener('timeupdate', function() {
                    if (isAdShowing()) {
                        this.muted = true;
                        this.playbackRate = 16.0;
                        if (!isNaN(this.duration) && isFinite(this.duration) && this.duration > 0) {
                            this.currentTime = this.duration;
                        }
                        for (const selector of skipSelectors) {
                            const btn = document.querySelector(selector);
                            if (btn && typeof btn.click === 'function') btn.click();
                        }
                    }
                });
            }
            return originalPlay.apply(this, arguments);
        };
    } catch (e) {}

    // Run periodically every 150ms
    setInterval(handleVideoAds, 150);

    // Observe DOM mutations
    const observer = new MutationObserver(handleVideoAds);
    observer.observe(document.body || document.documentElement, {
        childList: true,
        subtree: true
    });

    console.log("YouTube AdBlocker active.");
})();
