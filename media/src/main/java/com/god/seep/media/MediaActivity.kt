package com.god.seep.media

import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Route
import com.god.seep.base.arch.view.BaseActivity
import com.god.seep.media.databinding.MediaActivityBinding
import com.god.seep.media.ui.audio.AudioActivity
import com.god.seep.media.ui.camera.CameraActivity
import com.god.seep.media.ui.main.*
import com.god.seep.media.ui.media.MixMediaActivity

@Route(path = "/media/activity")
class MediaActivity : BaseActivity<MediaActivityBinding, MediaViewModel>() {

    override fun getLayoutId(): Int {
        return R.layout.media_activity
    }

    override fun createViewModel(): MediaViewModel {
        return getViewModel(MediaViewModel::class.java)
    }

    override fun initData() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, GSYPlayerFragment())
                .commitNow()
    }

    override fun registerEvent() {
        mBinding.speaker.setOnClickListener { startActivity(Intent(this, AudioActivity::class.java)) }
        mBinding.camera.setOnClickListener { startActivity(Intent(this, CameraActivity::class.java)) }
        mBinding.media.setOnClickListener { startActivity(Intent(this, MixMediaActivity::class.java)) }
    }

    fun exo(view: View) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, MediaFragment())
                .commitNow()

    }

    fun ijk(view: View) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, MediaPlayerFragment())
                .commitNow()

    }

    fun gsy(view: View) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, GSYPlayerFragment())
                .commitNow()
    }
}