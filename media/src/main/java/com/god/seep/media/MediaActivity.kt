package com.god.seep.media

import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.god.seep.base.arch.view.BaseActivity
import com.god.seep.media.databinding.MediaActivityBinding
import com.god.seep.media.ui.main.GSYPlayerFragment
import com.god.seep.media.ui.main.IjkPlayerFragment
import com.god.seep.media.ui.main.MediaFragment
import com.god.seep.media.ui.main.MediaViewModel

@Route(path = "/media/activity")
class MediaActivity : BaseActivity<MediaActivityBinding, MediaViewModel>() {

    override fun getLayoutId(): Int {
        return R.layout.media_activity
    }

    override fun createViewModel(): MediaViewModel {
        return getViewModel(MediaViewModel::class.java)
    }

    override fun initData() {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, GSYPlayerFragment())
                    .commitNow()
    }

    override fun registerEvent() {

    }

    fun exo(view: View) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, MediaFragment())
                .commitNow()

    }

    fun ijk(view: View) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, IjkPlayerFragment())
                .commitNow()

    }

    fun gsy(view: View) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, GSYPlayerFragment())
                .commitNow()
    }
}