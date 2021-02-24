package com.god.seep.media.codec

import android.media.*
import com.god.seep.media.video.Extractor
import timber.log.Timber

class AudioDecodeSync : BaseDecode() {

    private lateinit var audioTrack: AudioTrack

    override fun decodeType(): Int {
        return AUDIO
    }

    override fun reset(path: String) {
        super.reset(path)
        val format = if (mediaFormat!!.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mediaFormat?.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
        val sampleRate = mediaFormat?.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = mediaFormat?.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val channelConfig = if (channelCount == 1)
            AudioFormat.CHANNEL_IN_MONO
        else
            AudioFormat.CHANNEL_IN_STEREO
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate!!, channelConfig, format!!)
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelConfig)
                .build()
        audioTrack = AudioTrack(attributes, audioFormat, minBufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        audioTrack.play()
    }

    override fun configure() {
        mediaCodec.configure(mediaFormat, null, null, 0)
    }

    override fun handleOutputData(info: MediaCodec.BufferInfo): Boolean {
        Timber.e("音频 BufferInfo: flags=${info.flags}, offset=${info.offset}, size=${info.size}")
        val outputBufferId = mediaCodec.dequeueOutputBuffer(info, TIME_US)
        if (outputBufferId >= 0) {
            val outputBuffer = mediaCodec.getOutputBuffer(outputBufferId)
            audioTrack.write(outputBuffer!!, info.size, AudioTrack.WRITE_BLOCKING)
            mediaCodec.releaseOutputBuffer(outputBufferId, false)
        }
        //在所有解码后的帧都被渲染后，就可以停止播放了
        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Timber.e("所有渲染结束：zsr OutputBuffer BUFFER_FLAG_END_OF_STREAM")
            return true
        }
        return false
    }

    override fun done() {
        super.done()
        destroy()
    }

    fun stop() {
        isDone = true
    }

    fun destroy() {
        audioTrack.stop()
        audioTrack.release()
    }
}

class AsyncAudioDecode {
    private lateinit var audioTrack: AudioTrack
    private lateinit var mediaCodec: MediaCodec
    private lateinit var extractor: Extractor
    private var mediaFormat: MediaFormat? = null
    private var stopped: Boolean = false

    fun reset(path: String) {
        extractor = Extractor()
        extractor.reset(path)
        mediaFormat = extractor.audioFormat
        mediaCodec = MediaCodec.createDecoderByType(mediaFormat?.getString(MediaFormat.KEY_MIME)!!)
        extractor.selectTrack(false)
        configurePlayer()
    }

    fun configurePlayer() {
        val format = if (mediaFormat!!.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mediaFormat?.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
        val sampleRate = mediaFormat?.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = mediaFormat?.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val channelConfig = if (channelCount == 1)
            AudioFormat.CHANNEL_IN_MONO
        else
            AudioFormat.CHANNEL_IN_STEREO
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate!!, channelConfig, format!!)
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelConfig)
                .build()
        audioTrack = AudioTrack(attributes, audioFormat, minBufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        audioTrack.play()
    }

    fun start() {
        mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                if (stopped || index <= 0) return
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
                Timber.e("音频 BufferInfo: flags=${info.flags}, offset=${info.offset}, size=${info.size}")
                if (!stopped && index >= 0) {
                    val outputBuffer = codec.getOutputBuffer(index)
                    audioTrack.write(outputBuffer!!, info.size, AudioTrack.WRITE_BLOCKING)
                    codec.releaseOutputBuffer(index, false)
                }
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Timber.e("所有渲染结束：zsr OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                }
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Timber.e("音频解码出错：${e.message}")
                Timber.e(e)
                codec.reset()
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Timber.e("音频格式变更：$format")
            }
        })
        //配置configure
        mediaCodec.configure(mediaFormat, null, null, 0)
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