package com.xu.music.player.main;

/**
 * 播放进度换算。
 */
public final class PlaybackProgress {

    private PlaybackProgress() {
    }

    public static int percentage(double position, double duration) {
        if (duration <= 0) {
            return 0;
        }
        return Math.clamp((int) Math.round(position * 100 / duration), 0, 100);
    }
}
