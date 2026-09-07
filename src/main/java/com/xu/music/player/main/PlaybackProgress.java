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

    public static double positionAt(int x, int width, double duration) {
        if (width <= 1 || !Double.isFinite(duration) || duration <= 0) {
            return 0;
        }
        return Math.clamp((double) x / (width - 1), 0, 1) * duration;
    }

    public static double duration(double reportedDuration, Double storedDuration) {
        if (Double.isFinite(reportedDuration) && reportedDuration > 0) {
            return reportedDuration;
        }
        return storedDuration != null && Double.isFinite(storedDuration) && storedDuration > 0
                ? storedDuration
                : 0;
    }
}
