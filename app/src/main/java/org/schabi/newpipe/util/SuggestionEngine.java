/*
 * SPDX-FileCopyrightText: 2026 AdFreeYT contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.util;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.schabi.newpipe.App;
import org.schabi.newpipe.NewPipeDatabase;
import org.schabi.newpipe.database.AppDatabase;
import org.schabi.newpipe.database.history.model.StreamHistoryEntry;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import io.reactivex.rxjava3.core.Single;

/**
 * Engine responsible for personalized recommendations and filtering live streams.
 * Incorporates user search history and watch history for relevant suggestions.
 */
public final class SuggestionEngine {
    private static final String TAG = "SuggestionEngine";

    private static final List<StreamInfoItem> RECOMMENDATION_BUFFER =
            Collections.synchronizedList(new ArrayList<>());

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
        try {
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
        } catch (final Exception e) {
            return false;
        }
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
     * Extracts interest topics from the user's search history and watch history.
     *
     * @return list of interest search queries
     */
    public static List<String> getUserInterestQueries() {
        final List<String> interests = new ArrayList<>();
        try {
            final App app = App.getInstance();
            if (app != null) {
                final AppDatabase db = NewPipeDatabase.getInstance(app);

                // 1. Unique search history queries
                final List<String> searches = db.searchHistoryDAO()
                        .getUniqueEntries(15)
                        .firstElement()
                        .onErrorReturnItem(Collections.emptyList())
                        .blockingGet();
                if (searches != null) {
                    for (final String s : searches) {
                        if (!TextUtils.isEmpty(s) && !interests.contains(s.trim())) {
                            interests.add(s.trim());
                        }
                    }
                }

                // 2. Watch history channels and keywords
                final List<StreamHistoryEntry> streamHistory = db.streamHistoryDAO()
                        .getHistory()
                        .firstElement()
                        .onErrorReturnItem(Collections.emptyList())
                        .blockingGet();
                if (streamHistory != null) {
                    int count = 0;
                    for (final StreamHistoryEntry entry : streamHistory) {
                        if (entry != null && entry.getStreamEntity() != null) {
                            final String uploader = entry.getStreamEntity().getUploader();
                            if (!TextUtils.isEmpty(uploader) && !interests.contains(uploader.trim())) {
                                interests.add(uploader.trim());
                                count++;
                            }
                            final String title = entry.getStreamEntity().getTitle();
                            if (!TextUtils.isEmpty(title)) {
                                final String clean = title.replaceAll("[^a-zA-Z0-9 ]", " ").trim();
                                if (clean.length() >= 4 && clean.length() <= 40 && !interests.contains(clean)) {
                                    interests.add(clean);
                                    count++;
                                }
                            }
                        }
                        if (count >= 15) {
                            break;
                        }
                    }
                }
            }
        } catch (final Exception e) {
            Log.w(TAG, "Could not fetch user history for suggestions", e);
        }
        return interests;
    }

    /**
     * Retrieves the next suggestion topic, balancing user history interests (75%)
     * with curated discovery topics (25%).
     *
     * @return query string
     */
    public static String getNextSuggestionTopic() {
        final List<String> userInterests = getUserInterestQueries();
        if (!userInterests.isEmpty()) {
            final boolean pickHistory = ThreadLocalRandom.current().nextInt(100) < 75;
            if (pickHistory) {
                final int idx = ThreadLocalRandom.current().nextInt(userInterests.size());
                return userInterests.get(idx);
            }
        }
        final int idx = ThreadLocalRandom.current().nextInt(DISCOVERY_TOPICS.length);
        return DISCOVERY_TOPICS[idx];
    }

    /**
     * Searches YouTube for videos matching the given query and removes live streams.
     *
     * @param serviceId service identifier
     * @param query search query
     * @return single containing non-live stream items
     */
    public static Single<List<StreamInfoItem>> fetchVideosForQuery(final int serviceId,
                                                                   final String query) {
        return ExtractorHelper.searchFor(serviceId, query,
                Collections.singletonList("videos"), "")
                .map(SearchInfo::getRelatedItems)
                .map(items -> {
                    final List<StreamInfoItem> candidates = new ArrayList<>();
                    if (items != null) {
                        for (final InfoItem item : items) {
                            if (item instanceof StreamInfoItem) {
                                final StreamInfoItem streamItem = (StreamInfoItem) item;
                                if (!isLiveStreamItem(streamItem)) {
                                    candidates.add(streamItem);
                                }
                            }
                        }
                    }
                    Collections.shuffle(candidates);
                    return candidates;
                })
                .onErrorReturnItem(Collections.emptyList());
    }

    /**
     * Fetches personalized recommendation videos, balancing user history and discovery topics,
     * using an in-memory buffer pool and resilient fallbacks so scrolling never stalls.
     *
     * @param serviceId streaming service identifier
     * @param seenUrls set of already seen video URLs (or null)
     * @param limit maximum number of items to return
     * @return single containing list of stream items
     */
    public static Single<List<StreamInfoItem>> fetchRecommendations(final int serviceId,
                                                                    final Set<String> seenUrls,
                                                                    final int limit) {
        return Single.fromCallable(() -> {
            final List<StreamInfoItem> buffered = new ArrayList<>();
            synchronized (RECOMMENDATION_BUFFER) {
                final Iterator<StreamInfoItem> it = RECOMMENDATION_BUFFER.iterator();
                while (it.hasNext() && buffered.size() < limit) {
                    final StreamInfoItem item = it.next();
                    it.remove();
                    if (item != null && item.getUrl() != null) {
                        if (seenUrls == null || seenUrls.add(item.getUrl())) {
                            buffered.add(item);
                        }
                    }
                }
            }
            return buffered;
        }).flatMap(buffered -> {
            if (buffered.size() >= limit) {
                return Single.just(buffered);
            }

            final String topic1 = getNextSuggestionTopic();
            return fetchVideosForQuery(serviceId, topic1)
                    .onErrorReturnItem(Collections.emptyList())
                    .map(list1 -> {
                        final List<StreamInfoItem> result = new ArrayList<>(buffered);
                        final List<StreamInfoItem> extra = new ArrayList<>();

                        if (list1 != null) {
                            for (final StreamInfoItem item : list1) {
                                if (item != null && item.getUrl() != null) {
                                    if (seenUrls == null || seenUrls.add(item.getUrl())) {
                                        if (result.size() < limit) {
                                            result.add(item);
                                        } else {
                                            extra.add(item);
                                        }
                                    }
                                }
                            }
                        }

                        if (!extra.isEmpty()) {
                            synchronized (RECOMMENDATION_BUFFER) {
                                if (RECOMMENDATION_BUFFER.size() < 100) {
                                    RECOMMENDATION_BUFFER.addAll(extra);
                                }
                            }
                        }

                        return result;
                    });
        }).onErrorReturn(throwable -> {
            Log.w(TAG, "Error fetching recommendations", throwable);
            return Collections.emptyList();
        });
    }

    /**
     * Fetches random non-live videos from YouTube.
     *
     * @param serviceId streaming service identifier
     * @param limit maximum number of random videos to retrieve
     * @return single containing list of random video items
     */
    public static Single<List<StreamInfoItem>> fetchRandomVideos(final int serviceId,
                                                                final int limit) {
        return fetchRecommendations(serviceId, null, limit);
    }

    /**
     * Loads the Home Feed:
     * 1. Fetches trending kiosk
     * 2. Removes live stream clutter (like live news, etc.)
     * 3. Replaces the live section with personalized history/discovery recommendations
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

                    return fetchRecommendations(serviceId, null, 25)
                            .onErrorReturnItem(Collections.emptyList())
                            .map(randomVideos -> {
                                final List<StreamInfoItem> finalFeed = new ArrayList<>();
                                final Set<String> seenUrls = new HashSet<>();

                                // Insert personalized recommendations at the top
                                for (final StreamInfoItem item : randomVideos) {
                                    if (item.getUrl() != null && seenUrls.add(item.getUrl())) {
                                        finalFeed.add(item);
                                    }
                                }

                                // Append non-live trending videos
                                for (final StreamInfoItem item : cleanTrending) {
                                    if (item.getUrl() != null && seenUrls.add(item.getUrl())) {
                                        finalFeed.add(item);
                                    } else if (item.getUrl() != null) {
                                        synchronized (RECOMMENDATION_BUFFER) {
                                            if (RECOMMENDATION_BUFFER.size() < 60) {
                                                RECOMMENDATION_BUFFER.add(item);
                                            }
                                        }
                                    }
                                }

                                kioskInfo.setRelatedItems(finalFeed);
                                return kioskInfo;
                            });
                });
    }
}
