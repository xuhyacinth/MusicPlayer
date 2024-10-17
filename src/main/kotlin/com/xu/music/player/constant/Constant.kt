package com.xu.music.player.constant;

import java.io.Serializable;

import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

/**
 * 常量类
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class Constant implements Serializable {

    private static final long serialVersionUID = 1L;

    private Constant() {

    }

    /**
     * 播放器颜色
     */
    public static List<Color> COLORS = new ArrayList<>();

    static {
        for (int i = 1; i <= 16; i++) {
            COLORS.add(Utils.getColor(i));
        }
    }

    /**
     * 频谱长度
     */
    public static final int SPECTRUM_TOTAL_NUMBER = 128;

    /**
     * 播放列表
     */
    public static final Map<Integer, SongEntity> PLAYING_LIST = new LinkedHashMap<>();

    /**
     * 正在播放歌曲
     */
    public static volatile SongEntity PLAYING_SONG = null;

    /**
     * 正在播放歌曲
     */
    public static volatile boolean PLAYING_LYRIC = false;

    /**
     * 正在播放歌曲时长
     */
    public static volatile double PLAYING_SONG_LENGTH = 0;

    /**
     * 正在播放歌曲索引
     */
    public static volatile Integer PLAYING_INDEX = null;

    /**
     * 是否正在播放
     */
    public static volatile boolean MUSIC_PLAYER_PLAYING_STATE = true;

    /**
     * 频谱 前景颜色
     */
    public static volatile Color SPECTRUM_FOREGROUND_COLOR = Utils.getColor(SWT.COLOR_BLUE);

}
