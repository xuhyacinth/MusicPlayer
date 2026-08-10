package com.xu.music.player.lyric;

/**
 * 一行带时间标签的歌词。
 */
public record LrcLine(double seconds, String tag, String text) {
}
