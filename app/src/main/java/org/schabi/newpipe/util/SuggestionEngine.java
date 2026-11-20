/*
 * SPDX-FileCopyrightText: 2026 AdFreeYT contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.util;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import io.reactivex.rxjava3.core.Single;

/**
 * Engine responsible for home feed recommendations and filtering live streams.
 */
public final class SuggestionEngine {
    private static final String TAG = "SuggestionEngine";

    /**
     * Curated list of discovery topics for random recommendations on the home page.
     */
    private static final String[] DISCOVERY_TOPICS = {
        "Documentary",
        "Science",
        "Technology",
        "Nature 4K",
        "Gaming",
        "Animation",
        "Space Exploration",
        "World History",
        "Travel Documentary",
        "DIY Engineering",
        "Cooking Street Food",
        "Wildlife",
        "Interesting Facts",
        "Short Film",
        "Physics Experiments",
        "Deep Ocean",
        "Architecture Design"
    };

    private SuggestionEngine() {
    }

    /**
     * Determines whether a given StreamInfoItem is a live stream or ended live stream.
     *
     * @param item the item to check
     * @return true if the item is a live stream
     */
    public static boolean isLiveStreamItem(final StreamInfoItem item) {
        if (item == null) {
            return false;
        }

        final StreamType streamType = item.getStreamType();
        if (StreamTypeUtil.isLiveStream(streamType)) {
            return true;
        }

        if (streamType == StreamType.LIVE_STREAM
                || streamType == StreamType.AUDIO_LIVE_STREAM
                || streamType == StreamType.POST_LIVE_STREAM) {
            return true;
        }

        if (item.getDuration() <= 0) {
            return true;
        }

        final String name = item.getName();
        if (!TextUtils.isEmpty(name)) {
            final String lower = name.toLowerCase();
            if (lower.contains(" live ") || lower.startsWith("live ") || lower.endsWith(" live")
                    || lower.contains("livestream") || lower.contains("live stream")
                    || lower.contains("🔴") || name.contains("सीधा LIVE")
                    || name.contains("LIVE|")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Filters out all live stream items from the provided list.
     *
     * @param items original stream items
     * @return non-live stream items
     */
    @NonNull
    public static List<StreamInfoItem> filterLiveStreams(final List<StreamInfoItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        final List<StreamInfoItem> filtered = new ArrayList<>(items.size());
        for (final StreamInfoItem item : items) {
            if (!isLiveStreamItem(item)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Fetches random non-live videos from YouTube using rotating discovery topics.
     *
     * @param serviceId streaming service identifier
     * @param limit maximum number of random videos to retrieve
     * @return single containing list of random video items
     */
    public static Single<List<StreamInfoItem>> fetchRandomVideos(final int serviceId,
                                                                final int limit) {
        return Single.fromCallable(() -> {
            final int index = ThreadLocalRandom.current().nextInt(DISCOVERY_TOPICS.length);
            return DISCOVERY_TOPICS[index];
        }).flatMap(topic -> ExtractorHelper.searchFor(serviceId, topic,
                Collections.singletonList("videos"), "")
                .map(SearchInfo::getRelatedItems)
                .map(items -> {
                    final List<StreamInfoItem> randomCandidates = new ArrayList<>();
                    if (items != null) {
                        for (final InfoItem item : items) {
                            if (item instanceof StreamInfoItem) {
                                final StreamInfoItem streamItem = (StreamInfoItem) item;
                                if (!isLiveStreamItem(streamItem)) {
                                    randomCandidates.add(streamItem);
                                }
                            }
                        }
                    }
                    Collections.shuffle(randomCandidates);
                    final int count = Math.min(limit, randomCandidates.size());
                    final List<StreamInfoItem> result =
                            new ArrayList<>(randomCandidates.subList(0, count));
                    return result;
                }))
                .onErrorReturn(throwable -> {
                    Log.w(TAG, "Error fetching random videos, falling back", throwable);
                    return Collections.emptyList();
                });
    }

    /**
     * Loads the Home Feed:
     * 1. Fetches trending kiosk
     * 2. Removes live stream clutter (like live news, etc.)
     * 3. Replaces the live section with fresh random videos
     *
     * @param serviceId service identifier
     * @param url kiosk URL
     * @param forceReload force cache reload
     * @return single containing transformed KioskInfo
     */
    public static Single<KioskInfo> loadHomeFeed(final int serviceId,
                                                final String url,
                                                final boolean forceReload) {
        return ExtractorHelper.getKioskInfo(serviceId, url, forceReload)
                .flatMap(kioskInfo -> {
                    final List<StreamInfoItem> cleanTrending =
                            filterLiveStreams(kioskInfo.getRelatedItems());

                    return fetchRandomVideos(serviceId, 8)
                            .onErrorReturnItem(Collections.emptyList())
                            .map(randomVideos -> {
                                final List<StreamInfoItem> finalFeed = new ArrayList<>();
                                final Set<String> seenUrls = new HashSet<>();

                                // Insert random videos at the top (replacing live section)
                                for (final StreamInfoItem item : randomVideos) {
                                    if (item.getUrl() != null && seenUrls.add(item.getUrl())) {
                                        finalFeed.add(item);
                                    }
                                }

                                // Append non-live trending videos
                                for (final StreamInfoItem item : cleanTrending) {
                                    if (item.getUrl() != null && seenUrls.add(item.getUrl())) {
                                        finalFeed.add(item);
                                    }
                                }

                                kioskInfo.setRelatedItems(finalFeed);
                                return kioskInfo;
                            });
                });
    }
}
