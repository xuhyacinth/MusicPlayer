package com.xu.music.player.player;

import org.junit.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PcmSpectrumAnalyzerTest {

    @Test
    public void decodesStereo16BitLittleEndianToMono() {
        var analyzer = new PcmSpectrumAnalyzer(8);

        var sample = analyzer.decodeFrame(new byte[]{0x00, 0x40, 0x00, 0x40}, 0, 2, 16);

        assertEquals(0.5, sample, 0.0001);
    }

    @Test
    public void publishesDefensiveSpectrumSnapshot() {
        var analyzer = new PcmSpectrumAnalyzer(8);
        var format = new AudioFormat(44_100, 16, 1, true, false);
        var samples = new byte[]{
                0x00, 0x00, 0x00, 0x20, 0x00, 0x40, 0x00, 0x20,
                0x00, 0x00, 0x00, (byte) 0xE0, 0x00, (byte) 0xC0, 0x00, (byte) 0xE0
        };

        analyzer.accept(samples, 0, samples.length, format);
        analyzer.updateSpectrum();
        var first = analyzer.spectrumSnapshot();
        first[0] = 99;

        assertEquals(4, first.length);
        assertNotEquals(99, analyzer.spectrumSnapshot()[0], 0.0);
    }
}
