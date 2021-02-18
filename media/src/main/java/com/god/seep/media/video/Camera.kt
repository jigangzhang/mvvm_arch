package com.god.seep.media.video

interface Camera {

    fun openCamera()

    fun switchCamera()

    fun startPreview()

    fun takePicture()

    fun closeCamera()

}