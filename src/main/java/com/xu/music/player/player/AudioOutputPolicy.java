package com.xu.music.player.player;

import javax.sound.sampled.AudioFormat;
import java.util.Locale;
import java.util.function.Predicate;

/** 优先原始采样率及位深，仅在用户明确选择兼容播放时允许降为 16 位。 */
final class AudioOutputPolicy {
    private AudioOutputPolicy() {
    }

    static AudioFormat select(AudioFormat source, boolean allowCompatibility,
                              Predicate<AudioFormat> supported) throws UnsupportedAudioOutputException {
        int bits = source.getSampleSizeInBits();
        // MP3 等压缩格式未声明 PCM 位深，沿用解码器常用的 16 位输出。
        if (bits <= 0) bits = 16;
        var preferred = pcm(source, bits);
        if (supported.test(preferred)) return preferred;

        var compatible = pcm(source, 16);
        boolean canReduce = bits > 16 && supported.test(compatible);
        if (allowCompatibility && canReduce) return compatible;
        throw new UnsupportedAudioOutputException(
                "当前 Java Sound 输出通道不支持 " + describe(preferred)
                        + "。未自动降低位深。", canReduce);
    }

    private static AudioFormat pcm(AudioFormat source, int bits) {
        int bytes = PcmSamples.bytesPerSample(bits);
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, source.getSampleRate(), bits,
                source.getChannels(), source.getChannels() * bytes, source.getSampleRate(), false);
    }

    static String describe(AudioFormat format) {
        String rate = String.format(Locale.ROOT, "%.3f", format.getSampleRate() / 1000.0)
                .replaceAll("0+$", "").replaceAll("\\.$", "");
        String bits = format.getSampleSizeInBits() > 0 ? format.getSampleSizeInBits() + "bit" : "压缩";
        return rate + "kHz/" + bits + "/" + format.getChannels() + "ch";
    }

    static String describeRoute(AudioFormat source, AudioFormat output) {
        return "源 " + describe(source) + " → 输出 " + describe(output)
                + (source.getSampleSizeInBits() > output.getSampleSizeInBits() ? "（兼容）" : "");
    }
}
