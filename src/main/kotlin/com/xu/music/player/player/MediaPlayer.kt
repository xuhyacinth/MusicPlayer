package com.xu.music.player.player

import javafx.application.Platform
import javafx.scene.media.AudioSpectrumListener
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import java.io.File
import java.net.URL
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream

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
class MediaPlayer : Player {

    /** JavaFX 媒体播放器 */
    private var mediaPlayer: MediaPlayer? = null

    /** 音量设置跨歌曲保留，也允许在加载前调节。 */
    private var currentVolume = 1.0

    /** 临时 FLAC 转 WAV 文件 */
    private var preparedAudio: PreparedAudio? = null
    private val loadLock = Any()
    @Volatile private var loadGeneration = 0L
    private var loadThread: Thread? = null
    private var pendingAudio: PreparedAudio? = null

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
                // AudioSpectrum 返回频段幅值(dB)，范围约 -80(阈值) ~ 0，直接存 dB 供 UI 归一化绘制
                TRANS.add(mag.toDouble())
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

        install(PreparedAudio.prepare(File(path)))
    }

    /** 耗时解码在后台执行；媒体对象和回调始终在 JavaFX 线程上操作。 */
    override fun loadAsync(path: String, onLoaded: () -> Unit, onError: (Exception) -> Unit) {
        check(Platform.isFxApplicationThread()) { "异步加载必须由 JavaFX 线程发起" }
        stop()
        val generation = loadGeneration
        fun fail(error: Exception) {
            if (generation != loadGeneration) return
            stop()
            onError(error)
        }
        loadThread = Thread({
            try {
                val audio = PreparedAudio.prepare(File(path))
                synchronized(loadLock) {
                    if (generation != loadGeneration) {
                        audio.close()
                        return@Thread
                    }
                    pendingAudio = audio
                    Platform.runLater {
                        if (generation != loadGeneration) return@runLater
                        synchronized(loadLock) { pendingAudio = null; loadThread = null }
                        try {
                            install(audio)
                            val native = mediaPlayer!!
                            native.setOnError { fail(native.error ?: IllegalStateException("音频加载失败")) }
                            native.setOnReady {
                                if (generation == loadGeneration) {
                                    try { onLoaded() } catch (e: Exception) { fail(e) }
                                }
                            }
                            native.error?.let { fail(it) }
                        } catch (e: Exception) {
                            fail(e)
                        }
                    }
                }
            } catch (e: Exception) {
                Platform.runLater { fail(e) }
            }
        }, "musicplayer-audio-loader").apply { isDaemon = true; start() }
    }

    private fun install(audio: PreparedAudio) {
        preparedAudio = audio
        try {
            mediaPlayer = MediaPlayer(Media(audio.file.toURI().toString()))
            bindListener()
        } catch (e: Exception) {
            mediaPlayer?.dispose()
            mediaPlayer = null
            preparedAudio = null
            audio.close()
            throw e
        }
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
        synchronized(loadLock) {
            loadGeneration++
            loadThread?.interrupt()
            loadThread = null
            pendingAudio?.close()
            pendingAudio = null
        }
        playing = false
        paused = false
        mediaPlayer?.stop()
        mediaPlayer?.dispose()
        mediaPlayer = null
        preparedAudio?.close()
        preparedAudio = null
        TRANS.clear()
    }

    override fun volume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f).toDouble()
        mediaPlayer?.volume = currentVolume
    }

    override fun seek(seconds: Double) {
        val total = duration()
        if (!seconds.isFinite() || !total.isFinite() || total <= 0.0) return
        mediaPlayer?.seek(Duration.seconds(seconds.coerceIn(0.0, total)))
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
        mediaPlayer?.volume = currentVolume
        val native = mediaPlayer
        val generation = loadGeneration
        mediaPlayer?.setOnEndOfMedia {
            if (generation != loadGeneration || native !== mediaPlayer) return@setOnEndOfMedia
            playing = false
            paused = false
            onEndOfMedia?.invoke()
        }
        // 配置频谱采集：128 个频段、阈值 -80dB、更新间隔 0.05s
        mediaPlayer?.audioSpectrumNumBands = 128
        mediaPlayer?.audioSpectrumThreshold = -80
        mediaPlayer?.audioSpectrumInterval = 0.05
        mediaPlayer?.audioSpectrumListener = spectrumListener
    }

    companion object {
        /**
         * 频谱频段幅值（AudioSpectrumListener 回调写入）
         */
        val TRANS: Deque<Double> = ConcurrentLinkedDeque()
    }
}
