package com.god.seep.media.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import com.god.seep.media.audio.format
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import java.util.*

class Extractor {
    private var mediaExtractor: MediaExtractor = MediaExtractor()
    var videoFormat: MediaFormat? = null
    var audioFormat: MediaFormat? = null
    private var videoTrackId: Int = -1
    private var audioTrackId: Int = -1
    private var released: Boolean = false

    var curSampleTime: Long = -1
    var curSampleFlags: Int = -1

    val videoDuration: Long
        get() = videoFormat?.getLong(MediaFormat.KEY_DURATION) ?: -1
    val audioDuration: Long
        get() = audioFormat?.getLong(MediaFormat.KEY_DURATION) ?: -1

    val allFormat: String
        get() = "$videoFormat --- $audioFormat"

    fun reset(path: String) {
        videoTrackId = -1
        audioTrackId = -1
        videoFormat = null
        audioFormat = null
        mediaExtractor.setDataSource(path)
        //轨道数目
        for (index in 0 until mediaExtractor.trackCount) {
            val format = mediaExtractor.getTrackFormat(index)
            Timber.e("媒体格式信息：$format")
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null)
                if (mime.startsWith("video")) {
                    videoTrackId = index
                    videoFormat = format
                } else if (mime.startsWith("audio")) {
                    audioTrackId = index
                    audioFormat = format
                }
        }
    }

    //提取
    fun extract() {
        Timber.e("sampleTime（当前时间戳）：${mediaExtractor.sampleTime}, sampleSize: mediaExtractor.sampleSize, sampleFlag: ${mediaExtractor.sampleFlags}, sampleTrack: ${mediaExtractor.sampleTrackIndex}")

        val byteBuffer = ByteBuffer.allocate(1024 * 4)
        mediaExtractor.readSampleData(byteBuffer, 0)
        //读取下一帧
        mediaExtractor.advance()
        mediaExtractor.release()
    }

    /**
     * 轨道必须提前选择，否则会在播放时出现未知错误（音频播放时的解码错误）
     */
    fun selectTrack(isVideo: Boolean) {
        //选择要解析的轨道
        mediaExtractor.selectTrack(if (isVideo) videoTrackId else audioTrackId)
    }

    fun readBuffer(buffer: ByteBuffer, isVideo: Boolean): Int {
        buffer.clear()
        if (released)
            return -1
        //选择要解析的轨道
//        mediaExtractor.selectTrack(if (isVideo) videoTrackId else audioTrackId)
        //读取当前帧的数据
        val bufferCount = mediaExtractor.readSampleData(buffer, 0)
        if (bufferCount < 0) return -1
        //记录当前时间戳
        curSampleTime = mediaExtractor.sampleTime
        //记录当前帧的标志位
        curSampleFlags = mediaExtractor.sampleFlags
        //进入下一帧
        Timber.e("当前帧：sampleTime（当前时间戳）：${mediaExtractor.sampleTime}, sampleSize: mediaExtractor.sampleSize, sampleFlag: ${mediaExtractor.sampleFlags}, sampleTrack: ${mediaExtractor.sampleTrackIndex}")
        mediaExtractor.advance()
        return bufferCount
    }

    fun release() {
        released = true
        mediaExtractor.release()
    }

}

class Muxer(val context: Context, val listener: MuxerListener) : Thread() {
    private lateinit var mediaMuxer: MediaMuxer
    private val videoExtractor = Extractor()
    private val audioExtractor = Extractor()

    fun setDataSource(path: String) {
        videoExtractor.reset(path)
        audioExtractor.reset(path)
        val storeDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val file = File(storeDir, "${Date().format()}.mp4")
        mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        videoExtractor.selectTrack(true)
        audioExtractor.selectTrack(false)
    }

    fun init(outPath: String, format: Int) {
        val mediaMuxer = MediaMuxer(outPath, format)
        val videoFormat = MediaFormat.createVideoFormat("video/avc", 320, 240)
        val csd = ByteArray(8)
        videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd))
        videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd))
        mediaMuxer.addTrack(videoFormat)
        mediaMuxer.start()
//        mediaMuxer.writeSampleData()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

    override fun run() {
        super.run()
        listener.onStart()
        val videoFormat = MediaFormat.createVideoFormat("video/avc", 320, 240)
        videoFormat.setByteBuffer("csd-0", videoExtractor.videoFormat!!.getByteBuffer("csd-0"))
        videoFormat.setByteBuffer("csd-1", videoExtractor.videoFormat!!.getByteBuffer("csd-1"))
        val videoId = mediaMuxer.addTrack(videoFormat)
        val audioId = mediaMuxer.addTrack(audioExtractor.audioFormat!!)
        mediaMuxer.start()
        val buffer = ByteBuffer.allocate(1024 * 500)
        val info = MediaCodec.BufferInfo()

        var videoSize = videoExtractor.readBuffer(buffer, true)
        var preSampleTime = -1L
        while (videoSize > 0) {
            info.offset = 0
            info.size = videoSize
            info.presentationTimeUs = videoExtractor.curSampleTime
            info.flags = videoExtractor.curSampleFlags
            mediaMuxer.writeSampleData(videoId, buffer, info)
            if (preSampleTime == -1L)
                preSampleTime = info.presentationTimeUs
            //时间戳单位是微妙
            if (info.presentationTimeUs - preSampleTime > 10000000)
                break
            videoSize = videoExtractor.readBuffer(buffer, true)
        }

        var audioSize = audioExtractor.readBuffer(buffer, false)
        preSampleTime = -1
        while (audioSize > 0) {
            info.size = audioSize
            info.offset = 0
            info.presentationTimeUs = audioExtractor.curSampleTime
            info.flags = audioExtractor.curSampleFlags
            mediaMuxer.writeSampleData(audioId, buffer, info)
            if (preSampleTime == -1L)
                preSampleTime = info.presentationTimeUs
            if (info.presentationTimeUs - preSampleTime > 10000000)
                break
            audioSize = audioExtractor.readBuffer(buffer, false)

        }

        audioExtractor.release()
        videoExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        listener.onSuccess()
    }
}

interface MuxerListener {
    fun onStart()

    fun onSuccess()

    fun onFailed()
}