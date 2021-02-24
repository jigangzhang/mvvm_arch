package com.god.seep.media.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import android.view.TextureView
import com.god.seep.media.video.Extractor
import timber.log.Timber


class VideoDecode(private val textureView: TextureView) : BaseDecode() {
    override fun decodeType(): Int {
        return VIDEO
    }

    override fun configure() {
        mediaCodec.configure(mediaFormat, Surface(textureView.surfaceTexture), null, 0)
    }

    /**
     * DTS (Decoding Time Stamp)：即解码时间戳，这个时间戳的意义在于告诉播放器该在什么时候解码这一帧的数据
     * PTS (Presentation Time Stamp)：显示时间戳，这个时间戳告诉播放器，什么时候播放这一帧
     *  需要注意的是，虽然 DTS 、PTS 是用于指导播放端的行为，但他们是在编码的时候，由编码器生成的。
     *  在没有B帧的情况下，DTS和 PTS 的输出顺序是一样的，一旦存在 B 帧，则顺序不一样。
     */
    var startMs = -1L
    override fun handleOutputData(info: MediaCodec.BufferInfo): Boolean {
        val outputBufferId = mediaCodec.dequeueOutputBuffer(info, TIME_US)
        if (outputBufferId >= 0) {
            if (startMs == -1L) {
                startMs = System.currentTimeMillis()
            }
            //矫正pts
            sleepRender(info, startMs)
            //释放buffer，并渲染到Surface中
            mediaCodec.releaseOutputBuffer(outputBufferId, true)
        }
        //在所有解码后的帧都被渲染后，就可以停止播放了
        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Timber.e("所有渲染结束：zsr OutputBuffer BUFFER_FLAG_END_OF_STREAM")
            return true
        }
        return false
    }

    /**
     * 数据的时间戳对齐
     */
    private fun sleepRender(info: MediaCodec.BufferInfo, startMs: Long) {
        val ptsTimes = info.presentationTimeUs / 1000
        val systemTimes = System.currentTimeMillis() - startMs
        val diff = ptsTimes - systemTimes
        if (diff > 0) {
            Thread.sleep(diff)
        }
    }

    fun stop() {
        isDone = true
    }
}

/**
 *音频的异步解码也一样
 */
class AsyncVideoDecode {
    private lateinit var mediaCodec: MediaCodec
    private lateinit var extractor: Extractor
    private var mediaFormat: MediaFormat? = null
    private var stopped: Boolean = false

    fun reset(path: String) {
        extractor = Extractor()
        extractor.reset(path)
        mediaFormat = extractor.videoFormat
        mediaCodec = MediaCodec.createDecoderByType(mediaFormat?.getString(MediaFormat.KEY_MIME)!!)
        extractor.selectTrack(true)
    }

    fun start(textureView: TextureView) {
        mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                if (stopped) return
                val inputBuffer = codec.getInputBuffer(index)
                val size = extractor.readBuffer(inputBuffer!!, true)
                if (size >= 0) {
                    codec.queueInputBuffer(index, 0, size, extractor.curSampleTime, extractor.curSampleFlags)
                } else {
                    //结束
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                codec.releaseOutputBuffer(index, true)
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Timber.e("视频解码出错：${e.message}")
                Timber.e(e)
                codec.reset()
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Timber.e("视频格式变更：$format")
            }
        })
        //配置configure
        mediaCodec.configure(mediaFormat, Surface(textureView.surfaceTexture), null, 0)
        //开始解码
        mediaCodec.start()
    }

    fun stop() {
        mediaCodec.stop()
    }

    fun destroy() {
        stopped = true
        mediaCodec.stop()
        mediaCodec.release()
        extractor.release()
    }
}