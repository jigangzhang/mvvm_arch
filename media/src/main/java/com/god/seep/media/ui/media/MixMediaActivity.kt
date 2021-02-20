package com.god.seep.media.ui.media

import android.graphics.SurfaceTexture
import android.view.TextureView
import com.god.seep.base.BaseApplication
import com.god.seep.base.arch.view.BaseActivity
import com.god.seep.base.arch.viewmodel.BaseViewModel
import com.god.seep.media.R
import com.god.seep.media.codec.AsyncAudioDecode
import com.god.seep.media.codec.AsyncVideoDecode
import com.god.seep.media.codec.AudioDecodeSync
import com.god.seep.media.codec.VideoDecode
import com.god.seep.media.databinding.ActivityMixMediaBinding
import com.god.seep.media.video.Extractor
import com.god.seep.media.video.Muxer
import com.god.seep.media.video.MuxerListener
import java.util.*

class MixMediaActivity : BaseActivity<ActivityMixMediaBinding, BaseViewModel>() {

    private lateinit var videoDecode: VideoDecode
    private lateinit var audioDecode: AudioDecodeSync
    private lateinit var asyncVideoDecode: AsyncVideoDecode
    private lateinit var asyncAudioDecode: AsyncAudioDecode

    //    private val path = "https://yc-shop-dev.oss-cn-shenzhen.aliyuncs.com/secretUpload/20210122/a91b0cf4-a0a8-43c6-9c68-f1bec6ad1168.mp4"
    private val path = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4"

    override fun getLayoutId(): Int {
        return R.layout.activity_mix_media
    }

    override fun createViewModel(): BaseViewModel {
        return getViewModel(BaseViewModel::class.java)
    }

    override fun initData() {
        val mediaExtractor = Extractor()
        mediaExtractor.reset(path)
        mBinding.tvFormat.text = mediaExtractor.allFormat
    }

    private fun play() {
        videoDecode = VideoDecode(mBinding.texture)
        videoDecode.reset(path)
//                asyncVideoDecode = AsyncVideoDecode()
//                asyncVideoDecode.reset(path)
//                asyncVideoDecode.start(mBinding.texture)
//        audioDecode = AudioDecodeSync()
//        audioDecode.reset(path)
        asyncAudioDecode = AsyncAudioDecode()
        asyncAudioDecode.reset(path)
        asyncAudioDecode.start()
        BaseApplication.getInstance().threadPool.execute(videoDecode)
//        BaseApplication.getInstance().threadPool.execute(audioDecode)
    }

    override fun registerEvent() {
        mBinding.texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                play()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                videoDecode.stop()
//                asyncVideoDecode.destroy()
//                audioDecode.stop()
                asyncAudioDecode.destroy()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

        }
        mBinding.btnChoose.setOnClickListener {
            val muxer = Muxer(this, object : MuxerListener {
                override fun onStart() {
                    runOnUiThread { mBinding.tvPath.text = "${Date()}：视频文件合成中..." }
                }

                override fun onSuccess() {
                    runOnUiThread {
                        mBinding.tvPath.text = "${Date()}：视频文件合成结束"
                        mBinding.btnChoose.isEnabled = true
                    }
                }

                override fun onFailed() {
                    runOnUiThread { mBinding.tvPath.text = "${Date()}：视频文件合成失败" }
                }
            })
            muxer.setDataSource(path)
            muxer.start()
            mBinding.btnChoose.isEnabled = false
        }
    }
}