package org.schabi.newpipe.fragments.list.kiosk;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.schabi.newpipe.error.ErrorInfo;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.util.ExtractorHelper;
import org.schabi.newpipe.util.KioskTranslator;
import org.schabi.newpipe.util.Localization;
import org.schabi.newpipe.util.ServiceHelper;
import org.schabi.newpipe.util.SuggestionEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;

public class DefaultKioskFragment extends KioskFragment {

    private int pageCounter = 1;
    private final Set<String> seenUrls = new HashSet<>();

    @Override
    public Single<KioskInfo> loadResult(final boolean forceReload) {
        final Context ctx = getContext();
        if (ctx != null) {
            contentCountry = Localization.getPreferredContentCountry(ctx);
        }
        pageCounter = 1;
        return SuggestionEngine.loadHomeFeed(serviceId, url, forceReload)
                .onErrorResumeNext(throwable -> ExtractorHelper.getKioskInfo(serviceId, url, forceReload))
                .map(kioskInfo -> {
                    if (!Page.isValid(kioskInfo.getNextPage())) {
                        kioskInfo.setNextPage(new Page("home_feed_page_" + pageCounter++));
                    }
                    return kioskInfo;
                });
    }

    @Override
    public void handleResult(@NonNull final KioskInfo result) {
        seenUrls.clear();
        if (result.getRelatedItems() != null) {
            for (final StreamInfoItem item : result.getRelatedItems()) {
                if (item != null && item.getUrl() != null) {
                    seenUrls.add(item.getUrl());
                }
            }
        }
        super.handleResult(result);
        if (currentNextPage == null || !Page.isValid(currentNextPage)) {
            currentNextPage = new Page("home_feed_page_" + pageCounter++);
        }
        showListFooter(true);
    }

    @Override
    public Single<ListExtractor.InfoItemsPage<StreamInfoItem>> loadMoreItemsLogic() {
        if (currentNextPage != null && !currentNextPage.getId().startsWith("home_feed_page_")) {
            return ExtractorHelper.getMoreKioskItems(serviceId, url, currentNextPage)
                    .onErrorResumeNext(throwable -> fetchMoreHomeFeedItems());
        }
        return fetchMoreHomeFeedItems();
    }

    private Single<ListExtractor.InfoItemsPage<StreamInfoItem>> fetchMoreHomeFeedItems() {
        final int nextPageNum = pageCounter++;
        return SuggestionEngine.fetchRandomVideos(serviceId, 12)
                .onErrorReturnItem(Collections.emptyList())
                .map(items -> {
                    final List<StreamInfoItem> uniqueItems = new ArrayList<>();
                    if (items != null) {
                        for (final StreamInfoItem item : items) {
                            if (item != null && item.getUrl() != null && seenUrls.add(item.getUrl())) {
                                uniqueItems.add(item);
                            }
                        }
                    }
                    return new ListExtractor.InfoItemsPage<>(
                            uniqueItems,
                            new Page("home_feed_page_" + nextPageNum),
                            Collections.emptyList()
                    );
                });
    }

    @Override
    public void handleNextItems(final ListExtractor.InfoItemsPage<StreamInfoItem> result) {
        super.handleNextItems(result);
        if (currentNextPage == null || !Page.isValid(currentNextPage)) {
            currentNextPage = new Page("home_feed_page_" + pageCounter++);
        }
        showListFooter(true);
    }

    @Override
    public boolean hasMoreItems() {
        return true;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (serviceId < 0) {
            updateSelectedDefaultKiosk();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceId != ServiceHelper.getSelectedServiceId(requireContext())) {
            if (currentWorker != null) {
                currentWorker.dispose();
            }
            updateSelectedDefaultKiosk();
            reloadContent();
        }
    }

    private void updateSelectedDefaultKiosk() {
        try {
            serviceId = ServiceHelper.getSelectedServiceId(requireContext());

            final KioskList kioskList = NewPipe.getService(serviceId).getKioskList();
            kioskId = kioskList.getDefaultKioskId();
            url = kioskList.getListLinkHandlerFactoryByType(kioskId).fromId(kioskId).getUrl();

            kioskTranslatedName = KioskTranslator.getTranslatedKioskName(kioskId, requireContext());
            name = kioskTranslatedName;

            currentInfo = null;
            currentNextPage = null;
        } catch (final ExtractionException e) {
            showError(new ErrorInfo(e, UserAction.REQUESTED_KIOSK,
                    "Loading default kiosk for selected service"));
        }
    }
}
