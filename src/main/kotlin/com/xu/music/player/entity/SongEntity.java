package com.xu.music.player.entity;

import java.io.Serializable;
import lombok.Data;

import java.util.Date;

/**
 * 实体类
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
@Data
public class SongEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private String id;

    /**
     * 歌曲名称
     */
    private String name;

    /**
     * 歌曲信息
     */
    private String info;

    /**
     * 标志
     */
    private Integer flag;

    /**
     * 排序
     */
    private Integer index;

    /**
     * 歌词路径
     */
    private String lyricPath;

    /**
     * 歌词信息
     */
    private String lyricInfo;

    /**
     * 歌手
     */
    private String author;

    /**
     * 歌曲长度
     */
    private Double length;

    /**
     * 歌曲路径
     */
    private String songPath;

    /**
     *
     */
    private String createBy;

    /**
     *
     */
    private Date createTime;

    /**
     *
     */
    private String updateBy;

    /**
     *
     */
    private Date updateTime;

}
