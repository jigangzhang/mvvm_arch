package com.god.seep.media.ui.camera

import android.view.SurfaceHolder
import com.god.seep.base.arch.view.BaseActivity
import com.god.seep.base.arch.viewmodel.BaseViewModel
import com.god.seep.media.R
import com.god.seep.media.databinding.ActivityCameraBinding
import com.god.seep.media.video.Camera2Record
import com.god.seep.media.video.CameraRecord

class CameraActivity : BaseActivity<ActivityCameraBinding, BaseViewModel>() {

    private lateinit var cameraRecord: CameraRecord
    private lateinit var camera2Record: Camera2Record

    override fun getLayoutId(): Int {
        return R.layout.activity_camera
    }

    override fun createViewModel(): BaseViewModel {
        return getViewModel(BaseViewModel::class.java)
    }

    override fun initData() {
//        cameraRecord = CameraRecord()
        camera2Record = Camera2Record(this)
        mBinding.surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
//                cameraRecord.openCamera()
//                cameraRecord.adjustCameraOrientation(this@CameraActivity)
                camera2Record.openCamera(mBinding.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                cameraRecord.startPreview(holder, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                cameraRecord.closeCamera()
                camera2Record.closeCamera()
            }
        })
    }

    override fun registerEvent() {
        mBinding.camera.setOnClickListener {
//            cameraRecord.takePicture(this)
            camera2Record.takePicture(mBinding.surface)
        }
        mBinding.tvSwitch.setOnClickListener {
//            cameraRecord.switchCamera(mBinding.surface)
            camera2Record.switchCamera(mBinding.surface)
        }
    }
}