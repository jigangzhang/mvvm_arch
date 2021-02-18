package com.god.seep.media.video

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Environment
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.god.seep.media.audio.format
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs

/**
 * 参考：https://blog.csdn.net/u011418943/article/details/107279236
 */
class Camera2Record(val context: Context) {
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private lateinit var frontCameraInfo: CameraCharacteristics
    private lateinit var backCameraInfo: CameraCharacteristics
    private var frontCameraId: String? = null
    private var backCameraId: String? = null
    private var currentId: String? = null
    private lateinit var imageReader: ImageReader
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private var sensorOrientation: Int = 0
    private val deviceOrientationListener = DeviceOrientationListener(context)
    private var cameraClosed: Boolean = true

    init {
        getCameraInfo()
    }

    fun getCameraInfo() {
        for (cameraId in cameraManager.cameraIdList) {
            Timber.e("camera cameraId: $cameraId")
            //相机所有信息
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            //相机的方向，前置，后置，外置
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing != null) {
                //后置摄像头
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameraId = cameraId
                    backCameraInfo = characteristics
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    //前置摄像头
                    frontCameraId = cameraId
                    frontCameraInfo = characteristics
                }
                currentId = cameraId
            }
            //是否支持Camera2高级特性
            val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                Timber.e("不支持Camera2特性")
                break
            }
        }
    }

    /**
     *摄像头的宽大于高
     */
    private fun initPreviewParams(texture: SurfaceView, shortSize: Int, longSize: Int) {
        val characteristics = if (currentId == backCameraId) backCameraInfo else frontCameraInfo
        //拿到配置的map
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        //获取摄像头传感器方向
        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        //获取预览尺寸
        val previewSizes = map?.getOutputSizes(SurfaceTexture::class.java)
        //获取最佳尺寸
        if (previewSizes != null) {
            val bestSize = getBestSize(shortSize, longSize, previewSizes as Array<Size>)
            /**
             * 配置预览属性
             * 与 Camera1 不同的是，Camera2 是把尺寸信息给到 Surface (SurfaceView 或者 ImageReader)，
             * Camera2 会根据 Surface 配置的大小，输出对应尺寸的画面;
             * 注意摄像头的 width > height ，而我们使用竖屏，所以宽高要变化一下
             */
//            texture.surfaceTexture?.setDefaultBufferSize(bestSize.height, bestSize.width)
            texture.holder.setFixedSize(bestSize.height, bestSize.width)
        }
        //设置图片尺寸，图片选择最大的分辨率即可
        val sizes = map?.getOutputSizes(ImageFormat.JPEG)
        val largest = Collections.max(sizes?.asList()) { o1, o2 -> o1.width * o1.height - o2.width * o2.height }
        //最大Image为 1，因为是 JPEG
        imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader -> //当有图片数据时回调该接口
            //获取捕获的照片数据
            val image = reader.acquireLatestImage()
            //JPEG，只需要获取下标为0的数据即可
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            val storeDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photo = File(storeDir, "${Date().format()}.jpg")
            val fos = FileOutputStream(photo)
            //旋转图片
            val matrix = Matrix()
            bitmap = if (currentId == frontCameraId) {
                //需要旋转+镜像
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                matrix.setRotate(90f)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            image.close()
        }, null)
    }

    /**
     * Camera2 会根据 Surface 配置的大小，输出对应尺寸的画面，所以设置 TextureView 的大小即可，摄像头的 width > height ，而我们使用竖屏，所以宽高要变化一下
     */
    private fun getBestSize(shortSize: Int, longSize: Int, sizes: Array<Size>): Size {
        //默认取一个
        var bestSize = sizes[0]
        val uiRatio = (longSize / shortSize).toFloat()
        var minRatio = uiRatio
        for (previewSize in sizes) {
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

    fun openCamera(texture: SurfaceView) {
        openCamera(currentId, texture)
    }

    //texture: TextureView
    private fun openCamera(cameraId: String?, texture: SurfaceView) {
        currentId = cameraId
        if (cameraId == null) return
        initPreviewParams(texture, texture.width, texture.height)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Timber.e("没有摄像头权限")
            return
        }
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraClosed = false
                cameraDevice = camera
                //预览
                createPreviewPipeline(camera, texture)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                Timber.e("摄像头开启失败，error：$error")
            }
        }, null)
    }

    fun switchCamera(texture: SurfaceView) {
        closeCamera()
        if (currentId == frontCameraId)
            openCamera(backCameraId, texture)
        else
            openCamera(frontCameraId, texture)
    }

    private fun createPreviewPipeline(camera: CameraDevice, texture: SurfaceView) {
        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//        val surface = Surface(texture.surfaceTexture)
        val surface = texture.holder.surface
        //添加surface容器
        captureBuilder.addTarget(surface)
        //创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求,这个必须在创建 Seesion 之前就准备好，传递给底层用于 pipeline
        camera.createCaptureSession(arrayListOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                //设置自动聚焦
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                //设置自动曝光
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                val captureRequest = captureBuilder.build()
                //设置预览时连续捕获图片数据
                session.setRepeatingRequest(captureRequest, null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Timber.e("预览配置失败")
            }
        }, null)
    }

    fun stopPreview() {
        if (cameraClosed) return
        captureSession.stopRepeating()
    }

    fun takePicture(texture: SurfaceView) {
        if (cameraClosed) return
        //拍照的session
        val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        //设置装载图像数据的surface
        captureBuilder.addTarget(imageReader.surface)
        //设置自动聚焦
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        //设置自动曝光
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        //根据设备方向计算照片的方向
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation())
        //先停止预览
        stopPreview()
        captureSession.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                //拍完之后继续预览
                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//                captureRequest.addTarget(Surface(texture.surfaceTexture))
                captureRequest.addTarget(texture.holder.surface)
                captureSession.setRepeatingRequest(captureRequest.build(), null, null)
            }
        }, null)
    }

    private fun adjustCameraOrientation(): Int {
        //判断当前横竖屏
        //获取显示方向
        val degrees = when (context.display?.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        var result = 0
        if (currentId == backCameraId) {
            result = (sensorOrientation - degrees + 360) % 360
        } else if (currentId == frontCameraId) {
            result = (sensorOrientation + degrees) % 360
            result = (360 - result) % 360
        }
        return result
    }

    private fun getJpegOrientation(): Int {
        var myDeviceOrientation = deviceOrientationListener.orientation
        if (myDeviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0
        }
        // Round device orientation to a multiple of 90
        myDeviceOrientation = (myDeviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
//        val facingFront = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (currentId == frontCameraId) {
            myDeviceOrientation = -myDeviceOrientation
        }
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + myDeviceOrientation + 360) % 360
    }

    fun closeCamera() {
        stopPreview()
        cameraDevice.close()
        cameraClosed = true
    }
}

class DeviceOrientationListener(context: Context) : OrientationEventListener(context) {
    var orientation: Int = 0
        private set

    override fun onOrientationChanged(orientation: Int) {
        this.orientation = orientation
    }
}