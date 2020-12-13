package com.xu.music.player.lyric;

/**
 * Java MusicPlayer 歌词线程通知
 *
 * @Author: hyacinth
 * @ClassName: LyricyNotify
 * @Description: TODO
 * @Date: 2019年12月26日 下午8:01:37
 * @Copyright: hyacinth
 */
public interface LyricyNotify {

    /**
     * Java MusicPlayer 歌词线程通知
     * @param lrc
     * @param pro
     */
    void lyric(double lrc, double pro);

}
