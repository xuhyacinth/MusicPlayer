package com.xu.music.player.player;

/**
 * 对小端 16 位 PCM 应用音量，不依赖声卡是否提供增益或静音控件。
 */
final class PlaybackVolume {

    private volatile int percentage = 100;

    void setPercentage(int percentage) {
        this.percentage = Math.clamp(percentage, 0, 100);
    }

    void apply(byte[] source, int offset, int length, byte[] output) {
        int current = percentage;
        if (current == 100) {
            System.arraycopy(source, offset, output, 0, length);
            return;
        }
        for (int i = 0; i < length; i += 2) {
            int sample = (short) ((source[offset + i] & 0xff) | (source[offset + i + 1] << 8));
            int scaled = sample * current / 100;
            output[i] = (byte) scaled;
            output[i + 1] = (byte) (scaled >> 8);
        }
    }
}
