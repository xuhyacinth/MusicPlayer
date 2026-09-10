package com.xu.music.player.player;

import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class FlacPcmInputStreamTest {
    private static final AudioFormat PCM24 = new AudioFormat(48000, 24, 2, true, false);

    @Test(timeout = 5000)
    public void endsAtDeclaredSamplesEvenWithSyncByteInTail() throws Exception {
        for (int bits : new int[]{16, 24}) {
            byte[] original = fixture(bits);
            byte[] tagged = Arrays.copyOf(original, original.length + 1);
            tagged[original.length] = (byte) 0xff;
            var format = new AudioFormat(48000, bits, 2, true, false);
            try (var pcm = convert(new ByteArrayInputStream(tagged), format)) {
                assertEquals(4800, pcm.getFrameLength());
                assertArrayEquals(decode(original, format), pcm.readAllBytes());
                assertEquals(-1, pcm.read(new byte[12]));
                assertEquals(-1, pcm.read(new byte[12]));
            }
        }
    }

    @Test(timeout = 5000)
    public void unknownSampleCountReadsUntilPhysicalEnd() throws Exception {
        byte[] original = fixture(24);
        byte[] streaming = withoutSampleCount(original);
        try (var pcm = convert(new ByteArrayInputStream(streaming), PCM24)) {
            assertEquals(AudioSystem.NOT_SPECIFIED, pcm.getFrameLength());
            assertArrayEquals(decode(original, PCM24), pcm.readAllBytes());
            assertEquals(-1, pcm.read(new byte[6]));
        }
    }

    @Test(timeout = 5000)
    public void unknownLengthBadHeaderStopsInsteadOfRetryingForever() throws Exception {
        byte[] original = withoutSampleCount(fixture(24));
        byte[] malformed = Arrays.copyOf(original, original.length + 2);
        // 同步字后保留位为 1，旧解码器在 EOF 后会反复检查同一个坏帧头。
        malformed[original.length] = (byte) 0xff;
        malformed[original.length + 1] = (byte) 0xfa;
        try (var pcm = convert(new ByteArrayInputStream(malformed), PCM24)) {
            var error = assertThrows(IOException.class, pcm::readAllBytes);
            assertTrue(error.getMessage().contains("未取得进展"));
        }
    }

    @Test(timeout = 5000)
    public void truncatedDeclaredAudioReportsError() throws Exception {
        byte[] original = fixture(24);
        byte[] truncated = Arrays.copyOf(original, original.length - 100);
        try (var pcm = convert(new ByteArrayInputStream(truncated), PCM24)) {
            var error = assertThrows(IOException.class, pcm::readAllBytes);
            assertTrue(error.getMessage().contains("提前结束"));
        }
    }

    @Test(timeout = 5000)
    public void interruptionStopsResyncAndLeavesInterruptFlagSet() throws Exception {
        assertCancellation(true);
    }

    @Test(timeout = 5000)
    public void closeCancelsResyncWithoutWaitingForReadLock() throws Exception {
        assertCancellation(false);
    }

    private void assertCancellation(boolean interrupt) throws Exception {
        var input = new RepeatingBadFrames(withoutSampleCount(fixture(24)));
        var failure = new AtomicReference<Throwable>();
        var interrupted = new AtomicBoolean();
        try (var pcm = convert(input, PCM24)) {
            Thread reader = Thread.ofPlatform().daemon().name("flac-cancel-test").start(() -> {
                try {
                    pcm.readAllBytes();
                } catch (Throwable error) {
                    failure.set(error);
                    interrupted.set(Thread.currentThread().isInterrupted());
                }
            });
            try {
                assertTrue("应进入持续坏帧的输入", input.tailRead.await(2, TimeUnit.SECONDS));
                if (interrupt) reader.interrupt();
                else pcm.close();
                reader.join(2000);
                assertFalse("解码线程必须退出，而不是仅放弃等待", reader.isAlive());
                assertTrue(failure.get() instanceof IOException);
                if (interrupt) {
                    assertTrue(failure.get() instanceof InterruptedIOException);
                    assertTrue(interrupted.get());
                }
            } finally {
                reader.interrupt();
                pcm.close();
                reader.join(1000);
            }
        }
        assertTrue(input.closed);
    }

    private static byte[] fixture(int bits) throws IOException {
        try (var input = FlacPcmInputStreamTest.class.getResourceAsStream(
                "/audio/stereo-48000-" + bits + "bit.flac")) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    private static byte[] withoutSampleCount(byte[] original) {
        byte[] data = original.clone();
        // STREAMINFO 的总采样数占 36 位；0 表示长度未知，其余格式字段不变。
        data[21] &= (byte) 0xf0;
        Arrays.fill(data, 22, 26, (byte) 0);
        return data;
    }

    private static byte[] decode(byte[] data, AudioFormat format) throws Exception {
        try (var pcm = convert(new ByteArrayInputStream(data), format)) {
            return pcm.readAllBytes();
        }
    }

    private static AudioInputStream convert(InputStream input, AudioFormat target) throws Exception {
        return PcmAudioConverter.convert(AudioSystem.getAudioInputStream(new BufferedInputStream(input)), target);
    }

    private static final class RepeatingBadFrames extends InputStream {
        private final byte[] prefix;
        private int position;
        private final CountDownLatch tailRead = new CountDownLatch(1);
        private volatile boolean closed;

        private RepeatingBadFrames(byte[] prefix) { this.prefix = prefix; }

        @Override public int read() throws IOException {
            if (closed) throw new IOException("测试输入已关闭");
            if (position < prefix.length) return prefix[position++] & 0xff;
            tailRead.countDown();
            return ((position++ - prefix.length) & 1) == 0 ? 0xff : 0xfa;
        }

        @Override public int read(byte[] data, int offset, int length) throws IOException {
            for (int i = 0; i < length; i++) data[offset + i] = (byte) read();
            return length;
        }

        @Override public void close() { closed = true; }
    }
}
