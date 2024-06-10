package com.xu.music.player.player

import cn.hutool.core.io.NioUtil
import cn.hutool.core.text.CharSequenceUtil
import com.xu.music.player.constant.Constant
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.File
import java.net.URL
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine.Info
import javax.sound.sampled.FloatControl
import javax.sound.sampled.FloatControl.Type
import javax.sound.sampled.SourceDataLine

/**
 * SourceDataLine 音频播放
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
class SourceDataLinePlayer private constructor() : Player {

    /**
     * SourceDataLine
     */
    private var data: SourceDataLine? = null

    /**
     * AudioInputStream
     */
    private var audio: AudioInputStream? = null

    /**
     * FloatControl
     */
    private var control: FloatControl? = null

    /**
     * 暂停
     */
    @Volatile
    private var paused = false

    /**
     * 播放
     */
    @Volatile
    private var playing = false

    @Throws(Exception::class)
    override fun load(url: URL?) {
        load(AudioSystem.getAudioInputStream(url))
    }

    @Throws(Exception::class)
    override fun load(file: File?) {
        val name = file!!.name
        if (CharSequenceUtil.endWithIgnoreCase(name, ".mp3")) {
            var stream = MpegAudioFileReader().getAudioInputStream(file)
            var format = stream.format
            format = AudioFormat(
                Encoding.PCM_SIGNED, format.sampleRate, 16, format.channels,
                format.channels * 2, format.sampleRate, false
            )
            stream = AudioSystem.getAudioInputStream(format, stream)
            load(stream)
            return
        }
        if (CharSequenceUtil.endWithIgnoreCase(name, ".flac")) {
            var stream = AudioSystem.getAudioInputStream(file)

            var format = stream.format
            format = AudioFormat(
                Encoding.PCM_SIGNED, format.sampleRate, 16, format.channels,
                format.channels * 2, format.sampleRate, false
            )
            stream = AudioSystem.getAudioInputStream(format, stream)
            load(stream)
            return
        }
        load(AudioSystem.getAudioInputStream(file))
    }

    @Throws(Exception::class)
    override fun load(path: String?) {
        load(path?.let { File(it) })
    }

    @Throws(Exception::class)
    override fun load(stream: AudioInputStream?) {
        val info = Info(SourceDataLine::class.java, stream!!.format, AudioSystem.NOT_SPECIFIED)
        data = AudioSystem.getLine(info) as SourceDataLine
        data!!.open(stream.format)
        this.audio = stream
    }

    @Throws(Exception::class)
    override fun load(encoding: Encoding?, stream: AudioInputStream?) {
        load(AudioSystem.getAudioInputStream(encoding, stream))
    }

    @Throws(Exception::class)
    override fun load(format: AudioFormat?, stream: AudioInputStream?) {
        load(AudioSystem.getAudioInputStream(format, stream))
    }

    override fun pause() {
        this.paused = true
    }

    override fun resume(duration: Long) {
        this.paused = false
        synchronized(this) {
            (this as Object).notifyAll()
        }
    }

    @Throws(Exception::class)
    override fun play() {
        if (null == this.audio || null == this.data) {
            return
        }
        this.playing = true
        data!!.start()
        if (data!!.isControlSupported(Type.MASTER_GAIN)) {
            control = data!!.getControl(Type.MASTER_GAIN) as FloatControl
        }
        val buff = ByteArray(4)
        val channels = audio!!.format.channels
        val rate = audio!!.format.sampleRate
        while (audio!!.read(buff) != -1 && playing) {
            synchronized(this) {
                while (this.paused) {
                    data!!.flush()
                    (this as Object).wait()
                }
            }
            setSpectrum(rate, channels, buff)
            data!!.write(buff, 0, 4)
        }
        data!!.drain()
        data!!.stop()
    }

    override fun stop() {
        if (null == this.audio || null == this.data) {
            return
        }
        this.playing = false
        data!!.stop()
        NioUtil.close(this.data)
        NioUtil.close(this.audio)
    }

    override fun volume(volume: Float) {
        if (null == control) {
            return
        }
        if (volume < control!!.minimum || volume > control!!.maximum) {
            return
        }
        control!!.value = volume
    }

    private object SingletonHolder {
        val player: SourceDataLinePlayer = SourceDataLinePlayer()
    }

    /**
     * 设置频谱
     *
     * @param rate     比特率
     * @param channels 声道
     * @param buff     缓冲
     * @date 2024年6月9日11点27分
     * @since V1.0.0.0
     */
    private fun setSpectrum(rate: Float, channels: Int, buff: ByteArray) {
        // 立体声
        if (channels == 2) {
            if (rate == 16f) {
                put(((buff[1].toInt() shl 8) or (buff[0].toInt() and 0xFF)).toDouble()) // 左声道
                put(((buff[3].toInt() shl 8) or (buff[2].toInt() and 0xFF)).toDouble()) // 右声道
                return
            }
            put(buff[0].toDouble()) // 左声道
            put(buff[2].toDouble()) // 左声道
            put(buff[1].toDouble()) // 右声道
            put(buff[3].toDouble()) // 右声道
            return
        }
        // 单声道
        if (rate == 16f) {
            put(((buff[1].toInt() shl 8) or (buff[0].toInt() and 0xFF)).toDouble())
            put(((buff[3].toInt() shl 8) or (buff[2].toInt() and 0xFF)).toDouble())
            return
        }
        put(buff[0].toDouble())
        put(buff[1].toDouble())
        put(buff[2].toDouble())
        put(buff[3].toDouble())
    }

    private fun put(v: Double) {
        synchronized(deque) {
            deque.add(v)
            if (deque.size > Constant.SPECTRUM_TOTAL_NUMBER) {
                deque.removeFirst()
            }
        }
    }

    companion object {
        /**
         * 频谱
         */
        val deque: Deque<Double> = LinkedList()

        fun createPlayer(): SourceDataLinePlayer {
            return SingletonHolder.player
        }
    }
}
