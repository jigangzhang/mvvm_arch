package com.god.seep.media.ui.main

import android.content.pm.ActivityInfo
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import com.god.seep.base.arch.view.BaseFragment
import com.god.seep.base.util.CacheManager
import com.god.seep.media.R
import com.god.seep.media.databinding.MediaFragmentBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.video.VideoListener
import java.io.File

class MediaFragment : BaseFragment<MediaFragmentBinding, MediaViewModel>(), Player.EventListener, VideoListener {

    private var isLandscape: Boolean = true
    private lateinit var iv_pause_full: ImageView
    private lateinit var iv_play_full: ImageView
    private lateinit var iv_pause: ImageView
    private lateinit var iv_play: ImageView
    private lateinit var mPlayer: SimpleExoPlayer
    private lateinit var cache: SimpleCache

    companion object {
        fun newInstance() = MediaFragment()
    }

    override fun getLayoutId(): Int {
        return R.layout.media_fragment
    }

    override fun createViewModel(): MediaViewModel {
        return getViewModel(MediaViewModel::class.java)
    }

    override fun initData() {
        initVideo("http://v.jiemian.com/25/d1/25d166c99cc70bdf3da03f4866f4188b_256.mp4")
    }

    private fun initVideo(path: String) {
        if (TextUtils.isEmpty(path)) return
        activity!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        mPlayer = SimpleExoPlayer.Builder(mContext).build()
        mBinding.video.player = mPlayer
        val factory = CacheDataSource.Factory()
        val dir: File = File(context?.externalCacheDir, CacheManager.VIDEO_CACHE)
        cache = SimpleCache(dir, NoOpCacheEvictor(), ExoDatabaseProvider(context!!))
        factory.setCache(cache)
        val upstreamFactory = DefaultDataSourceFactory(context!!)
        factory.setUpstreamDataSourceFactory(upstreamFactory)
        //http://v.jiemian.com/25/d1/25d166c99cc70bdf3da03f4866f4188b_256.mp4，测试链接
        val mediaSource = ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(path))
        mPlayer.setMediaSource(mediaSource)
        mPlayer.addVideoListener(this)
        mPlayer.addListener(this)
        mPlayer.prepare()
        iv_play = mBinding.video.findViewById(R.id.exo_play_small)
        iv_pause = mBinding.video.findViewById(R.id.exo_pause_small)
        iv_play_full = mBinding.fullVideo.findViewById(R.id.exo_play_small)
        iv_pause_full = mBinding.fullVideo.findViewById(R.id.exo_pause_small)
        iv_play.setOnClickListener { mBinding.video.findViewById<ImageView>(R.id.exo_play).performClick() }
        iv_pause.setOnClickListener { mBinding.video.findViewById<ImageView>(R.id.exo_pause).performClick() }
        iv_play_full.setOnClickListener { mBinding.fullVideo.findViewById<ImageView>(R.id.exo_play).performClick() }
        iv_pause_full.setOnClickListener { mBinding.fullVideo.findViewById<ImageView>(R.id.exo_pause).performClick() }
        mBinding.video.findViewById<ImageView>(R.id.exo_zoom).setOnClickListener {
            if (isLandscape) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            mBinding.fullVideo.visibility = View.VISIBLE
            mBinding.fullVideo.player = mPlayer
            mBinding.video.player = null
            mBinding.video.visibility = View.GONE
            updatePlayPauseButton(iv_play_full, iv_pause_full)
        }
        mBinding.fullVideo.findViewById<ImageView>(R.id.exo_zoom).setOnClickListener { v ->
            if (isLandscape) activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            mBinding.fullVideo.visibility = View.GONE
            mBinding.fullVideo.player = null
            mBinding.video.visibility = View.VISIBLE
            mBinding.video.player = mPlayer
            updatePlayPauseButton(iv_play, iv_pause)
        }
        updatePlayPauseButton(iv_play, iv_pause)
    }

    override fun registerEvent() {

    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        isLandscape = height < width
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (mBinding.fullVideo.visibility === View.VISIBLE) updatePlayPauseButton(iv_play_full, iv_pause_full) else updatePlayPauseButton(iv_play, iv_pause)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (mBinding.fullVideo.visibility === View.VISIBLE) updatePlayPauseButton(iv_play_full, iv_pause_full) else updatePlayPauseButton(iv_play, iv_pause)
    }

    private fun updatePlayPauseButton(play: View?, pause: View?) {
        var requestPlayPauseFocus = false
        val shouldShowPauseButton = shouldShowPauseButton()
        if (play != null) {
            requestPlayPauseFocus = requestPlayPauseFocus or (shouldShowPauseButton && play.isFocused)
            play.visibility = if (shouldShowPauseButton) View.GONE else View.VISIBLE
        }
        if (pause != null) {
            requestPlayPauseFocus = requestPlayPauseFocus or (!shouldShowPauseButton && pause.isFocused)
            pause.visibility = if (shouldShowPauseButton) View.VISIBLE else View.GONE
        }
        if (requestPlayPauseFocus) {
            requestPlayPauseFocus(play, pause)
        }
    }

    private fun shouldShowPauseButton(): Boolean {
        return mPlayer != null && mPlayer.playbackState != Player.STATE_ENDED && mPlayer.playbackState != Player.STATE_IDLE && mPlayer.playWhenReady
    }

    private fun requestPlayPauseFocus(play: View?, pause: View?) {
        val shouldShowPauseButton = shouldShowPauseButton()
        if (!shouldShowPauseButton && play != null) {
            play.requestFocus()
        } else if (shouldShowPauseButton && pause != null) {
            pause.requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mPlayer != null) mPlayer.play()
    }

    override fun onPause() {
        super.onPause()
        if (mPlayer != null) mPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mPlayer != null) {
            mPlayer.removeListener(this)
            mPlayer.removeVideoListener(this)
            mPlayer.release()
            cache.release()
        }
    }

    //todo:必须由activity控制吗
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mBinding.fullVideo.visibility === View.VISIBLE) {
                mBinding.fullVideo.findViewById<ImageView>(R.id.exo_zoom).performClick()
                return true
            }
        }
        return false
//        return super.onKeyDown(keyCode, event)
    }
}