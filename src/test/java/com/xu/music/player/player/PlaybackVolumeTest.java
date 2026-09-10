package com.xu.music.player.player;

import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.junit.Assert.*;

public class PlaybackVolumeTest {

    @Test
    public void defaultsToOriginalVolumeAndDoesNotModifyInput() {
        var volume = new PlaybackVolume();
        byte[] input = samples(32767, -32768, 1000, -1000);
        byte[] original = input.clone();
        byte[] output = new byte[input.length];
        volume.apply(input, 0, input.length, output, 16);
        assertArrayEquals(original, output);
        assertArrayEquals(original, input);
    }

    @Test
    public void scalesBothChannelsAndSignedSamples() {
        var volume = new PlaybackVolume();
        volume.setPercentage(50);
        byte[] input = samples(32767, -32768, 1000, -1000);
        byte[] output = new byte[input.length];
        volume.apply(input, 0, input.length, output, 16);
        assertArrayEquals(samples(16383, -16384, 500, -500), output);
        assertArrayEquals(samples(32767, -32768, 1000, -1000), input);
    }

    @Test
    public void zeroIsExactSilenceAndRestoringVolumeRestoresSound() {
        var volume = new PlaybackVolume();
        byte[] input = samples(32767, -32768, 1, -1);
        byte[] output = new byte[input.length];
        Arrays.fill(output, (byte) 127);
        volume.setPercentage(0);
        volume.apply(input, 0, input.length, output, 16);
        assertArrayEquals(new byte[input.length], output);
        volume.setPercentage(100);
        volume.apply(input, 0, input.length, output, 16);
        assertArrayEquals(input, output);
    }

    @Test
    public void clampsVolumeAndHonorsInputOffset() {
        var volume = new PlaybackVolume();
        byte[] input = samples(6000, -6000, 2000, -2000);
        byte[] output = new byte[4];
        volume.setPercentage(200);
        volume.apply(input, 4, 4, output, 16);
        assertArrayEquals(samples(2000, -2000), output);
        volume.setPercentage(-1);
        volume.apply(input, 4, 4, output, 16);
        assertArrayEquals(new byte[4], output);
    }

    @Test
    public void sameSettingAppliesToNewSessionsAndPartialWritesOnlyScaleOnce() throws Exception {
        var volume = new PlaybackVolume();
        volume.setPercentage(25);
        var firstOutput = new ByteArrayOutputStream();
        try (var session = session(volume, firstOutput)) {
            session.start();
            byte[] input = samples(8000, -8000, 4000, -4000);
            session.write(input, input.length);
            assertArrayEquals(samples(2000, -2000, 1000, -1000), firstOutput.toByteArray());
        }
        var secondOutput = new ByteArrayOutputStream();
        try (var session = session(volume, secondOutput)) {
            session.start();
            session.pause();
            volume.setPercentage(0);
            assertTrue(session.paused());
            session.resume();
            session.write(samples(8000, -8000), 4);
            volume.setPercentage(25);
            session.write(samples(8000, -8000), 4);
            assertArrayEquals(samples(0, 0, 2000, -2000), secondOutput.toByteArray());
        }
    }

    @Test
    public void seekingDoesNotResetVolume() throws Exception {
        var volume = new PlaybackVolume();
        volume.setPercentage(50);
        var output = new ByteArrayOutputStream();
        try (var session = session(volume, output)) {
            session.start();
            session.pause();
            assertTrue(session.requestSeek(0));
            session.applyPendingSeek();
            assertTrue(session.paused());
            session.resume();
            session.write(samples(8000, -8000), 4);
            assertArrayEquals(samples(4000, -4000), output.toByteArray());
        }
    }

    private static PlaybackSession session(PlaybackVolume volume, ByteArrayOutputStream output) {
        var format = new AudioFormat(44100, 16, 2, true, false);
        var line = (SourceDataLine) Proxy.newProxyInstance(SourceDataLine.class.getClassLoader(),
                new Class<?>[]{SourceDataLine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "available" -> 4096;
                    case "getLongFramePosition" -> 0L;
                    case "write" -> {
                        int length = Math.min(4, (int) args[2]);
                        output.write((byte[]) args[0], (int) args[1], length);
                        yield length;
                    }
                    default -> null;
                });
        PlaybackSession.AudioSource source = () -> new AudioInputStream(
                new ByteArrayInputStream(samples(8000, -8000)), format, 1);
        return new PlaybackSession(new AudioInputStream(ByteArrayInputStream.nullInputStream(), format, 1),
                line, format, new PcmSpectrumAnalyzer(512), source, volume);
    }

    private static byte[] samples(int... samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            bytes[i * 2] = (byte) samples[i];
            bytes[i * 2 + 1] = (byte) (samples[i] >> 8);
        }
        return bytes;
    }
}
