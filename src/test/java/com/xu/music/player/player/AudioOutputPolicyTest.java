package com.xu.music.player.player;

import javax.sound.sampled.AudioFormat;
import org.junit.Test;
import static org.junit.Assert.*;

public class AudioOutputPolicyTest {
    private static final AudioFormat FLAC24 = new AudioFormat(new AudioFormat.Encoding("FLAC"),
            48000, 24, 2, -1, -1, false);

    @Test public void prefersOriginalPrecisionEvenWhenCompatibilityWasAllowed() throws Exception {
        for (boolean allowed : new boolean[]{false, true}) {
            var output = AudioOutputPolicy.select(FLAC24, allowed, format -> true);
            assertEquals(24, output.getSampleSizeInBits());
            assertEquals(48000, output.getSampleRate(), 0);
            assertEquals(2, output.getChannels());
            assertEquals(6, output.getFrameSize());
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, output.getEncoding());
            assertFalse(output.isBigEndian());
        }
    }

    @Test public void neverSilentlyReducesPrecision() {
        var error = assertThrows(UnsupportedAudioOutputException.class,
                () -> AudioOutputPolicy.select(FLAC24, false, format -> format.getSampleSizeInBits() == 16));
        assertTrue(error.compatibilityAvailable());
        assertTrue(error.getMessage().contains("24bit"));
    }

    @Test public void explicitCompatibilityKeepsSampleRateAndChannels() throws Exception {
        var output = AudioOutputPolicy.select(FLAC24, true, format -> format.getSampleSizeInBits() == 16);
        assertEquals(16, output.getSampleSizeInBits());
        assertEquals(48000, output.getSampleRate(), 0);
        assertEquals(2, output.getChannels());
        assertEquals(4, output.getFrameSize());
        assertTrue(AudioOutputPolicy.describeRoute(FLAC24, output).contains("兼容"));
    }

    @Test public void noDeviceDoesNotOfferUnusableCompatibility() {
        for (boolean allowed : new boolean[]{false, true}) {
            var error = assertThrows(UnsupportedAudioOutputException.class,
                    () -> AudioOutputPolicy.select(FLAC24, allowed, format -> false));
            assertFalse(error.compatibilityAvailable());
        }
    }

    @Test public void preservesOtherPcmDepthsAndRates() throws Exception {
        for (int bits : new int[]{8, 16, 24, 32}) {
            var source = new AudioFormat(96000, bits, 1, true, true);
            var output = AudioOutputPolicy.select(source, false, format -> true);
            assertEquals(bits, output.getSampleSizeInBits());
            assertEquals(96000, output.getSampleRate(), 0);
            assertEquals(1, output.getChannels());
            assertEquals(bits / 8, output.getFrameSize());
        }
    }

    @Test public void unspecifiedCompressedDepthUses16BitWithoutInventingSourceDepth() throws Exception {
        var source = new AudioFormat(new AudioFormat.Encoding("MPEG1L3"), 44100, -1, 2, -1, -1, false);
        var output = AudioOutputPolicy.select(source, false, format -> true);
        assertEquals(16, output.getSampleSizeInBits());
        assertTrue(AudioOutputPolicy.describeRoute(source, output).contains("44.1kHz/压缩"));
        assertFalse(AudioOutputPolicy.describeRoute(source, output).contains("兼容"));
    }
}
