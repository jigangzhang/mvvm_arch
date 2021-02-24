package com.god.seep.media.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import timber.log.Timber
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer


/**
 * Create Time：2021/1/27 on 15:58
 * Description:
 * Author     :zhangjigang 123
 */
class IjkVideoView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    var mMediaPlayer: IMediaPlayer? = null //视频控制类

    //    private var mVideoPlayerListener //自定义监听器
//            : VideoPlayerListener? = null
    private var mSurfaceView: SurfaceView? = null ////播放视图
    private var mPath = "" //视频文件地址


    init {
        isFocusable = true
    }

    fun setPath(path: String) {
        mPath = path
        if (path.isEmpty()) {
            initSurfaceView()
        } else {
            loadVideo()
        }

    }

    private fun initSurfaceView() {
        mSurfaceView = SurfaceView(context)
        mSurfaceView?.let {
            it.holder.addCallback(SurfaceCallBack())
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER)
            it.layoutParams = lp
            this.addView(it)
        }
    }

    private fun loadVideo() {
        if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
        }
        mMediaPlayer = IjkMediaPlayer()
        val ijkMediaPlayer = mMediaPlayer as IjkMediaPlayer
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)

        ijkMediaPlayer.setOnPreparedListener {
            Timber.e("IjkVideoView video prepared")
            start()
        }
        ijkMediaPlayer.setOnErrorListener { iMediaPlayer, i, i2 ->
            Timber.e("IjkVideoView video error, %i, %i", i, i2)
            false
        }
        ijkMediaPlayer.setOnCompletionListener {
            Timber.e("IjkVideoView video completion")
        }

        ijkMediaPlayer.dataSource = mPath
        ijkMediaPlayer.setDisplay(mSurfaceView?.holder)
        ijkMediaPlayer.prepareAsync()
    }

//    fun setListener(listener: VideoPlayerListener?) {
//        mVideoPlayerListener = listener
//        if (mMediaPlayer != null) {
//            mMediaPlayer!!.setOnPreparedListener(listener)
//        }
//    }

    fun isPlaying(): Boolean {
        return if (mMediaPlayer != null) {
            mMediaPlayer!!.isPlaying
        } else false
    }

    fun start() = mMediaPlayer?.start()

    fun pause() = mMediaPlayer?.pause()

    fun stop() = mMediaPlayer?.stop()

    fun reset() = mMediaPlayer?.reset()

    fun release() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    inner class SurfaceCallBack : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            TODO("Not yet implemented")
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            loadVideo()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            TODO("Not yet implemented")
        }
    }
}
