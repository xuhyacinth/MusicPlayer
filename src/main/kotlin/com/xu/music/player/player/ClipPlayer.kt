package com.xu.music.player.player

import cn.hutool.core.text.CharSequenceUtil
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.File
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine.Info
import javax.sound.sampled.FloatControl
import javax.sound.sampled.FloatControl.Type
import javax.sound.sampled.LineEvent

/**
 * Clip 音频播放
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
class ClipPlayer private constructor() : Player {

    /**
     * Clip
     */
    private var clip: Clip? = null

    /**
     * FloatControl
     */
    private var control: FloatControl? = null

    /**
     * 播放位置
     */
    @Volatile
    private var position: Long = 0

    /**
     * 暂停
     */
    @Volatile
    private var paused = false


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
        val info = Info(Clip::class.java, stream!!.format, AudioSystem.NOT_SPECIFIED)
        this.clip = AudioSystem.getLine(info) as Clip
        clip!!.addLineListener { event: LineEvent ->
            println(event.type.toString() + "\t" + event.framePosition)
        }
        clip!!.open(stream)
        if (clip!!.isControlSupported(Type.MASTER_GAIN)) {
            control = clip!!.getControl(Type.MASTER_GAIN) as FloatControl
        }
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
        if (this.clip != null && clip!!.isRunning) {
            paused = true
            position = clip!!.microsecondPosition
            clip!!.stop()
        }
    }

    override fun resume(duration: Long) {
        if (this.clip != null && paused) {
            paused = false
            this.position = if ((0L == duration)) position else duration
            clip!!.microsecondPosition = position
            clip!!.start()
        }
    }

    override fun play() {
        if (this.clip != null) {
            clip!!.start()
            clip!!.drain()
        }
    }

    override fun stop() {
        if (this.clip != null) {
            clip!!.stop()
            clip!!.close()
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

    private object SingletonHolder {
        val player: ClipPlayer = ClipPlayer()
    }

    companion object {
        fun createPlayer(): ClipPlayer {
            return SingletonHolder.player
        }
    }

}
