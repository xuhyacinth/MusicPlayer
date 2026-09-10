package com.xu.music.player.player;

import org.junit.Test;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

public class HighPrecisionPcmTest {
    @Test public void reads24BitSignedSamplesIncludingLowBits() {
        byte[] bytes = {1, 0, 0, -1, -1, -1, 0, 0, -128, -1, -1, 127};
        assertEquals(1, PcmSamples.read(bytes, 0, 3));
        assertEquals(-1, PcmSamples.read(bytes, 3, 3));
        assertEquals(-8388608, PcmSamples.read(bytes, 6, 3));
        assertEquals(8388607, PcmSamples.read(bytes, 9, 3));
        byte[] output = new byte[12];
        PcmSamples.write(output, 0, 3, 1);
        PcmSamples.write(output, 3, 3, -1);
        PcmSamples.write(output, 6, 3, -8388608);
        PcmSamples.write(output, 9, 3, 8388607);
        assertArrayEquals(bytes, output);
    }

    @Test public void volumePreservesAll24BitsAt100AndScalesSignedSamples() {
        var volume = new PlaybackVolume();
        byte[] source = samples(3, 8388607, -8388608, 257, -257);
        byte[] original = source.clone();
        byte[] output = new byte[source.length];
        volume.apply(source, 0, source.length, output, 24);
        assertArrayEquals(source, output);
        volume.setPercentage(50);
        volume.apply(source, 0, source.length, output, 24);
        assertArrayEquals(samples(3, 4194303, -4194304, 128, -128), output);
        volume.setPercentage(0);
        volume.apply(source, 0, source.length, output, 24);
        assertArrayEquals(new byte[source.length], output);
        volume.setPercentage(100);
        volume.apply(source, 6, 6, output, 24);
        assertEquals(257, PcmSamples.read(output, 0, 3));
        assertEquals(-257, PcmSamples.read(output, 3, 3));
        assertArrayEquals(original, source);
    }

    @Test public void volume32BitDoesNotOverflow() {
        var volume = new PlaybackVolume();
        volume.setPercentage(50);
        var input = samples(4, Integer.MAX_VALUE, Integer.MIN_VALUE);
        var output = new byte[input.length];
        volume.apply(input, 0, input.length, output, 32);
        assertArrayEquals(samples(4, 1073741823, -1073741824), output);
    }

    @Test public void spectrumUses24BitPrecisionWithoutModifyingPlaybackBytes() {
        var analyzer = new PcmSpectrumAnalyzer(8);
        assertEquals(1.0 / 8388608, analyzer.decodeFrame(samples(3, 1, 1), 0, 2, 24), 0);
        assertEquals(-1, analyzer.decodeFrame(samples(3, -8388608), 0, 1, 24), 0);
        assertEquals(8388607.0 / 8388608, analyzer.decodeFrame(samples(3, 8388607), 0, 1, 24), 0);
        var format = new AudioFormat(48000, 24, 1, true, false);
        byte[] input = samples(3, 0, 2097152, 4194304, 2097152, 0, -2097152, -4194304, -2097152);
        byte[] original = input.clone();
        analyzer.accept(input, 0, input.length, format);
        analyzer.updateSpectrum();
        assertArrayEquals(original, input);
        assertTrue(analyzer.spectrumSnapshot()[1] > 0);
    }

    @Test public void partialStereoWritesUseSixByteFramesAndScaleOnce() throws Exception {
        var output = new ByteArrayOutputStream();
        var format = new AudioFormat(48000, 24, 2, true, false);
        var line = (SourceDataLine) Proxy.newProxyInstance(SourceDataLine.class.getClassLoader(),
                new Class<?>[]{SourceDataLine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "available" -> 13;
                    case "getLongFramePosition" -> 0L;
                    case "write" -> {
                        int length = Math.min(6, (int) args[2]);
                        assertEquals(0, (int) args[2] % 6);
                        output.write((byte[]) args[0], (int) args[1], length);
                        yield length;
                    }
                    default -> null;
                });
        var volume = new PlaybackVolume();
        volume.setPercentage(50);
        try (var session = new PlaybackSession(new AudioInputStream(new ByteArrayInputStream(new byte[0]),
                format, 0), line, format, new PcmSpectrumAnalyzer(8), null, volume)) {
            session.start();
            var input = samples(3, 8388607, -8388608, 257, -257);
            session.write(input, input.length);
            assertArrayEquals(samples(3, 4194303, -4194304, 128, -128), output.toByteArray());
        }
    }

    private static byte[] samples(int bytes, int... values) {
        byte[] data = new byte[values.length * bytes];
        for (int i = 0; i < values.length; i++) PcmSamples.write(data, i * bytes, bytes, values[i]);
        return data;
    }
}
