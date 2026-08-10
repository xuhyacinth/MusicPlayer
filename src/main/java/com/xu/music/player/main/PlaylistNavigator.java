package com.xu.music.player.main;

/**
 * 播放列表索引计算。
 */
public final class PlaylistNavigator {

    private PlaylistNavigator() {
    }

    public static int move(int current, int size, int delta) {
        if (size <= 0) {
            throw new IllegalArgumentException("播放列表不能为空");
        }
        return Math.floorMod(current + delta, size);
    }
}
