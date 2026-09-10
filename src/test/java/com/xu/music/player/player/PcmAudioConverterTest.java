package com.xu.music.player.player;

import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PcmAudioConverterTest {
    private static final AudioFormat TARGET = new AudioFormat(48000, 16, 2, true, false);

    @Test
    public void decodes16BitFlacWithoutChangingSamples() throws Exception {
        assertDecodedSamples(16);
    }

    @Test
    public void decodes24BitFlacThroughIntermediatePcm() throws Exception {
        try (var source = openFlac(24)) {
            assertFalse(AudioSystem.isConversionSupported(TARGET, source.getFormat()));
            assertThrows(IllegalArgumentException.class,
                    () -> AudioSystem.getAudioInputStream(TARGET, source));
        }
        assertDecodedSamples(24);
    }

    @Test
    public void keepsExisting16BitPcmUnchanged() throws Exception {
        byte[] samples = {0, 0, -1, 127, 0, -128, -1, -1};
        try (var source = new AudioInputStream(new ByteArrayInputStream(samples), TARGET, 2);
             var pcm = PcmAudioConverter.convert(source, TARGET)) {
            assertArrayEquals(samples, pcm.readAllBytes());
        }
    }

    @Test
    public void seeks24BitFlacForwardAndBackwardUsingConvertedStreams() throws Exception {
        List<TrackedAudio> inputs = new ArrayList<>();
        PlaybackSession.AudioSource reopen = () -> {
            var input = new TrackedAudio(openFlac(24));
            inputs.add(input);
            return PcmAudioConverter.convert(input, TARGET);
        };
        var line = (SourceDataLine) Proxy.newProxyInstance(
                SourceDataLine.class.getClassLoader(), new Class<?>[]{SourceDataLine.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getLongFramePosition" -> 0L;
                    default -> null;
                });
        try (var session = new PlaybackSession(reopen.open(), line, TARGET,
                new PcmSpectrumAnalyzer(512), reopen)) {
            session.start();
            for (int frame : new int[]{2400, 480}) {
                assertTrue(session.requestSeek(frame / 48000.0));
                session.applyPendingSeek();
                assertEquals(frame / 48000.0, session.positionSeconds(), 0.000001);
                byte[] sample = session.audio().readNBytes(4);
                assertEquals(4, sample.length);
                assertFrame(sample, 0, frame, 24);
                assertTrue(inputs.get(inputs.size() - 2).closed);
            }
            assertEquals(3, inputs.size());
        }
        assertTrue(inputs.stream().allMatch(input -> input.closed));
    }

    @Test
    public void closesIntermediateStreamWhenTargetConversionFails() throws Exception {
        try (var source = new TrackedAudio(openFlac(24))) {
            var unsupported = new AudioFormat(new AudioFormat.Encoding("UNSUPPORTED_TEST"),
                    48000, 16, 2, 4, 48000, false);
            assertThrows(IllegalArgumentException.class,
                    () -> PcmAudioConverter.convert(source, unsupported));
            assertTrue(source.closed);
        }
    }

    @Test
    public void preservesEvery24BitSampleIncludingLowEightBits() throws Exception {
        var target = new AudioFormat(48000, 24, 2, true, false);
        try (var source = openFlac(24); var pcm = PcmAudioConverter.convert(source, target)) {
            byte[] data = pcm.readAllBytes();
            assertEquals(4800 * 6, data.length);
            for (int frame = 0; frame < 4800; frame++) assertOriginalFrame(data, frame * 6, frame);
        }
    }

    @Test
    public void native24BitSeekRetainsFrameBoundariesAndLowBits() throws Exception {
        var target = new AudioFormat(48000, 24, 2, true, false);
        List<TrackedAudio> inputs = new ArrayList<>();
        PlaybackSession.AudioSource reopen = () -> {
            var input = new TrackedAudio(openFlac(24));
            inputs.add(input);
            return PcmAudioConverter.convert(input, target);
        };
        var line = (SourceDataLine) Proxy.newProxyInstance(SourceDataLine.class.getClassLoader(),
                new Class<?>[]{SourceDataLine.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "getLongFramePosition" -> 0L;
                    default -> null;
                });
        try (var session = new PlaybackSession(reopen.open(), line, target,
                new PcmSpectrumAnalyzer(512), reopen)) {
            session.start();
            session.pause();
            for (int frame : new int[]{2400, 480}) {
                assertTrue(session.requestSeek(frame / 48000.0));
                session.applyPendingSeek();
                assertTrue(session.paused());
                assertEquals(frame / 48000.0, session.positionSeconds(), 0.000001);
                byte[] sample = session.audio().readNBytes(6);
                assertEquals(6, sample.length);
                assertOriginalFrame(sample, 0, frame);
                assertTrue(inputs.get(inputs.size() - 2).closed);
            }
        }
        assertTrue(inputs.stream().allMatch(input -> input.closed));
    }

    private static void assertOriginalFrame(byte[] data, int offset, int frame) {
        int expected = (((frame % 257 - 128) * 200) << 8) + frame % 251;
        assertEquals("左声道帧 " + frame, expected, PcmSamples.read(data, offset, 3));
        assertEquals("右声道帧 " + frame, -expected, PcmSamples.read(data, offset + 3, 3));
    }

    private static void assertDecodedSamples(int bits) throws Exception {
        try (var source = new TrackedAudio(openFlac(bits))) {
            assertEquals(bits, source.getFormat().getSampleSizeInBits());
            assertEquals("FLAC", source.getFormat().getEncoding().toString());
            try (var pcm = PcmAudioConverter.convert(source, TARGET)) {
                assertTrue(TARGET.matches(pcm.getFormat()));
                byte[] samples = pcm.readAllBytes();
                assertEquals(4800 * 4, samples.length);
                for (int frame = 0; frame < 4800; frame++) {
                    assertFrame(samples, frame * 4, frame, bits);
                }
            }
            assertTrue(source.closed);
        }
    }

    private static void assertFrame(byte[] samples, int offset, int frame, int bits) {
        double expectedLeft = (frame % 257 - 128) * 200 + (bits == 24 ? (frame % 251) / 256.0 : 0);
        int left = (short) ((samples[offset] & 0xff) | (samples[offset + 1] << 8));
        int right = (short) ((samples[offset + 2] & 0xff) | (samples[offset + 3] << 8));
        // 兼容转换经过浮点归一化和整数量化，误差限定为两个 16 位最低有效位；原位深另有逐字节精度测试。
        assertEquals("左声道帧 " + frame, expectedLeft, left, bits == 24 ? 2.0 : 0);
        assertEquals("右声道帧 " + frame, -expectedLeft, right, bits == 24 ? 2.0 : 0);
    }

    private static AudioInputStream openFlac(int bits) throws Exception {
        var resource = PcmAudioConverterTest.class.getResource("/audio/stereo-48000-" + bits + "bit.flac");
        assertNotNull(resource);
        return AudioSystem.getAudioInputStream(resource);
    }

    private static final class TrackedAudio extends AudioInputStream {
        boolean closed;

        TrackedAudio(AudioInputStream source) {
            super(source, source.getFormat(), source.getFrameLength());
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
