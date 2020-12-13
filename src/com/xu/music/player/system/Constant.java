package com.xu.music.player.system;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Constant {

    /**
     * 用户文件夹
     */
    public static final String SYSTEM_USER_HOME = System.getProperties().getProperty("user.home");// 用户文件夹

    /**
     * 用户文件名称
     */
    public static final String SYSTEM_USER_NAME = System.getProperties().getProperty("user.name");// 用户文件名称

    /**
     * 歌单存放文件名
     */
    public static final String MUSIC_PLAYER_SONG_LISTS_NAME = "MusicPlayer.song";// 歌单存放文件名

    /**
     * 歌单存放路径
     */
    public static final String MUSIC_PLAYER_SONG_LISTS_PATH = SYSTEM_USER_HOME + File.separator + ".MusicPlayer" + File.separator;// 歌单存放路径

    /**
     * 歌单存放文件
     */
    public static final String MUSIC_PLAYER_SONG_LISTS_FULL_PATH = MUSIC_PLAYER_SONG_LISTS_PATH + MUSIC_PLAYER_SONG_LISTS_NAME;// 歌单存放文件

    /**
     * 播放器日志
     */
    public static final String MUSIC_PLAYER_LOG = "Log.log";// 播放器日志
    /**
     * 分割符
     */
    public static final String MUSIC_PLAYER_SYSTEM_SPLIT = "<-->";// 分割符
    /**
     * 文件下载路径
     */
    public static final String MUSIC_PLAYER_DOWNLOAD_PATH = SYSTEM_USER_HOME + File.separator + ".MusicPlayer" + File.separator + "download" + File.separator;// 文件下载路径
    /**
     * 播放器颜色
     */
    public static List<Color> MUSIC_PLAYER_COLORS = new ArrayList<Color>();//播放器颜色
    /**
     * 播放列表
     */
    public static LinkedList<String> MUSIC_PLAYER_SONGS_LIST = new LinkedList<String>();// 播放列表
    /**
     * 临时播放列表
     *
     * @date 2020年1月10日12:54:08
     */
    public static LinkedList<String> MUSIC_PLAYER_SONGS_TEMP_LIST = new LinkedList<String>();// 临时播放列表
    /**
     * 歌词
     *
     * @date 2020年1月10日12:54:08
     */
    public static LinkedList<String> PLAYING_SONG_LYRIC = new LinkedList<String>();// 歌词
    /**
     * 是否有歌词
     */
    public static volatile boolean PLAYING_SONG_HAVE_LYRIC = false;// 是否有歌词
    /**
     * 是否开启歌词
     */
    public static volatile boolean MUSIC_PLAYER_SYSTEM_START_LYRIC = true;// 是否开启歌词
    /**
     * 是否开启频谱
     */
    public static volatile boolean MUSIC_PLAYER_SYSTEM_START_SPECTRUM = true;// 是否开启频谱
    /**
     * 正在播放歌曲索引
     */
    public static int PLAYING_SONG_INDEX = 0;// 正在播放歌曲索引
    /**
     * 正在播放歌曲播放时长
     */
    public static int PLAYING_SONG_LENGTH = 0;// 正在播放歌曲播放时长
    /**
     * 正在播放歌曲
     */
    public static String PLAYING_SONG_NAME = "";// 正在播放歌曲
    /**
     * 是否正在播放
     */
    public static boolean MUSIC_PLAYER_PLAYING_STATE = true;// 是否正在播放
    /**
     * 频谱 背景颜色
     */
    public static volatile Color SPECTRUM_BACKGROUND_COLOR = Color.WHITE;// 频谱 背景颜色
    /**
     * 频谱 前景颜色
     */
    public static volatile Color SPECTRUM_FOREGROUND_COLOR = Color.BLUE;// 频谱 前景颜色
    /**
     * 频谱 整个频谱的宽度
     */
    public static volatile int SPECTRUM_TOTAL_WIDTH = 0;// 频谱 整个频谱的宽度
    /**
     * 频谱 整个频谱的高度
     */
    public static volatile int SPECTRUM_TOTAL_HEIGHT = 0;// 频谱 整个频谱的高度
    /**
     * 频谱 显示的频谱的数量
     */
    public static volatile int SPECTRUM_TOTAL_NUMBER = 0;// 频谱 显示的频谱的数量
    /**
     * 频谱 存储大小
     */
    public static volatile int SPECTRUM_SAVE_INIT_SIZE = 50;// 频谱 存储大小
    /**
     * 频谱 样式 0 条形 1方块
     */
    public static volatile int SPECTRUM_STYLE = 0;// 频谱 样式 0 条形 1方块
    /**
     * 频谱 FFT
     */
    public static volatile boolean SPECTRUM_REAL_FFT = false;// 频谱 FFT
    /**
     * 频谱 刷新时间间隔
     */
    public static volatile long SPECTRUM_REFLASH_TIME = 100;// 频谱 刷新时间间隔
    /**
     * 频谱 宽度
     */
    public static volatile int SPECTRUM_SPLIT_WIDTH = 5;// 频谱 宽度
    /**
     * 文件下载 核心池大小
     */
    public static volatile int MUSIC_PLAYER_DOWNLOAD_CORE_POOL_SIZE = 10;// 文件下载 核心池大小

    /**
     * 文件下载 最大池
     */
    public static volatile int MUSIC_PLAYER_DOWNLOAD_MAX_POOL_SIZE = 15;// 文件下载 最大池

    /**
     * 文件下载 每个线程下载10M
     */
    public static volatile long MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD = 10 * 1024 * 1024;// 文件下载 每个线程下载10M

}
