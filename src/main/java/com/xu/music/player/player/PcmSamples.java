package com.xu.music.player.player;

/** 小端有符号 PCM 采样读写，保留 24 位采样的低八位。 */
final class PcmSamples {
    private PcmSamples() {
    }

    static int bytesPerSample(int bits) {
        if (bits != 8 && bits != 16 && bits != 24 && bits != 32) {
            throw new IllegalArgumentException("不支持的 PCM 位深：" + bits);
        }
        return bits / 8;
    }

    static int read(byte[] buffer, int offset, int bytes) {
        int value = buffer[offset + bytes - 1] << ((bytes - 1) * 8);
        for (int i = 0; i < bytes - 1; i++) {
            value |= (buffer[offset + i] & 0xff) << (i * 8);
        }
        return value;
    }

    static void write(byte[] buffer, int offset, int bytes, int value) {
        for (int i = 0; i < bytes; i++) {
            buffer[offset + i] = (byte) (value >> (i * 8));
        }
    }
}
