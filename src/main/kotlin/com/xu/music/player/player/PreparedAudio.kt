package com.xu.music.player.player

import java.io.File
import java.io.FilterInputStream
import java.io.InterruptedIOException
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import org.slf4j.LoggerFactory

/** 解码结果拥有临时文件；未采用的结果和停止播放时均需释放。 */
class PreparedAudio(val file: File, private val temporary: Boolean = false) : AutoCloseable {
    override fun close() {
        if (temporary) deleteTemporary(0)
    }

    private fun deleteTemporary(attempt: Int) {
        try {
            Files.deleteIfExists(file.toPath())
        } catch (e: Exception) {
            // Windows 媒体句柄异步释放，延迟重试，不能阻塞界面线程。
            if (attempt < 6) cleanup.schedule({ deleteTemporary(attempt + 1) }, 250, TimeUnit.MILLISECONDS)
            else LoggerFactory.getLogger(PreparedAudio::class.java).warn("清理临时 WAV 文件失败: {}", file, e)
        }
    }

    companion object {
        private val cleanup = Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "musicplayer-temp-cleanup").apply { isDaemon = true }
        }
        fun prepare(source: File): PreparedAudio {
            if (Thread.currentThread().isInterrupted) throw InterruptedIOException("取消音频加载")
            require(source.isFile) { "文件不存在: ${source.absolutePath}" }
            if (!source.extension.equals("flac", ignoreCase = true)) return PreparedAudio(source)
            // SPI 转换丢失样本数；必须在已声明的 PCM 末尾停止，不能再请求下一帧。
            val frames = AudioSystem.getAudioFileFormat(source).frameLength.toLong()
            require(frames > 0) { "FLAC 缺少有效的总样本数，无法安全解码" }
            val result = PreparedAudio(File.createTempFile("musicplayer_flac_", ".wav"), true)
            result.file.deleteOnExit()
            try {
                AudioSystem.getAudioInputStream(source).use { input ->
                    val format = input.format
                    val pcm = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.sampleRate,
                        format.sampleSizeInBits, format.channels,
                        format.channels * ((format.sampleSizeInBits + 7) / 8), format.sampleRate, false)
                    AudioSystem.getAudioInputStream(pcm, input).use { converted ->
                        var decodedBytes = 0L
                        val interruptible = object : FilterInputStream(converted) {
                            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                                if (Thread.currentThread().isInterrupted) throw InterruptedIOException("取消音频加载")
                                val count = super.read(bytes, offset, length)
                                if (count > 0) decodedBytes += count
                                return count
                            }
                        }
                        AudioInputStream(interruptible, pcm, frames).use { bounded ->
                            AudioSystem.write(bounded, AudioFileFormat.Type.WAVE, result.file)
                        }
                        check(decodedBytes == frames * pcm.frameSize) { "FLAC 数据不完整，解码样本数与声明不一致" }
                    }
                }
                if (Thread.currentThread().isInterrupted) throw InterruptedIOException("取消音频加载")
                return result
            } catch (e: Exception) {
                result.close()
                throw e
            }
        }
    }
}
