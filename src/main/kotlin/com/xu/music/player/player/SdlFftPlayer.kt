package com.xu.music.player.player

import cn.hutool.core.io.IoUtil
import cn.hutool.core.text.CharSequenceUtil
import com.xu.music.player.constant.Constant
import com.xu.music.player.hander.DataBaseError
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.io.File
import java.net.URL
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import org.slf4j.LoggerFactory

/**
 * SourceDataLine FFT 频谱播放
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class SdlFftPlayer private constructor() : Player {

    /** SourceDataLine */
    private var data: SourceDataLine? = null

    /** AudioInputStream */
    private var audio: AudioInputStream? = null

    /** 音频时长 */
    private var duration = 0.0

    /** FloatControl */
    private var control: FloatControl? = null

    /** 暂停 */
    @Volatile
    private var paused = false

    /** 播放 */
    @Volatile
    private var playing = false

    @Throws(Exception::class)
    override fun load(url: URL?) {
        stop()
        load(AudioSystem.getAudioInputStream(url))
    }

    @Throws(Exception::class)
    override fun load(file: File?) {
        stop()
        if (!file!!.exists()) {
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
        stop()
        load(File(path))
    }

    /**
     * 加载 AudioInputStream 构建 SourceDataLine
     *
     * @param stream AudioInputStream 音频流
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @Throws(Exception::class)
    override fun load(stream: AudioInputStream?) {
        stop()

        var audioStream = stream
        var format = audioStream!!.format
        format = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, 16, format.channels,
            format.channels * 2, format.sampleRate, false
        )
        audioStream = AudioSystem.getAudioInputStream(format, audioStream)
        val info = DataLine.Info(SourceDataLine::class.java, audioStream.format, AudioSystem.NOT_SPECIFIED)
        data = AudioSystem.getLine(info) as SourceDataLine
        data!!.open(audioStream.format)
        this.audio = audioStream
    }

    @Throws(Exception::class)
    override fun load(encoding: AudioFormat.Encoding?, stream: AudioInputStream?) {
        stop()
        load(AudioSystem.getAudioInputStream(encoding, stream))
    }

    @Throws(Exception::class)
    override fun load(format: AudioFormat?, stream: AudioInputStream?) {
        stop()
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
            this.data!!.start()
            if (this.data!!.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                control = this.data!!.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            }

            this.duration = getAudioDuration(this.audio!!, this.audio!!.format)
            val buff = ByteArray(4)
            val channels = this.audio!!.format.channels
            val rate = this.audio!!.format.sampleRate
            while (audio!!.read(buff) != -1 && playing) {
                synchronized(this) {
                    while (this.paused) {
                        (this as Object).wait()
                    }
                }
                setSpectrum(buff, channels, rate.toInt())
                this.data!!.write(buff, 0, 4)
            }
            data!!.drain()
            data!!.stop()
            this.playing = false
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

        synchronized(SRC_ARRAY) {
            srcWriteIndex = 0
            srcCount = 0
            SRC_ARRAY.fill(0.0)
        }

        this.playing = true
        EXECUTOR.submit(Runnable { start() })
        startFftTask()
    }

    private fun startFftTask() {
        EXECUTOR.submit(Runnable {
            val localFft = FastFourierTransformer(DftNormalization.STANDARD)
            while (playing) {
                if (paused) {
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        break
                    }
                    continue
                }

                var input: DoubleArray? = null
                synchronized(SRC_ARRAY) {
                    if (srcCount >= Constant.SPECTRUM_TOTAL_NUMBER) {
                        input = DoubleArray(Constant.SPECTRUM_TOTAL_NUMBER)
                        var readIdx = srcWriteIndex
                        for (i in 0 until Constant.SPECTRUM_TOTAL_NUMBER) {
                            input!![i] = SRC_ARRAY[readIdx]
                            readIdx = (readIdx + 1) % Constant.SPECTRUM_TOTAL_NUMBER
                        }
                    }
                }

                if (input != null) {
                    try {
                        val complex = localFft.transform(input, TransformType.FORWARD)
                        val tempTrans = LinkedList<Double>()
                        for (i in 0 until Constant.SPECTRUM_TOTAL_NUMBER) {
                            tempTrans.add(complex[i].abs())
                        }
                        TRANS.clear()
                        TRANS.addAll(tempTrans)
                    } catch (e: Exception) {
                        log.error("FFT 计算异常", e)
                    }
                }

                try {
                    Thread.sleep(30)
                } catch (e: InterruptedException) {
                    break
                }
            }
        })
    }

    override fun stop() {
        this.playing = false
        if (this.data != null) {
            try {
                this.data!!.stop()
            } catch (e: Exception) {
                // 忽略停止时的异常
            }
            IoUtil.close(this.data)
            this.data = null
        }
        if (this.audio != null) {
            IoUtil.close(this.audio)
            this.audio = null
        }
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
        return data?.framePosition?.toDouble() ?: 0.0
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

    /**
     * 计算音频时长
     *
     * @param audio  音频流
     * @param format 音频格式
     * @return 音频时长
     * @date 2019年10月31日19:06:39
     */
    companion object {
        private val log = LoggerFactory.getLogger(SdlFftPlayer::class.java)

        /**
         * 环形采样数组以极致优化性能，规避对象装箱拆箱及垃圾回收开销
         */
        private val SRC_ARRAY = DoubleArray(Constant.SPECTRUM_TOTAL_NUMBER)
        private var srcWriteIndex = 0
        private var srcCount = 0

        /**
         * FFT 提取出的频域能量阵列结果
         */
        val TRANS: Deque<Double> = ConcurrentLinkedDeque()

        /**
         * 线程池（采用虚拟线程以极致优化异步开销）
         */
        private val EXECUTOR: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

        fun create(): SdlFftPlayer {
            return SingletonHolder.PLAYER
        }

        @JvmStatic
        fun getAudioDuration(audio: AudioInputStream, format: AudioFormat): Double {
            return audio.frameLength * format.frameSize /
                    (format.sampleRate * format.channels * (format.sampleSizeInBits / 8.0))
        }

        private object SingletonHolder {
            val PLAYER = SdlFftPlayer()
        }
    }

    private fun setSpectrum(buff: ByteArray, channels: Int, sample: Int) {
        if (buff.size != 4) {
            return
        }

        var value = 0.0
        // 立体声
        if (channels == 2) {
            if (sample == 16) {
                val left = ((buff[1].toInt() shl 8) or (buff[0].toInt() and 0xFF)).toShort()
                val right = ((buff[3].toInt() shl 8) or (buff[2].toInt() and 0xFF)).toShort()
                value = (left + right) / 2.0 / 32768.0
            } else {
                val left = (buff[0].toInt() and 0xFF) / 128.0 - 1.0
                val right = (buff[1].toInt() and 0xFF) / 128.0 - 1.0
                value = (left + right) / 2.0
            }
        } else {
            // 单声道
            if (sample == 16) {
                value = ((buff[1].toInt() shl 8) or (buff[0].toInt() and 0xFF)).toShort() / 32768.0
            } else {
                value = (buff[0].toInt() and 0xFF) / 128.0 - 1.0
            }
        }

        synchronized(SRC_ARRAY) {
            SRC_ARRAY[srcWriteIndex] = value
            srcWriteIndex = (srcWriteIndex + 1) % Constant.SPECTRUM_TOTAL_NUMBER
            if (srcCount < Constant.SPECTRUM_TOTAL_NUMBER) {
                srcCount++
            }
        }
    }
}
