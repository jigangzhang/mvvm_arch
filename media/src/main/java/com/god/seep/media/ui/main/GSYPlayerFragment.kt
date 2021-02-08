package com.god.seep.media.ui.main

import android.content.pm.ActivityInfo
import android.view.View
import android.widget.ImageView
import com.god.seep.base.arch.view.BaseFragment
import com.god.seep.media.R
import com.god.seep.media.databinding.GsyplayerFragmentBinding
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.OrientationUtils

/**
 * Create Time：2021/1/31 on 14:37
 * Description:
 * Author     :zhangjigang 123
 */
class GSYPlayerFragment : BaseFragment<GsyplayerFragmentBinding, MediaViewModel>() {
    private lateinit var orientationUtils: OrientationUtils

    override fun getLayoutId(): Int {
        return R.layout.gsyplayer_fragment
    }

    override fun createViewModel(): MediaViewModel {
        return getViewModel(MediaViewModel::class.java)
    }

    override fun initData() {
//        val source1 = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4"
//        val source1 = "file:///storage/emulated/0/Android/data/com.god.seep.media/files/Music/0207181814.wav"
        val source1 = "https://yc-shop-dev.oss-cn-shenzhen.aliyuncs.com/secretUpload/20210122/a91b0cf4-a0a8-43c6-9c68-f1bec6ad1168.mp4"
        mBinding.player.setUp(source1, true, "测试视频")
        val thumb = ImageView(context)
        thumb.scaleType = ImageView.ScaleType.CENTER_CROP
        mBinding.player.thumbImageView = thumb
        mBinding.player.titleTextView.visibility = View.VISIBLE
        mBinding.player.backButton.visibility = View.VISIBLE
        orientationUtils = OrientationUtils(activity, mBinding.player)
        mBinding.player.fullscreenButton.setOnClickListener { orientationUtils.resolveByClick() }
        mBinding.player.setIsTouchWiget(true)
        mBinding.player.backButton.setOnClickListener {
            if (orientationUtils.screenType == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mBinding.player.fullscreenButton.performClick()
            }
        }
        mBinding.player.startPlayLogic()
    }

    override fun registerEvent() {

    }

    override fun onPause() {
        super.onPause()
        mBinding.player.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        mBinding.player.onVideoResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        GSYVideoManager.releaseAllVideos()
        orientationUtils.releaseListener()
        mBinding.player.setVideoAllCallBack(null)
    }
}