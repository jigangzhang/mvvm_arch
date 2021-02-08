package com.god.seep.media.video

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.Camera
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import timber.log.Timber
import java.lang.IllegalArgumentException
import kotlin.math.abs


class CameraRecord {
    //参考：https://blog.csdn.net/u011418943/article/details/107256406
    private var frontCameraId: Int = -1
    private var backCameraId: Int = -1
    private lateinit var frontCameraInfo: Camera.CameraInfo
    private lateinit var backCameraInfo: Camera.CameraInfo
    private lateinit var camera: Camera
    private var currentId: Int = -1
    private lateinit var currentInfo: Camera.CameraInfo

    init {
        val numberOfCameras = Camera.getNumberOfCameras()   //获取相机个数
        Timber.e("camera numbers: $numberOfCameras")
        for (i: Int in 0 until numberOfCameras) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(i, cameraInfo) //获取相机信息
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == cameraInfo.facing) {
                frontCameraId = i
                frontCameraInfo = cameraInfo
            } else if (Camera.CameraInfo.CAMERA_FACING_BACK == cameraInfo.facing) {
                backCameraId = i
                backCameraInfo = cameraInfo
            }
        }
    }

    fun openCamera() {
//        Camera.open()
        camera = when {
            frontCameraId != -1 -> {
                currentId = frontCameraId
                currentInfo = frontCameraInfo
                Camera.open(frontCameraId)
            }
            backCameraId != -1 -> {
                currentId = backCameraId
                currentInfo = backCameraInfo
                Camera.open(backCameraId)
            }
            else -> throw IllegalArgumentException("没有可用摄像头")
        }
    }

    fun switchCamera(surface: SurfaceView) {
        camera ?: return
        closeCamera()
        if (currentId != -1) {
            if (currentId == frontCameraId && backCameraId != -1) {
                currentId = backCameraId
                currentInfo = backCameraInfo
                camera = Camera.open(currentId)
            } else if (currentId == backCameraId && frontCameraId != -1) {
                currentId = frontCameraId
                currentInfo = frontCameraInfo
                camera = Camera.open(currentId)
            } else {
                throw IllegalArgumentException("切换摄像头不可用，没有另一个摄像头")
            }
        }
        adjustCameraOrientation(surface.context)
        startPreview(surface.holder, surface.width, surface.height)
    }

    private fun initPreviewParams(shortSize: Int, longSize: Int) {
        camera ?: return
        val parameters = camera.parameters
        //获取手机支持的尺寸
        val sizes = parameters.supportedPreviewSizes
        if (currentId != frontCameraId) {
            //设置聚焦，小米前置摄像头不支持（其他手机未知）
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        val bestSize = getBestSize(shortSize, longSize, sizes)
        //设置预览大小
        parameters.setPreviewSize(bestSize.width, bestSize.height)
        //设置图片大小，拍照
        parameters.setPictureSize(bestSize.width, bestSize.height)
        //设置格式，所有相机都支持 NV21格式
        parameters.previewFormat = ImageFormat.NV21
        //支持不用预览也可以拍照
        parameters.set("no-display-mode", "1")
        camera.parameters = parameters
    }

    /**
     * 获取预览最佳尺寸
     * 应该根据自己UI的大小去设置相机预览的大小，如果你的控件为 200x200，但相机的数据为 1920x1080 ，这样填充过去，画面肯定是会被拉伸的
     * 拿到手机相机支持的所有尺寸；所以，我们需要找到比例相同，或者近似的大小，跟UI配合，这样画面才不会拉伸，注意相机的 width > height
     * 当 UI 的比例跟相机支持的比例相同，直接返回，否则则找近似的
     */
    private fun getBestSize(shortSize: Int, longSize: Int, sizes: List<Camera.Size>): Camera.Size {
        //默认取一个
        var bestSize: Camera.Size = sizes[0]
        val uiRatio = (longSize / shortSize).toFloat()
        var minRatio = uiRatio
        for (previewSize: Camera.Size in sizes) {
            val cameraRatio = (previewSize.width / previewSize.height).toFloat()
            //如果找不到比例相同的，找一个最近的,防止预览变形
            val offset = abs(cameraRatio - minRatio)
            if (offset < minRatio) {
                minRatio = offset
                bestSize = previewSize
            }
            //比例相同
            if (uiRatio == cameraRatio) {
                bestSize = previewSize
                break
            }
        }
        return bestSize
    }

    fun startPreview(holder: SurfaceHolder, width: Int, height: Int) {
        camera ?: return
        //配置camera参数
        initPreviewParams(width, height)
        camera.setPreviewDisplay(holder)
        //开始预览
        camera.startPreview()
    }

    fun adjustCameraOrientation(context: Context) {
        val cameraInfo: Camera.CameraInfo = currentInfo
        //判断当前横竖屏
        val rotation = context.display?.rotation
        var degress = 0
        //获取手机方向
        when (rotation) {
            Surface.ROTATION_0 -> degress = 0
            Surface.ROTATION_90 -> degress = 90
            Surface.ROTATION_180 -> degress = 180
            Surface.ROTATION_270 -> degress = 270
        }
        var result = 0
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = (cameraInfo.orientation - degress + 360) % 360
        } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degress) % 360
            result = (360 - result) % 360
        }
        camera.setDisplayOrientation(result)
    }

    fun takePicture() {
        camera.takePicture({}, null, { data, camera ->
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        })
    }

    fun closeCamera() {
        camera.stopPreview()
        camera.release()
    }
}

fun cameraInfo() {

}