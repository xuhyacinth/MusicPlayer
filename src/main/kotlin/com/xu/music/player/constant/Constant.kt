package com.xu.music.player.constant

import com.xu.music.player.entity.SongEntity
import java.io.Serializable
import java.util.LinkedHashMap

/**
 * 常量类
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
object Constant : Serializable {

    /**
     * 频谱长度
     */
    const val SPECTRUM_TOTAL_NUMBER: Int = 512

    /**
     * 播放列表
     */
    val PLAYING_LIST: LinkedHashMap<Int, SongEntity> = LinkedHashMap()

    /**
     * 正在播放歌曲
     */
    @Volatile
    var PLAYING_SONG: SongEntity? = null

    /**
     * 是否播放歌词
     */
    @Volatile
    var PLAYING_LYRIC: Boolean = false

    /**
     * 正在播放歌曲时长
     */
    @Volatile
    var PLAYING_SONG_LENGTH: Double = 0.0

    /**
     * 正在播放歌曲索引
     */
    @Volatile
    var PLAYING_INDEX: Int? = null

    /**
     * 是否正在播放
     */
    @Volatile
    var MUSIC_PLAYER_PLAYING_STATE: Boolean = true

    /**
     * 频谱 前景颜色
     */
    @Volatile
    var SPECTRUM_FOREGROUND_COLOR: String = "#4169E1"
}
