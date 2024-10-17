package com.xu.music.player.constant

import com.xu.music.player.entity.SongEntity
import com.xu.music.player.utils.Utils.getColor
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import java.io.Serializable
import kotlin.concurrent.Volatile

/**
 * 常量类
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
object Constant : Serializable {

    private const val serialVersionUID = 1L

    /**
     * 播放器颜色
     */
    @JvmField
    var COLORS: MutableList<Color> = ArrayList()

    init {
        for (i in 1..16) {
            COLORS.add(getColor(i))
        }
    }

    /**
     * 频谱长度
     */
    const val SPECTRUM_TOTAL_NUMBER: Int = 128

    /**
     * 播放列表
     */
    @JvmField
    val PLAYING_LIST: Map<Int, SongEntity> = LinkedHashMap()

    /**
     * 正在播放歌曲
     */
    @JvmField
    @Volatile
    var PLAYING_SONG: SongEntity? = null

    /**
     * 正在播放歌曲
     */
    @JvmField
    @Volatile
    var PLAYING_LYRIC: Boolean = false

    /**
     * 正在播放歌曲时长
     */
    @JvmField
    @Volatile
    var PLAYING_SONG_LENGTH: Double = 0.0

    /**
     * 正在播放歌曲索引
     */
    @JvmField
    @Volatile
    var PLAYING_INDEX: Int? = null

    /**
     * 是否正在播放
     */
    @JvmField
    @Volatile
    var MUSIC_PLAYER_PLAYING_STATE: Boolean = true

    /**
     * 频谱 前景颜色
     */
    @JvmField
    @Volatile
    var SPECTRUM_FOREGROUND_COLOR: Color = getColor(SWT.COLOR_BLUE)
}
