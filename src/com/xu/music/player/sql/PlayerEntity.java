package com.xu.music.player.sql;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author hyacinth
 */
@Data
public class PlayerEntity {
    private String id;
    private String name;
    private String info;
    private String path;
    private Integer flag;
    private Integer index;
    private Integer lyric;
    private String author;
    private Double length;
    private String create_by;
    private LocalDateTime create_time;
    private String update_by;
    private LocalDateTime update_time;


}
