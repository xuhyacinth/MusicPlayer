package com.xu.music.player.entity

import java.io.Serializable
import java.util.Date

/**
 * 实体类
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
data class SongEntity(

    /**
     * id
     */
    var id: String? = null,

    /**
     * 歌曲名称
     */
    var name: String? = null,

    /**
     * 歌曲信息
     */
    var info: String? = null,

    /**
     * 标志
     */
    var flag: Int? = null,

    /**
     * 排序
     */
    var index: Int? = null,

    /**
     * 歌词路径
     */
    var lyricPath: String? = null,

    /**
     * 歌词信息
     */
    var lyricInfo: String? = null,

    /**
     * 歌手
     */
    var author: String? = null,

    /**
     * 歌曲长度
     */
    var length: Double? = 0.0,

    /**
     * 歌曲路径
     */
    var songPath: String? = null,

    /**
     * 创建人
     */
    var createBy: String? = null,

    /**
     * 创建时间
     */
    var createTime: Date? = null,

    /**
     * 更新人
     */
    var updateBy: String? = null,

    /**
     * 更新时间
     */
    var updateTime: Date? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}