package com.god.seep.base.image_video;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.god.seep.base.R;
import com.god.seep.base.adapter.BaseViewPager2Adapter;
import com.god.seep.base.bean.ContentInfo;
import com.god.seep.base.databinding.ItemImageVideoBinding;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

public class ImageVideoAdapter extends BaseViewPager2Adapter<ItemImageVideoBinding, ContentInfo> implements Player.EventListener {
    private final boolean showZoom;
    private SimpleExoPlayer mPlayer;
    private int videoPage = -1;
    private View iv_play;
    private View iv_pause;
    private OnZoomListener listener;

    public ImageVideoAdapter(boolean showZoom) {
        super(R.layout.item_image_video);
        this.showZoom = showZoom;
    }

    @Override
    protected void bindItem(ItemImageVideoBinding binding, ContentInfo item) {
        binding.setUrl(item.getUrl());
        binding.setIsVideo(item.getType() != 1);
        if (item.getType() == 1) {
            Glide.with(binding.image)
                    .asBitmap()
                    .load(item.getUrl())
                    .into(new BitmapImageViewTarget(binding.image));
        } else if (item.getType() != 1) {
            videoPage = getItemPosition(item);
            initVideo(binding, item.getUrl());
        }
    }

    private void initVideo(ItemImageVideoBinding binding, String path) {
        if (TextUtils.isEmpty(path)) return;
        mPlayer = new SimpleExoPlayer.Builder(getContext()).build();
        binding.video.setPlayer(mPlayer);

        DefaultDataSourceFactory factory = new DefaultDataSourceFactory(getContext());
        //http://v.jiemian.com/25/d1/25d166c99cc70bdf3da03f4866f4188b_256.mp4，测试链接
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(path));
        mPlayer.setMediaSource(mediaSource);
        mPlayer.addListener(this);
        mPlayer.prepare();
        iv_play = binding.video.findViewById(R.id.exo_play_small);
        iv_pause = binding.video.findViewById(R.id.exo_pause_small);
        iv_play.setOnClickListener(v -> binding.video.findViewById(R.id.exo_play).performClick());
        iv_pause.setOnClickListener(v -> binding.video.findViewById(R.id.exo_pause).performClick());
        View zoom = binding.video.findViewById(R.id.exo_zoom);
        if (showZoom)
            zoom.setOnClickListener(v -> {
                if (listener != null)
                    listener.onZoom();
            });
        else
            zoom.setVisibility(View.GONE);
        updatePlayPauseButton(iv_play, iv_pause);
    }

    public void selectPage(int position) {
        if (mPlayer != null) {
            if (videoPage == position) {
                updatePlayPauseButton(iv_play, iv_pause);
            } else {
                if (mPlayer.isPlaying())
                    iv_pause.performClick();
            }
        }
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        updatePlayPauseButton(iv_play, iv_pause);
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        updatePlayPauseButton(iv_play, iv_pause);
    }

    private void updatePlayPauseButton(View play, View pause) {
        boolean requestPlayPauseFocus = false;
        boolean shouldShowPauseButton = shouldShowPauseButton();
        if (play != null) {
            requestPlayPauseFocus |= shouldShowPauseButton && play.isFocused();
            play.setVisibility(shouldShowPauseButton ? View.GONE : View.VISIBLE);
        }
        if (pause != null) {
            requestPlayPauseFocus |= !shouldShowPauseButton && pause.isFocused();
            pause.setVisibility(shouldShowPauseButton ? View.VISIBLE : View.GONE);
        }
        if (requestPlayPauseFocus) {
            requestPlayPauseFocus(play, pause);
        }
    }

    private boolean shouldShowPauseButton() {
        return mPlayer != null
                && mPlayer.getPlaybackState() != Player.STATE_ENDED
                && mPlayer.getPlaybackState() != Player.STATE_IDLE
                && mPlayer.getPlayWhenReady();
    }

    private void requestPlayPauseFocus(View play, View pause) {
        boolean shouldShowPauseButton = shouldShowPauseButton();
        if (!shouldShowPauseButton && play != null) {
            play.requestFocus();
        } else if (shouldShowPauseButton && pause != null) {
            pause.requestFocus();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull BaseViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        recyclePlayer();
    }

    public void recyclePlayer() {
        if (mPlayer != null) {
            mPlayer.removeListener(this);
            mPlayer.release();
            mPlayer = null;
        }
    }

    public void setZoomListener(OnZoomListener listener) {
        this.listener = listener;
    }

    public interface OnZoomListener {
        void onZoom();
    }
}
