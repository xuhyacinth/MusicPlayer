package com.xu.music.player.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;

/**
 * 将音频解码与位深转换分开，统一首次播放和跳转后的 PCM 输出。
 */
final class PcmAudioConverter {

    private PcmAudioConverter() {
    }

    static AudioInputStream convert(AudioInputStream source, AudioFormat target) throws IOException {
        if (AudioSystem.isConversionSupported(target, source.getFormat())) {
            return AudioSystem.getAudioInputStream(target, source);
        }

        // FLAC 解码器保留原位深；仅显式选择兼容格式时再进行位深转换。
        var decoded = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, source);
        try {
            return AudioSystem.getAudioInputStream(target, decoded);
        } catch (RuntimeException exception) {
            try {
                decoded.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }
}
