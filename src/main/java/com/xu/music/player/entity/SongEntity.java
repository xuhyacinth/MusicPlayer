package com.xu.music.player.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体类
 *
 * @since 2024年6月4日19点07分
 * @version swt-java/v1.0.0
 */
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Integer getFlag() {
        return flag;
    }

    public void setFlag(Integer flag) {
        this.flag = flag;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getLyricPath() {
        return lyricPath;
    }

    public void setLyricPath(String lyricPath) {
        this.lyricPath = lyricPath;
    }

    public String getLyricInfo() {
        return lyricInfo;
    }

    public void setLyricInfo(String lyricInfo) {
        this.lyricInfo = lyricInfo;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public String getSongPath() {
        return songPath;
    }

    public void setSongPath(String songPath) {
        this.songPath = songPath;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
