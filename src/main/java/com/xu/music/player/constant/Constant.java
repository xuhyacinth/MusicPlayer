package com.xu.music.player.constant;

import java.io.File;
import java.io.Serializable;

import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
    public static SongEntity PLAYING_SONG = null;

    /**
     * 正在播放歌曲时长
     */
    public static double PLAYING_SONG_LENGTH = 0;

    /**
     * 正在播放歌曲索引
     */
    public static Integer PLAYING_INDEX = null;

    /**
     * 是否正在播放
     */
    public static boolean MUSIC_PLAYER_PLAYING_STATE = true;

    /**
     * 频谱 背景颜色
     */
    public static volatile Color SPECTRUM_BACKGROUND_COLOR = Utils.getColor(SWT.COLOR_WHITE);

    /**
     * 频谱 前景颜色
     */
    public static volatile Color SPECTRUM_FOREGROUND_COLOR = Utils.getColor(SWT.COLOR_BLUE);




    /**
     * 用户文件夹
     */
    public static final String SYSTEM_USER_HOME = System.getProperties().getProperty("user.home");

    /**
     * 用户文件名称
     */
    public static final String SYSTEM_USER_NAME = System.getProperties().getProperty("user.name");

    /**
     * 歌单存放文件名
     */
    public static final String MUSIC_PLAYER_SONG_LISTS_NAME = "MusicPlayer.song";

    /**
     * 歌单存放路径
     */
    public static final String MUSIC_PLAYER_SONG_LISTS_PATH = SYSTEM_USER_HOME + File.separator + ".MusicPlayer" + File.separator;

    /**
     * 歌单存放文件
     */
    public static final String MUSIC_PLAYER_SONG_LISTS_FULL_PATH = MUSIC_PLAYER_SONG_LISTS_PATH + MUSIC_PLAYER_SONG_LISTS_NAME;

    /**
     * 播放器日志
     */
    public static final String MUSIC_PLAYER_LOG = "Log.log";

    /**
     * 分割符
     */
    public static final String MUSIC_PLAYER_SYSTEM_SPLIT = "<-->";

    /**
     * 文件下载路径
     */
    public static final String MUSIC_PLAYER_DOWNLOAD_PATH = SYSTEM_USER_HOME + File.separator + ".MusicPlayer" + File.separator + "download" + File.separator;

    private static final long serialVersionUID = 1L;

    /**
     * 歌词
     *
     * @date 2020年1月10日12:54:08
     */
    public static LinkedList<String> PLAYING_SONG_LYRIC = new LinkedList<>();

    /**
     * 是否有歌词
     */
    public static volatile boolean PLAYING_SONG_HAVE_LYRIC = false;

    /**
     * 是否开启歌词
     */
    public static volatile boolean MUSIC_PLAYER_SYSTEM_START_LYRIC = true;

    /**
     * 是否开启频谱
     */
    public static volatile boolean MUSIC_PLAYER_SYSTEM_START_SPECTRUM = true;

    /**
     * 频谱 整个频谱的宽度
     */
    public static volatile int SPECTRUM_TOTAL_WIDTH = 0;

    /**
     * 频谱 整个频谱的高度
     */
    public static volatile int SPECTRUM_TOTAL_HEIGHT = 0;

    /**
     * 频谱 存储大小
     */
    public static volatile int SPECTRUM_SAVE_INIT_SIZE = 50;

    /**
     * 频谱 样式 0 条形 1方块
     */
    public static volatile int SPECTRUM_STYLE = 0;

    /**
     * 频谱 FFT
     */
    public static volatile boolean SPECTRUM_REAL_FFT = false;

    /**
     * 频谱 刷新时间间隔
     */
    public static volatile long SPECTRUM_REFLASH_TIME = 100;

    /**
     * 频谱 宽度
     */
    public static volatile int SPECTRUM_SPLIT_WIDTH = 5;

    /**
     * 文件下载 核心池大小
     */
    public static volatile int MUSIC_PLAYER_DOWNLOAD_CORE_POOL_SIZE = 10;

    /**
     * 文件下载 最大池
     */
    public static volatile int MUSIC_PLAYER_DOWNLOAD_MAX_POOL_SIZE = 15;

    /**
     * 文件下载 每个线程下载10M
     */
    public static volatile long MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD = 10 * 1024 * 1024;

}
