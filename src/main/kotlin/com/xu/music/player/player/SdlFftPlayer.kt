package com.xu.music.player.player

import cn.hutool.core.io.IoUtil
import cn.hutool.core.text.CharSequenceUtil
import com.xu.music.player.constant.Constant
import com.xu.music.player.hander.DataBaseError
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.Volatile

/**
 * SourceDataLine 音频播放
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class SdlFftPlayer private constructor() : Player {

    val log = LoggerFactory.getLogger("SdlFftPlayer")

    /**
     * FFT
     */
    private val fft = FastFourierTransformer(DftNormalization.STANDARD)

    /**
     * SourceDataLine
     */
    private var data: SourceDataLine? = null

    /**
     * AudioInputStream
     */
    private var audio: AudioInputStream? = null

    /**
     * 音频时长
     */
    private var duration = 0.0

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

    private object SingletonHolder {
        val PLAYER: SdlFftPlayer = SdlFftPlayer()
    }

    @Throws(Exception::class)
    override fun load(url: URL?) {
        if (this.playing) {
            stop()
        }

        load(AudioSystem.getAudioInputStream(url))
    }

    @Throws(Exception::class)
    override fun load(file: File) {
        if (this.playing) {
            stop()
        }

        if (!file.exists()) {
            throw DataBaseError("File does not exist")
        }

        val name = file.name
        if (CharSequenceUtil.endWithIgnoreCase(name, ".mp3")) {
            val stream = MpegAudioFileReader().getAudioInputStream(file)
            load(stream)
            return
        }

        if (CharSequenceUtil.endWithIgnoreCase(name, ".flac")) {
            val stream = AudioSystem.getAudioInputStream(file)
            load(stream)
            return
        }

        load(AudioSystem.getAudioInputStream(file))
    }

    @Throws(Exception::class)
    override fun load(path: String?) {
        if (this.playing) {
            stop()
        }

        load(File(path))
    }

    @Throws(Exception::class)
    override fun load(stream: AudioInputStream) {
        var stream = stream
        if (this.playing) {
            stop()
        }

        var format = stream.format
        format = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, 16, format.channels,
            format.channels * 2, format.sampleRate, false
        )
        stream = AudioSystem.getAudioInputStream(format, stream)
        val info = DataLine.Info(SourceDataLine::class.java, stream.format, AudioSystem.NOT_SPECIFIED)
        data = AudioSystem.getLine(info) as SourceDataLine
        data!!.open(stream.format)
        this.audio = stream
    }

    @Throws(Exception::class)
    override fun load(encoding: AudioFormat.Encoding?, stream: AudioInputStream?) {
        if (this.playing) {
            stop()
        }

        load(AudioSystem.getAudioInputStream(encoding, stream))
    }

    @Throws(Exception::class)
    override fun load(format: AudioFormat?, stream: AudioInputStream?) {
        if (this.playing) {
            stop()
        }

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

    private fun start() {
        try {
            this.playing = true
            data!!.start()
            if (data!!.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                control = data!!.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            }

            this.duration = getAudioDuration(
                this.audio,
                audio!!.format
            )
            val buff = ByteArray(4)
            val channels = audio!!.format.channels
            val rate = audio!!.format.sampleRate
            while (audio!!.read(buff) != -1 && playing) {
                synchronized(this) {
                    while (this.paused) {
                        (this as Object).wait()
                    }
                }
                setSpectrum(buff, channels, rate.toInt())
                data!!.write(buff, 0, 4)
            }
            data!!.drain()
            data!!.stop()
        } catch (e: Exception) {
            log.error("SdlFftPlayer 播放异常！", e)
        }
    }

    override fun play() {
        if (this.playing) {
            return
        }

        if (null == this.audio || null == this.data) {
            return
        }

        EXECUTOR.submit { this.start() }
    }

    override fun stop() {
        this.playing = false
        if (null == this.audio || null == this.data) {
            return
        }

        data!!.stop()
        IoUtil.close(this.audio)
        IoUtil.close(this.data)
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

    override fun position(): Double {
        return data!!.framePosition.toDouble()
    }

    override fun duration(): Double {
        return this.duration
    }

    override fun playing(): Boolean {
        return this.playing
    }

    override fun pausing(): Boolean {
        return this.paused
    }

    private fun setSpectrum(buff: ByteArray, channels: Int, sample: Int) {
        if (buff.size != 4) {
            return
        }

        // Stereo
        if (channels == 2) {
            if (sample == 16) {
                val left = ((buff[1].toInt() shl 8) or (buff[0].toInt() and 0xFF)).toShort()
                val right = ((buff[3].toInt() shl 8) or (buff[2].toInt() and 0xFF)).toShort()
                putSrc((left + right) / 2.0 / 32768.0)
                return
            }

            // Assuming 8-bit samples
            val left = (buff[0].toInt() and 0xFF) / 128.0 - 1.0
            val right = (buff[1].toInt() and 0xFF) / 128.0 - 1.0
            putSrc((left + right) / 2.0)
            return
        }

        // Mono
        if (sample == 16) {
            putSrc(((buff[1].toInt() shl 8) or (buff[0].toInt() and 0xFF)).toShort() / 32768.0)
            return
        }

        // Assuming 8-bit samples
        putSrc((buff[0].toInt() and 0xFF) / 128.0 - 1.0)
    }

    private fun putDst(value: Double) {
        TRANS.add(value)
        if (TRANS.size > Constant.SPECTRUM_TOTAL_NUMBER) {
            TRANS.removeFirst()
        }
    }

    private fun putSrc(value: Double) {
        synchronized(SRC) {
            SRC.add(value)
            if (SRC.size > Constant.SPECTRUM_TOTAL_NUMBER) {
                SRC.removeFirst()

                val complex = fft.transform(SRC.stream().mapToDouble { obj: Double -> obj.toDouble() }
                    .toArray(), TransformType.FORWARD)

                for (i in 0 until Constant.SPECTRUM_TOTAL_NUMBER) {
                    putDst(complex[i].abs())
                }
            }
        }
    }

    companion object {
        /**
         * 原始数据
         */
        protected val SRC: Deque<Double> = LinkedList()

        /**
         * 频谱
         */
        @JvmField
        val TRANS: Deque<Double> = LinkedList()

        /**
         * 线程池
         */
        private val EXECUTOR: ExecutorService = Executors.newFixedThreadPool(1)

        @JvmStatic
        fun create(): SdlFftPlayer {
            return SingletonHolder.PLAYER
        }

        /**
         * 计算音频时长
         *
         * @param audio  音频流
         * @param format 音频格式
         * @return 音频时长
         * @date 2019年10月31日19:06:39
         */
        fun getAudioDuration(audio: AudioInputStream?, format: AudioFormat): Double {
            return audio!!.frameLength * format.frameSize / (format.sampleRate * format.channels * (format.sampleSizeInBits / 8.0))
        }
    }
}