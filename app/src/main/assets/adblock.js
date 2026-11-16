/**
 * Smooth YouTube Ad Skipper & Enhancer for Mobile & Desktop Web
 * - Intercepts JSON player responses to strip ad placements at root
 * - Hides all mobile & desktop ad banners and promo cards
 * - Smooth single-action video ad skip (prevents glitching and buffering loops)
 * - Restores normal playback rate seamlessly
 * - Preserves background playback
 */
(function() {
    'use strict';

    if (window.__yt_adblock_smooth_installed) return;
    window.__yt_adblock_smooth_installed = true;

    // =========================================================================
    // 1. ROOT INTERCEPTION: Strip player ads from YouTube Player JSON Responses
    // =========================================================================
    function cleanAdData(obj) {
        if (!obj || typeof obj !== 'object') return obj;
        if (obj.playerAds) delete obj.playerAds;
        if (obj.adPlacements) delete obj.adPlacements;
        if (obj.adSlots) delete obj.adSlots;
        if (obj.adBreakHeartbeatParams) delete obj.adBreakHeartbeatParams;
        if (obj.playbackTracking) {
            delete obj.playbackTracking.videostatsPlaybackUrl;
            delete obj.playbackTracking.videostatsDelayplayUrl;
            delete obj.playbackTracking.videostatsWatchtimeUrl;
        }
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
                if (url.includes('/youtubei/v1/player') || url.includes('/youtubei/v1/next')) {
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

    // Intercept XMLHttpRequest for player API
    try {
        const originalXHROpen = XMLHttpRequest.prototype.open;
        const originalXHRSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.open = function(method, url) {
            this._url = url;
            return originalXHROpen.apply(this, arguments);
        };
        XMLHttpRequest.prototype.send = function() {
            if (this._url && (this._url.includes('/player') || this._url.includes('/next'))) {
                this.addEventListener('readystatechange', function() {
                    if (this.readyState === 4) {
                        try {
                            const data = JSON.parse(this.responseText);
                            cleanAdData(data);
                            Object.defineProperty(this, 'responseText', { value: JSON.stringify(data) });
                            Object.defineProperty(this, 'response', { value: JSON.stringify(data) });
                        } catch (e) {}
                    }
                });
            }
            return originalXHRSend.apply(this, arguments);
        };
    } catch (e) {}

    // =========================================================================
    // 2. CSS STYLES: Hide banners, overlays, and hide video opacity while skipping
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
        /* While ad is detected, make video transparent so user sees NO glitch or flicker */
        .ad-showing video,
        .ad-interrupting video,
        .video-ads video {
            opacity: 0 !important;
            pointer-events: none !important;
        }
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
    // 4. SMOOTH VIDEO AD SKIPPER (NO SEEK LOOPS, NO GLITCHING)
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

    let lastSkippedTime = 0;

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
            '.ytp-ad-preview-container, .ytp-ad-text, ytm-ad-slot-renderer, ' +
            '[class*="ad-showing"], [class*="ad-interrupting"]'
        );
    }

    function smoothSkipAd() {
        injectCSS();

        const adActive = isAdShowing();
        const video = document.querySelector('video');
        const moviePlayer = document.getElementById('movie_player') || document.querySelector('.html5-video-player');

        // Always trigger skip buttons if present
        for (const selector of skipSelectors) {
            const buttons = document.querySelectorAll(selector);
            buttons.forEach(function(btn) {
                if (btn && typeof btn.click === 'function') {
                    btn.click();
                }
            });
        }

        // Use native YouTube Player API method if available
        if (moviePlayer) {
            try {
                if (typeof moviePlayer.skipAd === 'function') {
                    moviePlayer.skipAd();
                }
            } catch (e) {}
        }

        if (adActive && video) {
            const now = Date.now();
            video.muted = true;
            video.playbackRate = 16.0;

            // Seek ONCE per ad segment with a 1-second debounce to prevent decoder buffering loops!
            if (now - lastSkippedTime > 1000) {
                lastSkippedTime = now;
                if (!isNaN(video.duration) && isFinite(video.duration) && video.duration > 0.5) {
                    // Seek to 0.1s before end so the player cleanly reaches ended state
                    video.currentTime = video.duration - 0.1;
                }
            }
        } else if (!adActive && video) {
            // Restore normal speed when ad is finished
            if (video.playbackRate > 2.0) {
                video.playbackRate = 1.0;
            }
        }

        // Dismiss anti-adblock modal if any
        const antiAdblockModal = document.querySelector('ytd-enforcement-message-view-model');
        if (antiAdblockModal) {
            const dialog = antiAdblockModal.closest('tp-yt-paper-dialog');
            if (dialog) dialog.remove();
            antiAdblockModal.remove();

            if (video && video.paused) video.play();
        }
    }

    // Hook HTMLMediaElement.prototype.play to attach per-video listeners
    try {
        const originalPlay = HTMLMediaElement.prototype.play;
        HTMLMediaElement.prototype.play = function() {
            if (!this.__smooth_ad_hooked) {
                this.__smooth_ad_hooked = true;
                this.addEventListener('timeupdate', function() {
                    if (isAdShowing()) {
                        smoothSkipAd();
                    }
                });
            }
            return originalPlay.apply(this, arguments);
        };
    } catch (e) {}

    // Run periodically every 200ms
    setInterval(smoothSkipAd, 200);

    // Observe DOM changes
    const observer = new MutationObserver(smoothSkipAd);
    observer.observe(document.body || document.documentElement, {
        childList: true,
        subtree: true
    });
})();
