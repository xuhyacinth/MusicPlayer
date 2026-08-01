package com.xu.music.player.window

import cn.hutool.core.io.FileUtil
import cn.hutool.core.util.ArrayUtil
import cn.hutool.core.util.IdUtil
import com.xu.music.player.entity.SongEntity
import com.xu.music.player.wrapper.InsertWrapper
import com.xu.music.player.wrapper.QueryWrapper
import javafx.stage.FileChooser
import javafx.stage.Window
import java.io.File
import java.util.Date
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * 歌曲选择窗口
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class SongChoose {

    /**
     * 歌曲选择并插入数据库
     *
     * @param window 父窗口
     * @return 是否导入了歌曲
     * @date 2024年6月4日19点07分
     * @since idea
     */
    fun open(window: Window?): Boolean {
        return try {
            val dialog = FileChooser()
            dialog.title = "选择歌曲"
            val filter = FileChooser.ExtensionFilter(
                "音频文件", *arrayOf("*.mp3", "*.MP3", "*.wav", "*.WAV", "*.flac", "*.FLAC", "*.pcm", "*.PCM")
            )
            dialog.extensionFilters.add(filter)
            dialog.selectedExtensionFilter = filter
            val files = dialog.showOpenMultipleDialog(window)
            if (ArrayUtil.isEmpty(files)) {
                return false
            }

            var currentCount = 0
            try {
                val queryWrapper = QueryWrapper<SongEntity>(SongEntity::class.java, "song")
                val existing = queryWrapper.list()
                if (existing != null) {
                    currentCount = existing.size
                }
            } catch (e: Exception) {
                // 忽略异常，默认从 0 开始排序
            }

            for (audioFile in files) {
                if (!audioFile.exists()) {
                    continue
                }

                // 获取音频长度
                val duration = getAudioDuration(audioFile)

                val song = SongEntity()
                song.id = IdUtil.fastSimpleUUID()
                val songName = FileUtil.mainName(audioFile)
                song.name = songName
                song.songPath = audioFile.absolutePath
                song.length = duration

                // 解析歌手
                if (songName.contains(" - ")) {
                    val parts = songName.split(" - ", limit = 2)
                    song.author = parts[0].trim()
                } else {
                    song.author = "未知歌手"
                }

                // 尝试匹配同名歌词
                val parent = audioFile.parent
                if (parent != null) {
                    val lyricPath = File(parent, "$songName.lrc")
                    if (FileUtil.exist(lyricPath)) {
                        song.lyricPath = lyricPath.absolutePath
                    } else {
                        val lyricPath2 = File(parent, "$songName.LRC")
                        if (FileUtil.exist(lyricPath2)) {
                            song.lyricPath = lyricPath2.absolutePath
                        }
                    }
                }

                song.flag = 1
                song.index = ++currentCount
                song.createTime = Date()
                song.updateTime = Date()

                // 插入数据库
                val insertWrapper = InsertWrapper<SongEntity>(song, "song")
                insertWrapper.insert()
            }
            true
        } catch (e: Exception) {
            throw RuntimeException("导入歌曲失败", e)
        }
    }

    /**
     * 获取音频时长
     *
     * @param file 音频文件
     * @return 时长(秒)
     */
    private fun getAudioDuration(file: File): Double {
        // 优先通过文件格式元数据获取属性(主要支持 MP3 SPI)
        try {
            val fileFormat = AudioSystem.getAudioFileFormat(file)
            val properties = fileFormat.properties()
            if (properties != null && properties.containsKey("duration")) {
                val durationObj = properties["duration"]
                if (durationObj is Long) {
                    return durationObj / 1_000_000.0
                } else if (durationObj is Number) {
                    return durationObj.toDouble() / 1_000_000.0
                }
            }
        } catch (e: Exception) {
            // 忽略，尝试其它方式
        }

        // 通过音频输入流获取帧数与帧率计算(支持 WAV, FLAC 等)
        try {
            AudioSystem.getAudioInputStream(file).use { audioStream ->
                val format = audioStream.format
                val frameLength = audioStream.frameLength
                val frameRate = format.frameRate
                if (frameLength != AudioSystem.NOT_SPECIFIED.toLong() && frameRate > 0) {
                    return frameLength / frameRate.toDouble()
                }
            }
        } catch (e: Exception) {
            // 忽略
        }

        return 0.0
    }
}
