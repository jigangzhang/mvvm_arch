package com.god.seep.media

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.god.seep.base.arch.view.BaseActivity
import com.god.seep.media.databinding.MediaActivityBinding
import com.god.seep.media.ui.main.MediaFragment
import com.god.seep.media.ui.main.MediaViewModel

@Route(path = "/media/activity")
class MediaActivity : BaseActivity<MediaActivityBinding, MediaViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MediaFragment.newInstance())
                    .commitNow()
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.media_activity
    }

    override fun createViewModel(): MediaViewModel {
        return getViewModel(MediaViewModel::class.java)
    }

    override fun initData() {

    }

    override fun registerEvent() {
        TODO("Not yet implemented")
    }
}