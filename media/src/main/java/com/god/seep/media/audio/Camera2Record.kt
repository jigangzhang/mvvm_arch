package com.god.seep.media.audio

import android.content.Context
import android.hardware.camera2.CameraManager


class Camera2Record(context: Context) {
    init {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
}