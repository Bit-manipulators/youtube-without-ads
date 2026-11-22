package org.schabi.newpipe.local.feed.item

import android.content.Context
import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.xwray.groupie.viewbinding.BindableItem
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.schabi.newpipe.MainActivity
import org.schabi.newpipe.R
import org.schabi.newpipe.database.stream.StreamWithState
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.databinding.ListStreamItemBinding
import org.schabi.newpipe.extractor.stream.StreamType.AUDIO_LIVE_STREAM
import org.schabi.newpipe.extractor.stream.StreamType.AUDIO_STREAM
import org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM
import org.schabi.newpipe.extractor.stream.StreamType.POST_LIVE_AUDIO_STREAM
import org.schabi.newpipe.extractor.stream.StreamType.POST_LIVE_STREAM
import org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM
import org.schabi.newpipe.util.Localization
import org.schabi.newpipe.util.NavigationHelper
import org.schabi.newpipe.util.StreamTypeUtil
import org.schabi.newpipe.util.image.CoilHelper

data class StreamItem(
    val streamWithState: StreamWithState,
    var itemVersion: ItemVersion = ItemVersion.NORMAL
) : BindableItem<ListStreamItemBinding>() {
    companion object {
        const val UPDATE_RELATIVE_TIME = 1
    }

    private val stream: StreamEntity = streamWithState.stream
    private val stateProgressTime: Long? = streamWithState.stateProgressMillis

    /**
     * Will be executed at the end of the [StreamItem.bind] (with (ListStreamItemBinding,Int)).
     * Can be used e.g. for highlighting a item.
     */
    var execBindEnd: Consumer<ListStreamItemBinding>? = null

    override fun getId(): Long = stream.uid

    enum class ItemVersion { NORMAL, MINI, GRID, CARD }

    override fun getLayout(): Int = when (itemVersion) {
        ItemVersion.NORMAL -> R.layout.list_stream_card_item
        ItemVersion.MINI -> R.layout.list_stream_mini_item
        ItemVersion.GRID -> R.layout.list_stream_grid_item
        ItemVersion.CARD -> R.layout.list_stream_card_item
    }

    override fun initializeViewBinding(view: View) = ListStreamItemBinding.bind(view)

    override fun bind(viewBinding: ListStreamItemBinding, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(UPDATE_RELATIVE_TIME)) {
            if (itemVersion != ItemVersion.MINI) {
                val details = getStreamInfoDetailLine(viewBinding.itemAdditionalDetails.context)
                if (!TextUtils.isEmpty(details) && !TextUtils.isEmpty(stream.uploader)) {
                    viewBinding.itemAdditionalDetails.text = " • $details"
                } else {
                    viewBinding.itemAdditionalDetails.text = details
                }
            }
            return
        }

        super.bind(viewBinding, position, payloads)
    }

    override fun bind(viewBinding: ListStreamItemBinding, position: Int) {
        viewBinding.itemVideoTitleView.text = stream.title
        viewBinding.itemUploaderView.text = stream.uploader

        if (stream.duration > 0) {
            viewBinding.itemDurationView.text = Localization.getDurationString(stream.duration)
            viewBinding.itemDurationView.setBackgroundResource(R.drawable.bg_duration_badge)
            viewBinding.itemDurationView.visibility = View.VISIBLE

            if (stateProgressTime != null && stateProgressTime > 0) {
                val durationSec = stream.duration
                val progressSec = TimeUnit.MILLISECONDS.toSeconds(stateProgressTime)
                viewBinding.itemProgressView.max = durationSec.toInt()
                if (stateProgressTime >= durationSec * 950) {
                    viewBinding.itemProgressView.progress = durationSec.toInt()
                } else {
                    val minVisibleSec = Math.max(1L, Math.ceil(durationSec * 0.025).toLong())
                    val displaySec = Math.min(durationSec, Math.max(progressSec, minVisibleSec))
                    viewBinding.itemProgressView.progress = displaySec.toInt()
                }
                viewBinding.itemProgressView.visibility = View.VISIBLE
            } else {
                viewBinding.itemProgressView.visibility = View.GONE
            }
        } else if (StreamTypeUtil.isLiveStream(stream.streamType)) {
            viewBinding.itemDurationView.setText(R.string.duration_live)
            viewBinding.itemDurationView.setBackgroundResource(R.drawable.bg_live_duration_badge)
            viewBinding.itemDurationView.visibility = View.VISIBLE
            viewBinding.itemProgressView.visibility = View.GONE
        } else {
            viewBinding.itemDurationView.visibility = View.GONE
            viewBinding.itemProgressView.visibility = View.GONE
        }

        CoilHelper.loadThumbnail(viewBinding.itemThumbnailView, stream.thumbnailUrl)

        if (itemVersion != ItemVersion.MINI) {
            val details = getStreamInfoDetailLine(viewBinding.itemAdditionalDetails.context)
            if (!TextUtils.isEmpty(details) && !TextUtils.isEmpty(stream.uploader)) {
                viewBinding.itemAdditionalDetails.text = " • $details"
            } else {
                viewBinding.itemAdditionalDetails.text = details
            }
        }

        viewBinding.itemUploaderView.setOnClickListener { v ->
            if (!TextUtils.isEmpty(stream.uploaderUrl)) {
                NavigationHelper.openChannelFragmentUsingIntent(
                    v.context, stream.serviceId, stream.uploaderUrl, stream.uploader
                )
            }
        }

        viewBinding.itemUploaderAvatarView.let { avatarView ->
            CoilHelper.loadAvatar(avatarView, null as String?)
            avatarView.visibility = View.VISIBLE
            avatarView.setOnClickListener { v ->
                if (!TextUtils.isEmpty(stream.uploaderUrl)) {
                    NavigationHelper.openChannelFragmentUsingIntent(
                        v.context, stream.serviceId, stream.uploaderUrl, stream.uploader
                    )
                }
            }
        }

        viewBinding.itemMenuButton.setOnClickListener {
            viewBinding.root.performLongClick()
        }

        execBindEnd?.accept(viewBinding)
    }

    override fun isLongClickable() = when (stream.streamType) {
        AUDIO_STREAM, VIDEO_STREAM, LIVE_STREAM, AUDIO_LIVE_STREAM, POST_LIVE_STREAM, POST_LIVE_AUDIO_STREAM -> true
        else -> false
    }

    private fun getStreamInfoDetailLine(context: Context): String {
        var viewsAndDate = ""
        val viewCount = stream.viewCount
        if (viewCount != null && viewCount >= 0) {
            viewsAndDate = when (stream.streamType) {
                AUDIO_LIVE_STREAM -> Localization.listeningCount(context, viewCount)
                LIVE_STREAM -> Localization.shortWatchingCount(context, viewCount)
                else -> Localization.shortViewCount(context, viewCount)
            }
        }
        val uploadDate = getFormattedRelativeUploadDate(context)
        return when {
            !TextUtils.isEmpty(uploadDate) -> when {
                viewsAndDate.isEmpty() -> uploadDate!!
                else -> Localization.concatenateStrings(viewsAndDate, uploadDate)
            }

            else -> viewsAndDate
        }
    }

    private fun getFormattedRelativeUploadDate(context: Context): String? {
        val uploadDate = stream.uploadDate
        return if (uploadDate != null) {
            var formattedRelativeTime = Localization.relativeTime(uploadDate)

            if (MainActivity.DEBUG) {
                val key = context.getString(R.string.show_original_time_ago_key)
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false)) {
                    formattedRelativeTime += " (" + stream.textualUploadDate + ")"
                }
            }

            formattedRelativeTime
        } else {
            stream.textualUploadDate
        }
    }

    override fun getSpanSize(spanCount: Int, position: Int): Int {
        return if (itemVersion == ItemVersion.GRID) 1 else spanCount
    }
}
