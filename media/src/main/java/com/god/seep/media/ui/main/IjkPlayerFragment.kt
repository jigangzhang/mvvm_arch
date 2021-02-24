package com.god.seep.media.ui.main

import com.god.seep.base.arch.view.BaseFragment
import com.god.seep.media.R
import com.god.seep.media.databinding.IjkplayerFragmentBinding
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * Create Time：2021/1/27 on 15:27
 * Description:
 * Author     :zhangjigang 123
 */
class IjkPlayerFragment : BaseFragment<IjkplayerFragmentBinding, MediaViewModel>() {

    override fun getLayoutId(): Int {
        return R.layout.ijkplayer_fragment
    }

    override fun createViewModel(): MediaViewModel {
        return getViewModel(MediaViewModel::class.java)
    }

    override fun initData() {
        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin("libijkplayer.so")
//        mBinding.ijkVideo.setPath("http://yc-shop-dev.oss-cn-shenzhen.aliyuncs.com/secretUpload/20210122/a91b0cf4-a0a8-43c6-9c68-f1bec6ad1168.mp4")
//        mBinding.ijkVideo.setPath("http://yc-shop-dev.oss-cn-shenzhen.aliyuncs.com/product_images/10e1e1f8-bac9-406a-b89b-eba57bf924fc.mp4")
        mBinding.ijkVideo.setPath("http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4")
        mBinding.ijkVideo.start()
    }

    override fun registerEvent() {

    }
}