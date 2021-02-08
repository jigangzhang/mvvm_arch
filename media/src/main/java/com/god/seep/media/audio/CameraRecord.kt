package com.god.seep.media.audio

import android.content.Context
import android.media.*
import android.media.AudioRecord
import android.os.Environment
import okhttp3.internal.notify
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

//采样率：44.1kHz
const val SAMPLE_RATE = 44100

class CameraRecord(val context: Context) : Thread() {
    private val minBufferSize: Int = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
    private val audioRecord: AudioRecord
    private var started = false
    private var done: Boolean = false
    private var released = false
    private val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.filesDir

    init {
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)
    }

    fun fetchFiles(): List<File> {
        return storageDir.listFiles()?.asList() ?: emptyList()
    }

    override fun run() {
        super.run()
        while (!released && !done) {
            Timber.e("storageDir: $storageDir")
            val pcmFile = File(storageDir, "${Date().format()}.pcm")
            if (!pcmFile.exists())
                pcmFile.createNewFile()
            val fos = FileOutputStream(pcmFile)
            val byteArray = ByteArray(minBufferSize)
            audioRecord.startRecording()
            while (!done) {
                val read = audioRecord.read(byteArray, 0, byteArray.size)
                if (read < 0)
                    Timber.e("audio record err: $read")
                if (AudioRecord.ERROR_INVALID_OPERATION != read)
                    fos.write(byteArray)
            }
            audioRecord.stop()
            fos.flush()
            fos.close()

            pcm2Wav(pcmFile, pcmFile.length(), SAMPLE_RATE, audioRecord.channelCount)
            synchronized(this) {
                this.wait()
            }
        }
    }

    fun startRecord() {
        done = false
        if (!started) {
            started = true
            start()
        }
        synchronized(this) {
            this.notify()
        }
    }

    fun stopRecord() {
        done = true
    }

    fun release() {
        released = true
        synchronized(this) {
            this.notifyAll()
        }
        audioRecord.release()
    }
}

fun Date.format(format: String = "MMddHHmmss"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(this)
}

fun Long.formatFileSize(): String {
    var num: Double = this.toDouble()
    return if (num < 1024) String.format(Locale.getDefault(), "%.2fB", num) else {
        num /= 1024
        if (num < 1024) String.format(Locale.getDefault(), "%.2fKB", num) else {
            num /= 1024
            String.format(Locale.getDefault(), "%.2fMB", num)
        }
    }
}

/**
 * 任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，
 * wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
 * FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的
 *
 * @param pcmByteCount 不包括header的音频数据总长度
 * @param sampleRate    采样率,也就是录制时使用的频率
 * @param channels          audioRecord的频道数量
 */
fun pcm2Wav(pcm: File, pcmByteCount: Long, sampleRate: Int, channels: Int): File {
    val totalSize = pcmByteCount + 36 //不包含前8个字节的WAV文件总长度
    val byteRate = sampleRate * 2 * channels
    val header = ByteArray(44)
    //RIFF
    header[0] = 'R'.toByte()
    header[1] = 'I'.toByte()
    header[2] = 'F'.toByte()
    header[3] = 'F'.toByte()
    //数据大小
    header[4] = (totalSize and 0xff).toByte()
    header[5] = (totalSize shr 8 and 0xff).toByte()
    header[6] = (totalSize shr 16 and 0xff).toByte()
    header[7] = (totalSize shr 24 and 0xff).toByte()
    //WAVE
    header[8] = 'W'.toByte()
    header[9] = 'A'.toByte()
    header[10] = 'V'.toByte()
    header[11] = 'E'.toByte()
    //FMT Chunk
    header[12] = 'f'.toByte()
    header[13] = 'm'.toByte()
    header[14] = 't'.toByte()
    header[15] = ' '.toByte()
    //数据大小
    header[16] = 16 //4 bytes: size of 'fmt ' chunk
    header[17] = 0
    header[18] = 0
    header[19] = 0
    //编码方式 10H为PCM编码格式
    header[20] = 1  //format = 1
    header[21] = 0
    //通道数
    header[22] = channels.toByte()
    header[23] = 0
    //采样率，每个通道的播放速度
    header[24] = (sampleRate and 0xff).toByte()
    header[25] = (sampleRate shr 8 and 0xff).toByte()
    header[26] = (sampleRate shr 16 and 0xff).toByte()
    header[27] = (sampleRate shr 24 and 0xff).toByte()
    //音频数据传送速率，采样率*通道数*采样深度/8
    header[28] = (byteRate and 0xff).toByte()
    header[29] = (byteRate shr 8 and 0xff).toByte()
    header[30] = (byteRate shr 16 and 0xff).toByte()
    header[31] = (byteRate shr 24 and 0xff).toByte()
    //确定系统一次要处理多少这样字节的数据，确定缓冲区，通道数*采样位数
    header[32] = (2 * channels).toByte()
    header[33] = 0
    //每个样本的数据位数
    header[34] = 16
    header[35] = 0
    //Data chunk
    header[36] = 'd'.toByte()
    header[37] = 'a'.toByte()
    header[38] = 't'.toByte()
    header[39] = 'a'.toByte()
    header[40] = (pcmByteCount and 0xff).toByte()
    header[41] = (pcmByteCount shr 8 and 0xff).toByte()
    header[42] = (pcmByteCount shr 16 and 0xff).toByte()
    header[43] = (pcmByteCount shr 24 and 0xff).toByte()
    //Header完成--结尾

    //追加Header
    val target = File(pcm.parent, "${pcm.nameWithoutExtension}.wav")
    val fos = FileOutputStream(target, true)
    fos.write(header)
    pcm.inputStream().use { input ->
        fos.write(input.readBytes())
    }
    fos.close()
//    val raf = RandomAccessFile(target, "rw")
//    raf.seek(0)
//    raf.write(header)
//    raf.close()
    return target
}

fun playPcmStatic(pcm: File) {
    val fis = FileInputStream(pcm)
    val bos = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length: Int
    fis.copyTo(bos, buffer.size)
    //音频数据
    val bytes = bos.toByteArray()
    val channel = AudioFormat.CHANNEL_IN_STEREO

    /**
     * 设置音频信息属性
     * 1.设置支持多媒体属性，比如audio，video
     * 2.设置音频格式，比如 music
     */
    val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    /**
     * 设置音频格式
     * 1. 设置采样率
     * 2. 设置采样位数
     * 3. 设置声道
     */
    val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(channel)
            .build()
    val audioTrack = AudioTrack(attrs, format, bytes.size, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE)//音频识别id
    //一次性写入
    audioTrack.write(bytes, 0, bytes.size)
    audioTrack.play()
    Timber.e("AudioTrack state: ${audioTrack.state}, play state: ${audioTrack.playState}")
//    audioTrack.setPlaybackPositionUpdateListener{}
//    audioTrack.registerStreamEventCallback()
    audioTrack.release()
}

fun playPcmStream(pcm: File) {
    val channel = AudioFormat.CHANNEL_IN_STEREO
    // 设置音频信息属性, 1.设置支持多媒体属性，比如audio，video;  2.设置音频格式，比如 music
    val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    //设置音频格式; 1. 设置采样率; 2. 设置采样位数;  3. 设置声道
    val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(channel)
            .build()
    val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, channel, AudioFormat.ENCODING_PCM_16BIT)
    val audioTrack = AudioTrack(attrs, format, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    audioTrack.play() //播放，等待数据
    Thread {
        val fis = FileInputStream(pcm)
        val buffer = ByteArray(bufferSize)
        var read = fis.read(buffer)
        while (read > 0) {
            Timber.e("AudioTrack state: ${audioTrack.state}, play state: ${audioTrack.playState}")
            audioTrack.write(buffer, 0, bufferSize)
            read = fis.read(buffer)
        }
        Timber.e("AudioTrack state: ${audioTrack.state}, play state: ${audioTrack.playState}")
        audioTrack.stop()
        Timber.e("AudioTrack state: ${audioTrack.state}, play state: ${audioTrack.playState}")
        audioTrack.release()
        fis.close()
    }.start()
}
