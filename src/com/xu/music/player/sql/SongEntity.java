package com.xu.music.player.sql;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author hyacinth
 */
@Data
public class SongEntity {

    private String id;
    private String name;
    private String info;
    private String songPath;
    private String lyricPath;
    private Integer flag;
    private Integer index;
    private Integer lyric;
    private String author;
    private Double length;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

}
