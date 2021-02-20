package com.god.seep.media.codec

import android.media.MediaCodec
import android.media.MediaFormat
import com.god.seep.media.video.Extractor

const val VIDEO = 1
const val AUDIO = 2
const val TIME_US = 1000L

abstract class BaseDecode : Runnable {

    lateinit var extractor: Extractor
    lateinit var mediaCodec: MediaCodec
    var mediaFormat: MediaFormat? = null

    @Volatile
    var isDone = false  //有多线程问题

    open fun reset(path: String) {
        extractor = Extractor()
        extractor.reset(path)
        mediaFormat = if (decodeType() == VIDEO) extractor.videoFormat else extractor.audioFormat
        mediaCodec = MediaCodec.createDecoderByType(mediaFormat?.getString(MediaFormat.KEY_MIME)!!)
        extractor.selectTrack(decodeType() == VIDEO)
        configure()
        mediaCodec.start()
    }

    override fun run() {
        val info = MediaCodec.BufferInfo()
        while (!isDone) {
            //等待 TIME_US 直到拿到空的 input buffer下标，单位为 us， -1 表示一直等待，知道拿到数据，0 表示立即返回
            val inputBufferId = mediaCodec.dequeueInputBuffer(TIME_US)
            //-1表示当前没有可用的缓冲区
            if (inputBufferId > 0) {
                //可用的、空的 ByteBuffer
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferId)
                if (inputBuffer != null) {
                    //拿到帧数据
                    val size = extractor.readBuffer(inputBuffer, true)
                    if (size >= 0) {
                        mediaCodec.queueInputBuffer(inputBufferId, 0, size, extractor.curSampleTime, extractor.curSampleFlags)
                    } else {
                        //结束，传递结束标志
                        mediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isDone = true
                    }
                }
            }
            val isFinish = handleOutputData(info)
            if (isFinish)
                break
        }
        done()
    }

    abstract fun decodeType(): Int

    abstract fun configure()

    abstract fun handleOutputData(info: MediaCodec.BufferInfo): Boolean

    open fun done() {
        isDone = true
        mediaCodec.stop()
        mediaCodec.release()
        extractor.release()
    }
}

