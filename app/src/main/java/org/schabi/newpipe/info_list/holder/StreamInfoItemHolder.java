package org.schabi.newpipe.info_list.holder;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.info_list.InfoItemBuilder;
import org.schabi.newpipe.local.history.HistoryRecordManager;
import org.schabi.newpipe.util.Localization;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.image.CoilHelper;

public class StreamInfoItemHolder extends StreamMiniInfoItemHolder {
    @Nullable
    public final TextView itemAdditionalDetails;
    @Nullable
    public final ImageView itemUploaderAvatarView;
    @Nullable
    public final View itemMenuButton;

    public StreamInfoItemHolder(final InfoItemBuilder infoItemBuilder, final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_stream_card_item, parent);
    }

    public StreamInfoItemHolder(final InfoItemBuilder infoItemBuilder, final int layoutId,
                                final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
        itemAdditionalDetails = itemView.findViewById(R.id.itemAdditionalDetails);
        itemUploaderAvatarView = itemView.findViewById(R.id.itemUploaderAvatarView);
        itemMenuButton = itemView.findViewById(R.id.itemMenuButton);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem,
                               final HistoryRecordManager historyRecordManager) {
        super.updateFromItem(infoItem, historyRecordManager);

        if (!(infoItem instanceof StreamInfoItem)) {
            return;
        }
        final StreamInfoItem item = (StreamInfoItem) infoItem;

        final String details = getStreamInfoDetailLine(item);
        if (itemAdditionalDetails != null) {
            if (!TextUtils.isEmpty(details)) {
                if (!TextUtils.isEmpty(item.getUploaderName())) {
                    itemAdditionalDetails.setText(" • " + details);
                } else {
                    itemAdditionalDetails.setText(details);
                }
            } else {
                itemAdditionalDetails.setText("");
            }
        }

        if (itemUploaderView != null) {
            itemUploaderView.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(item.getUploaderUrl())) {
                    NavigationHelper.openChannelFragmentUsingIntent(
                            v.getContext(), item.getServiceId(),
                            item.getUploaderUrl(), item.getUploaderName());
                }
            });
        }

        if (itemUploaderAvatarView != null) {
            if (item.getUploaderAvatars() != null && !item.getUploaderAvatars().isEmpty()) {
                CoilHelper.INSTANCE.loadAvatar(itemUploaderAvatarView, item.getUploaderAvatars());
                itemUploaderAvatarView.setVisibility(View.VISIBLE);
            } else if (!TextUtils.isEmpty(item.getUploaderUrl())) {
                CoilHelper.INSTANCE.loadAvatar(itemUploaderAvatarView, (String) null);
                itemUploaderAvatarView.setVisibility(View.VISIBLE);
            } else {
                itemUploaderAvatarView.setVisibility(View.GONE);
            }

            itemUploaderAvatarView.setOnClickListener(view -> {
                if (!TextUtils.isEmpty(item.getUploaderUrl())) {
                    NavigationHelper.openChannelFragmentUsingIntent(
                            view.getContext(), item.getServiceId(),
                            item.getUploaderUrl(), item.getUploaderName());
                }
            });
        }

        if (itemMenuButton != null) {
            itemMenuButton.setOnClickListener(view -> {
                if (itemBuilder.getOnStreamSelectedListener() != null) {
                    itemBuilder.getOnStreamSelectedListener().held(item);
                }
            });
        }
    }

    private String getStreamInfoDetailLine(final StreamInfoItem infoItem) {
        String viewsAndDate = "";
        if (infoItem.getViewCount() >= 0) {
            if (infoItem.getStreamType().equals(StreamType.AUDIO_LIVE_STREAM)) {
                viewsAndDate = Localization
                        .listeningCount(itemBuilder.getContext(), infoItem.getViewCount());
            } else if (infoItem.getStreamType().equals(StreamType.LIVE_STREAM)) {
                viewsAndDate = Localization
                        .shortWatchingCount(itemBuilder.getContext(), infoItem.getViewCount());
            } else {
                viewsAndDate = Localization
                        .shortViewCount(itemBuilder.getContext(), infoItem.getViewCount());
            }
        }

        final String uploadDate = Localization.relativeTimeOrTextual(itemBuilder.getContext(),
                infoItem.getUploadDate(),
                infoItem.getTextualUploadDate());
        if (!TextUtils.isEmpty(uploadDate)) {
            if (viewsAndDate.isEmpty()) {
                return uploadDate;
            }

            return Localization.concatenateStrings(viewsAndDate, uploadDate);
        }

        return viewsAndDate;
    }
}
