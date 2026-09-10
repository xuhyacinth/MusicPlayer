package com.xu.music.player.player;

/** 对小端有符号 PCM 应用音量，不依赖声卡提供增益或静音控件。 */
final class PlaybackVolume {
    private volatile int percentage = 100;

    void setPercentage(int percentage) {
        this.percentage = Math.clamp(percentage, 0, 100);
    }

    void apply(byte[] source, int offset, int length, byte[] output, int sampleSizeInBits) {
        int bytes = PcmSamples.bytesPerSample(sampleSizeInBits);
        int current = percentage;
        if (current == 100) {
            System.arraycopy(source, offset, output, 0, length);
            return;
        }
        for (int i = 0; i < length; i += bytes) {
            int sample = PcmSamples.read(source, offset + i, bytes);
            // 32 位采样乘百分比时使用长整型，避免溢出。
            int scaled = (int) ((long) sample * current / 100);
            PcmSamples.write(output, i, bytes, scaled);
        }
    }
}
