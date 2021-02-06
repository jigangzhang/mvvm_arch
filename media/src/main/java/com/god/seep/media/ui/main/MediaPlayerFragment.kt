package com.god.seep.media.ui.main

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.widget.MediaController
import com.god.seep.base.arch.view.BaseFragment
import com.god.seep.media.R
import com.god.seep.media.databinding.MediaplayerFragmentBinding
import timber.log.Timber
import kotlin.reflect.KProperty

/**
 * Create Time：2021/2/4 on 16:12
 * Description:
 * Author     :zhangjigang 123
 */
class MediaPlayerFragment : BaseFragment<MediaplayerFragmentBinding, MediaViewModel>() {

    override fun getLayoutId(): Int {
        return R.layout.mediaplayer_fragment
    }

    override fun createViewModel(): MediaViewModel {
        return getViewModel(MediaViewModel::class.java)
    }

    override fun initData() {
        val source = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4"
//        val source = "https://yc-shop-dev.oss-cn-shenzhen.aliyuncs.com/secretUpload/20210122/a91b0cf4-a0a8-43c6-9c68-f1bec6ad1168.mp4"
        mediaWithController(source)
        mediaWithSurface(source)
        surfaceWithController(source)
        textureWithController(source)
    }

    private fun mediaWithController(source: String) {
        val controller = MediaController(context)
        mBinding.video.setMediaController(controller)
        mBinding.video.requestFocus()
        mBinding.video.setVideoPath(source)
    }

    private fun mediaWithSurface(source: String) {
        //各种控制可以参考 MediaController，比如 seekTo 的进度控制等
        val player = MediaPlayer()
        mBinding.surface.setZOrderOnTop(false)
//        mBinding.surface.holder.setSizeFromLayout()
        mBinding.surface.holder.setFixedSize(640, 360)
//        mBinding.surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mBinding.surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Timber.e("surfaceCreated")
                player.setDisplay(holder)
                player.prepareAsync()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Timber.e("format: %s, width: %s, height: %s", format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Timber.e("surfaceDestroyed")
                player.release()
            }
        })
        player.setDataSource(source)
        player.setOnCompletionListener { Timber.e("OnCompletion") }
        player.setOnErrorListener { mp, what, extra ->
            Timber.e("OnError, type: %s, extra: %s, 错误码详解见注释", what, extra)
            true
        }
        player.setOnVideoSizeChangedListener { mp, width, height ->
            Timber.e("videoSize change, width: %s, height: %s", width, height)
        }
        player.setOnInfoListener { mp, what, extra ->
            Timber.e("OnInfo or warning, type: %s, extra: %s, 警告码详解见注释", what, extra)
            false
        }
        player.setOnSeekCompleteListener { Timber.e("OnSeekComplete") }
        val counter = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                player.apply {
                    mBinding.duration.text = "${currentPosition / 1000}s--${duration / 1000}s"
                    if (isPlaying)
                        sendEmptyMessageDelayed(0, 999)
                }
            }
        }
        val play = {
            player.start()
            mBinding.play.setImageResource(R.mipmap.common_video_pause)
            mBinding.duration.text = "${player.currentPosition / 1000}s--${player.duration / 1000}s"
            counter.sendEmptyMessage(0)
        }
        player.setOnPreparedListener {
            Timber.e("OnPrepared")
//            play()
        }
        mBinding.play.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                mBinding.play.setImageResource(R.mipmap.common_video_play)
            } else {
                play()
            }
        }
//        player.setOnDrmPreparedListener { mp, status ->  }
        player.setOnBufferingUpdateListener { mp, percent -> Timber.e("OnBufferingUpdate, 缓冲进度-percent: %d", percent) }
    }

    private fun surfaceWithController(source: String) {
        val player = MediaPlayer()
        val controller = MediaController(context)
        controller.setAnchorView(mBinding.thirdSurface)
        mBinding.thirdSurface.setZOrderOnTop(false)
        mBinding.thirdSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                player.setDisplay(holder)
                player.prepareAsync()
                controller.show()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                player.release()
            }
        })
        player.setDataSource(source)
        mBinding.thirdSurface.setOnClickListener { controller.show() }
//        val playerController: MediaController.MediaPlayerControl by player
        controller.setMediaPlayer(object : MediaController.MediaPlayerControl {
            override fun start() {
                player.start()
            }

            override fun pause() {
                player.pause()
            }

            override fun getDuration(): Int {
                return player.duration
            }

            override fun getCurrentPosition(): Int {
                return player.currentPosition
            }

            override fun seekTo(pos: Int) {
                player.seekTo(pos)
            }

            override fun isPlaying(): Boolean {
                return player.isPlaying
            }

            override fun getBufferPercentage(): Int {
                return 0
            }

            override fun canPause(): Boolean {
                return true
            }

            override fun canSeekForward(): Boolean {
                return true
            }

            override fun getAudioSessionId(): Int {
                return 0
            }

            override fun canSeekBackward(): Boolean {
                return true
            }
        })
        controller.isEnabled = true
    }

    /**
     * TextureView 可使用各种动画效果（平移、缩放、旋转等），SurfaceView不能
     * TextureView 可以放在ListView或者ScrollView中（可以使用View.setAlpha()等），SurfaceView不能
     * TextureView 只能用在硬件加速的窗口，TextureView比SurfaceView更耗内存，且可能会有1~3帧的延迟
     */
    private fun textureWithController(source: String) {
        val player = MediaPlayer()
        player.setDataSource(source)
        player.setOnPreparedListener { player.start() }
        mBinding.texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                player.setSurface(Surface(surface))
                player.prepareAsync()
                Timber.e("onSurfaceTextureAvailable, width: %d, height: %d", width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Timber.e("SurfaceTextureSizeChanged, width: %d, height: %d", width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Timber.e("onSurfaceTextureDestroyed")
                surface.release()
                player.release()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                //mBinding.texture.bitmap   //截屏
                Timber.e("onSurfaceTextureUpdated")
            }
        }
    }

    override fun registerEvent() {

    }
}

//private operator fun MediaPlayer.getValue(nothing: Nothing?, property: KProperty<*>): MediaController.MediaPlayerControl {
//    return
//}
