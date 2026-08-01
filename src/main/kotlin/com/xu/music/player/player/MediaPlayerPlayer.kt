package com.xu.music.player.player

import cn.hutool.core.io.IoUtil
import cn.hutool.core.text.CharSequenceUtil
import javafx.scene.media.AudioSpectrumListener
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import javax.sound.sampled.AudioFileFormat.Type.WAVE
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * JavaFX MediaPlayer 音频播放
 *
 * 使用 javafx.scene.media.MediaPlayer 原生播放 MP3/WAV 等格式；
 * FLAC 因 JavaFX 官方不支持，通过 jflac(SPI 注册到 AudioSystem) 解码为临时 WAV 后播放。
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class MediaPlayerPlayer : Player {

    private val log = LoggerFactory.getLogger(MediaPlayerPlayer::class.java)

    /** JavaFX 媒体播放器 */
    private var mediaPlayer: MediaPlayer? = null

    /** 临时 FLAC 转 WAV 文件 */
    private var tempWav: File? = null

    /** 暂停状态（JavaFX MediaPlayer 无 pause 标志位，需自行维护） */
    @Volatile
    private var paused = false

    /** 播放状态 */
    @Volatile
    private var playing = false

    /** 播放结束回调（供 UI 注册自动下一曲） */
    var onEndOfMedia: (() -> Unit)? = null

    private val spectrumListener = AudioSpectrumListener { _: Double, _: Double, magnitudes: FloatArray?, _: FloatArray? ->
        TRANS.clear()
        if (magnitudes != null) {
            for (mag in magnitudes) {
                // AudioSpectrum 返回对数幅值(dB)，转为线性幅值用于柱状图
                TRANS.add(Math.pow(10.0, mag / 20.0))
            }
        }
    }

    @Throws(Exception::class)
    override fun load(url: URL?) {
        if (url == null) {
            throw IllegalArgumentException("URL 不能为空")
        }
        load(url.toURI().toString())
    }

    @Throws(Exception::class)
    override fun load(file: File?) {
        if (file == null) {
            throw IllegalArgumentException("文件不能为空")
        }
        if (!file.exists()) {
            throw IllegalArgumentException("文件不存在: ${file.absolutePath}")
        }
        load(file.absolutePath)
    }

    @Throws(Exception::class)
    override fun load(path: String?) {
        if (path.isNullOrBlank()) {
            throw IllegalArgumentException("路径不能为空")
        }
        stop()

        val source = File(path)

        // FLAC 需要解码为临时 WAV（JavaFX 官方不支持 FLAC）
        if (CharSequenceUtil.endWithIgnoreCase(source.name, ".flac")) {
            tempWav = flacToTempWav(source)
            mediaPlayer = MediaPlayer(Media(tempWav!!.toURI().toString()))
        } else {
            mediaPlayer = MediaPlayer(Media(source.toURI().toString()))
        }

        bindListener()
    }

    @Throws(Exception::class)
    override fun load(stream: AudioInputStream?) {
        throw UnsupportedOperationException("MediaPlayerPlayer 不支持直接加载 AudioInputStream，请使用文件路径加载")
    }

    @Throws(Exception::class)
    override fun load(encoding: AudioFormat.Encoding?, stream: AudioInputStream?) {
        throw UnsupportedOperationException("MediaPlayerPlayer 不支持直接加载 AudioInputStream，请使用文件路径加载")
    }

    @Throws(Exception::class)
    override fun load(format: AudioFormat?, stream: AudioInputStream?) {
        throw UnsupportedOperationException("MediaPlayerPlayer 不支持直接加载 AudioInputStream，请使用文件路径加载")
    }

    override fun pause() {
        paused = true
        playing = false
        mediaPlayer?.pause()
    }

    override fun resume(duration: Long) {
        paused = false
        playing = true
        mediaPlayer?.play()
        if (duration > 0) {
            mediaPlayer?.seek(Duration.seconds(duration.toDouble()))
        }
    }

    override fun play() {
        playing = true
        paused = false
        mediaPlayer?.play()
    }

    override fun stop() {
        playing = false
        paused = false
        mediaPlayer?.stop()
        mediaPlayer?.dispose()
        mediaPlayer = null
        // 清理临时 FLAC 转 WAV 文件
        tempWav?.let { wav ->
            try {
                Files.deleteIfExists(wav.toPath())
            } catch (e: Exception) {
                log.warn("清理临时 WAV 文件失败: {}", wav.absolutePath, e)
            }
            tempWav = null
        }
        TRANS.clear()
    }

    override fun volume(volume: Float) {
        mediaPlayer?.volume = volume.coerceIn(0f, 1f).toDouble()
    }

    override fun position(): Double {
        val current = mediaPlayer?.currentTime ?: return 0.0
        return if (current == Duration.UNKNOWN) 0.0 else current.toSeconds()
    }

    override fun duration(): Double {
        val total = mediaPlayer?.totalDuration ?: return 0.0
        return if (total == Duration.UNKNOWN) 0.0 else total.toSeconds()
    }

    override fun playing(): Boolean {
        return playing
    }

    override fun pausing(): Boolean {
        return paused
    }

    /**
     * 绑定事件监听：播放结束自动下一曲、频谱数据采集
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun bindListener() {
        mediaPlayer?.setOnEndOfMedia {
            playing = false
            paused = false
            onEndOfMedia?.invoke()
        }
        mediaPlayer?.audioSpectrumListener = spectrumListener
    }

    /**
     * FLAC 解码为临时 WAV 文件
     *
     * jflac 通过 SPI 注册到 AudioSystem，可直接读取 FLAC 为 AudioInputStream，
     * 再写为 WAV 临时文件供 JavaFX MediaPlayer 播放。
     *
     * @param flac FLAC 源文件
     * @return 临时 WAV 文件
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun flacToTempWav(flac: File): File {
        var stream: AudioInputStream? = null
        try {
            stream = AudioSystem.getAudioInputStream(flac)
            // 统一转为 PCM_SIGNED 16bit，确保 JavaFX 能识别
            val format = stream.format
            val pcm = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, 16, format.channels,
                format.channels * 2, format.sampleRate, false
            )
            val converted = AudioSystem.getAudioInputStream(pcm, stream)
            val temp = File.createTempFile("musicplayer_flac_", ".wav")
            temp.deleteOnExit()
            AudioSystem.write(converted, WAVE, temp)
            return temp
        } catch (e: Exception) {
            throw RuntimeException("FLAC 解码失败: ${flac.absolutePath}", e)
        } finally {
            IoUtil.close(stream)
        }
    }

    companion object {
        /**
         * 频谱频段幅值（AudioSpectrumListener 回调写入）
         */
        val TRANS: Deque<Double> = ConcurrentLinkedDeque()
    }
}
