package com.god.seep.media.ui.camera

import android.os.Environment
import android.view.OrientationEventListener
import android.view.Surface
import android.view.SurfaceHolder
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.god.seep.base.arch.view.BaseActivity
import com.god.seep.base.arch.viewmodel.BaseViewModel
import com.god.seep.media.R
import com.god.seep.media.audio.format
import com.god.seep.media.databinding.ActivityCameraBinding
import com.god.seep.media.video.Camera2Record
import com.god.seep.media.video.CameraRecord
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CameraActivity : BaseActivity<ActivityCameraBinding, BaseViewModel>() {

    private lateinit var cameraRecord: CameraRecord
    private lateinit var camera2Record: Camera2Record
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProvider: ProcessCameraProvider
    private var isBackCamera = false

    override fun getLayoutId(): Int {
        return R.layout.activity_camera
    }

    override fun createViewModel(): BaseViewModel {
        return getViewModel(BaseViewModel::class.java)
    }

    override fun initData() {
        val orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture.targetRotation = rotation
            }
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            //将相机的生命周期和activity的生命周期绑定，cameraX 会自己释放
            cameraProvider = cameraProviderFuture.get()
            //预览的 capture，它里面支持角度换算
            val preview = Preview.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            //创建图片的 capture
            imageCapture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_AUTO).build()
            orientationEventListener.enable()
            //选择摄像头
            cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

            //预览之前先解绑
            cameraProvider.unbindAll()
            //将数据绑定到相机的生命周期中
            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview, imageCapture)
            //将previewView 的 surface 给相机预览
            preview.setSurfaceProvider(mBinding.preview.surfaceProvider)

        }, ContextCompat.getMainExecutor(this))

//        cameraRecord = CameraRecord()
//        camera2Record = Camera2Record(this)
//        mBinding.surface.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
////                cameraRecord.openCamera()
////                cameraRecord.adjustCameraOrientation(this@CameraActivity)
//                camera2Record.openCamera(mBinding.surface)
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
////                cameraRecord.startPreview(holder, width, height)
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
////                cameraRecord.closeCamera()
//                camera2Record.closeCamera()
//            }
//        })
    }

    private fun takePicture() {
        if (imageCapture != null) {
            val storeDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photo = File(storeDir, "${Date().format()}.jpg")
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photo).build()
            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Timber.e("CameraX 拍照成功：${outputFileResults.savedUri.toString()}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Timber.e("CameraX 拍照失败")
                }
            })
        }
    }

    private fun switchCamera() {
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
//        imageCapture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_AUTO).build()
        cameraSelector = CameraSelector.Builder().requireLensFacing(if (isBackCamera) {
            isBackCamera = false
            CameraSelector.LENS_FACING_FRONT
        } else {
            isBackCamera = true
            CameraSelector.LENS_FACING_BACK
        }).build()
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview, imageCapture)
        //将previewView 的 surface 给相机预览
        preview.setSurfaceProvider(mBinding.preview.surfaceProvider)
    }

    override fun registerEvent() {
        mBinding.camera.setOnClickListener {
//            cameraRecord.takePicture(this)
//            camera2Record.takePicture(mBinding.surface)
            takePicture()
        }
        mBinding.tvSwitch.setOnClickListener {
//            cameraRecord.switchCamera(mBinding.surface)
//            camera2Record.switchCamera(mBinding.surface)
            switchCamera()
        }
    }
}