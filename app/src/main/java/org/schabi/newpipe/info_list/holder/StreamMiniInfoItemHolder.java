package org.schabi.newpipe.info_list.holder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.schabi.newpipe.R;
import org.schabi.newpipe.database.stream.model.StreamStateEntity;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.info_list.InfoItemBuilder;
import org.schabi.newpipe.ktx.ViewUtils;
import org.schabi.newpipe.local.history.HistoryRecordManager;
import org.schabi.newpipe.util.DependentPreferenceHelper;
import org.schabi.newpipe.util.Localization;
import org.schabi.newpipe.util.StreamTypeUtil;
import org.schabi.newpipe.util.image.CoilHelper;
import org.schabi.newpipe.views.AnimatedProgressBar;

import java.util.concurrent.TimeUnit;

public class StreamMiniInfoItemHolder extends InfoItemHolder {
    public final ImageView itemThumbnailView;
    public final TextView itemVideoTitleView;
    public final TextView itemUploaderView;
    public final TextView itemDurationView;
    private final AnimatedProgressBar itemProgressView;

    StreamMiniInfoItemHolder(final InfoItemBuilder infoItemBuilder, final int layoutId,
                             final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
        itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
        itemDurationView = itemView.findViewById(R.id.itemDurationView);
        itemProgressView = itemView.findViewById(R.id.itemProgressView);
    }

    public StreamMiniInfoItemHolder(final InfoItemBuilder infoItemBuilder, final ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_stream_mini_item, parent);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem,
                               final HistoryRecordManager historyRecordManager) {
        if (!(infoItem instanceof StreamInfoItem)) {
            return;
        }
        final StreamInfoItem item = (StreamInfoItem) infoItem;

        itemVideoTitleView.setText(item.getName());
        itemUploaderView.setText(item.getUploaderName());

        if (item.getDuration() > 0) {
            itemDurationView.setText(Localization.getDurationString(item.getDuration()));
            itemDurationView.setBackgroundResource(R.drawable.bg_duration_badge);
            itemDurationView.setVisibility(View.VISIBLE);

            StreamStateEntity state2 = null;
            if (DependentPreferenceHelper
                    .getPositionsInListsEnabled(itemProgressView.getContext())) {
                state2 = historyRecordManager.loadStreamState(infoItem)
                        .blockingGet();
            }
            if (state2 != null && state2.getProgressMillis() > 0) {
                final long durationSec = item.getDuration();
                final long progressMillis = state2.getProgressMillis();
                final long progressSec = TimeUnit.MILLISECONDS.toSeconds(progressMillis);
                itemProgressView.setMax((int) durationSec);

                if (state2.isFinished(durationSec) || progressMillis >= durationSec * 950) {
                    itemProgressView.setProgress((int) durationSec);
                } else {
                    final long minVisibleSec = Math.max(1, (long) Math.ceil(durationSec * 0.025));
                    final long displaySec = Math.min(durationSec, Math.max(progressSec, minVisibleSec));
                    itemProgressView.setProgress((int) displaySec);
                }
                itemProgressView.setVisibility(View.VISIBLE);
            } else {
                itemProgressView.setVisibility(View.GONE);
            }
        } else if (StreamTypeUtil.isLiveStream(item.getStreamType())) {
            itemDurationView.setText(R.string.duration_live);
            itemDurationView.setBackgroundResource(R.drawable.bg_live_duration_badge);
            itemDurationView.setVisibility(View.VISIBLE);
            itemProgressView.setVisibility(View.GONE);
        } else {
            itemDurationView.setVisibility(View.GONE);
            itemProgressView.setVisibility(View.GONE);
        }

        // Default thumbnail is shown on error, while loading and if the url is empty
        CoilHelper.INSTANCE.loadThumbnail(itemThumbnailView, item.getThumbnails());

        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnStreamSelectedListener() != null) {
                itemBuilder.getOnStreamSelectedListener().selected(item);
            }
        });

        switch (item.getStreamType()) {
            case AUDIO_STREAM:
            case VIDEO_STREAM:
            case LIVE_STREAM:
            case AUDIO_LIVE_STREAM:
            case POST_LIVE_STREAM:
            case POST_LIVE_AUDIO_STREAM:
                enableLongClick(item);
                break;
            case NONE:
            default:
                disableLongClick();
                break;
        }
    }

    @Override
    public void updateState(final InfoItem infoItem,
                            final HistoryRecordManager historyRecordManager) {
        final StreamInfoItem item = (StreamInfoItem) infoItem;

        StreamStateEntity state = null;
        if (DependentPreferenceHelper.getPositionsInListsEnabled(itemProgressView.getContext())) {
            state = historyRecordManager
                    .loadStreamState(infoItem)
                    .blockingGet();
        }
        if (state != null && item.getDuration() > 0
                && !StreamTypeUtil.isLiveStream(item.getStreamType())
                && state.getProgressMillis() > 0) {
            final long durationSec = item.getDuration();
            final long progressMillis = state.getProgressMillis();
            final long progressSec = TimeUnit.MILLISECONDS.toSeconds(progressMillis);
            itemProgressView.setMax((int) durationSec);

            final long finalProgress;
            if (state.isFinished(durationSec) || progressMillis >= durationSec * 950) {
                finalProgress = durationSec;
            } else {
                final long minVisibleSec = Math.max(1, (long) Math.ceil(durationSec * 0.025));
                finalProgress = Math.min(durationSec, Math.max(progressSec, minVisibleSec));
            }

            if (itemProgressView.getVisibility() == View.VISIBLE) {
                itemProgressView.setProgressAnimated((int) finalProgress);
            } else {
                itemProgressView.setProgress((int) finalProgress);
                ViewUtils.animate(itemProgressView, true, 500);
            }
        } else if (itemProgressView.getVisibility() == View.VISIBLE) {
            ViewUtils.animate(itemProgressView, false, 500);
        }
    }

    private void enableLongClick(final StreamInfoItem item) {
        itemView.setLongClickable(true);
        itemView.setOnLongClickListener(view -> {
            if (itemBuilder.getOnStreamSelectedListener() != null) {
                itemBuilder.getOnStreamSelectedListener().held(item);
            }
            return true;
        });
    }

    private void disableLongClick() {
        itemView.setLongClickable(false);
        itemView.setOnLongClickListener(null);
    }
}
