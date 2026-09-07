package com.xu.music.player.player;

import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class PlaybackSeekTest {

    @Test
    public void seeksForwardAndBackwardToActualPcmFrames() throws Exception {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            session.start();
            fixture.frames.set(100);
            assertTrue(session.requestSeek(7.25));
            assertEquals(7.25, session.positionSeconds(), 0.001);
            session.applyPendingSeek();

            assertEquals(725, readSample(session.audio()));
            assertEquals(7.25, session.positionSeconds(), 0.001);
            assertEquals(10, session.durationSeconds(), 0);
            assertTrue(fixture.running.get());
            assertTrue(fixture.streams.getFirst().closed);
            fixture.frames.addAndGet(50);
            assertEquals(7.75, session.positionSeconds(), 0.001);

            assertTrue(session.requestSeek(2.5));
            session.applyPendingSeek();
            assertEquals(250, readSample(session.audio()));
            assertEquals(2.5, session.positionSeconds(), 0.001);
            assertTrue(session.playing());
            assertFalse(session.paused());
            assertEquals(2, fixture.flushes.get());
        }
        assertTrue(fixture.streams.stream().allMatch(stream -> stream.closed));
    }

    @Test
    public void seekingWakesPausedWorkerWithoutResumingAudio() throws Exception {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            session.start();
            session.pause();
            var completed = new CompletableFuture<Void>();
            var worker = Thread.ofVirtual().start(() -> {
                try {
                    session.awaitIfPaused();
                    session.applyPendingSeek();
                    completed.complete(null);
                } catch (Exception exception) {
                    completed.completeExceptionally(exception);
                }
            });
            session.attachTasks(worker, null);
            assertTrue(session.requestSeek(5));
            completed.get(2, TimeUnit.SECONDS);

            assertTrue(session.paused());
            assertFalse(fixture.running.get());
            assertEquals(5, session.positionSeconds(), 0.001);
            assertEquals(500, readSample(session.audio()));
            session.resume();
            assertFalse(session.paused());
            assertTrue(fixture.running.get());
            assertEquals(5, session.positionSeconds(), 0.001);
        }
    }

    @Test
    public void latestClickWinsAndTargetsStayWithinSongBounds() throws Exception {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            session.start();
            session.requestSeek(8);
            session.requestSeek(3);
            session.applyPendingSeek();
            assertEquals(300, readSample(session.audio()));
            assertEquals(2, fixture.streams.size());

            session.requestSeek(-5);
            session.applyPendingSeek();
            assertEquals(0, readSample(session.audio()));
            assertEquals(0, session.positionSeconds(), 0);

            session.requestSeek(100);
            session.applyPendingSeek();
            assertEquals(10, session.positionSeconds(), 0);
            assertEquals(-1, session.audio().read(new byte[2]));
        }
    }

    @Test
    public void newerSeekCancelsInFlightDecodeAndClosesItsStream() throws Exception {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            session.start();
            fixture.onOpen = () -> session.requestSeek(2);
            session.requestSeek(8);
            session.applyPendingSeek();
            assertTrue(fixture.streams.get(1).closed);
            assertTrue(session.hasPendingSeek());

            fixture.onOpen = null;
            session.applyPendingSeek();
            assertEquals(200, readSample(session.audio()));
            assertTrue(fixture.running.get());
        }
    }

    @Test
    public void stoppedSessionCannotBeRevivedByInFlightSeek() throws Exception {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            session.start();
            fixture.onOpen = session::close;
            session.requestSeek(8);
            session.applyPendingSeek();
            assertFalse(session.playing());
            assertFalse(session.requestSeek(2));
            assertFalse(fixture.running.get());
            assertTrue(fixture.streams.stream().allMatch(stream -> stream.closed));
        }
    }

    @Test
    public void endOfStreamWaitYieldsToSeekInsteadOfCompleting() throws Exception {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            session.start();
            session.pause();
            session.requestSeek(4);
            assertFalse(session.awaitPlaybackEnd());
            assertTrue(session.playing());
            session.applyPendingSeek();
            assertTrue(session.paused());
            session.resume();
            assertTrue(session.awaitPlaybackEnd());
            assertFalse(session.requestSeek(2));
        }
    }

    @Test
    public void pendingSeekDiscardsOldBufferedAudio() throws Exception {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            session.start();
            session.pause();
            session.requestSeek(4);
            session.write(new byte[200], 200);
            assertEquals(0, fixture.frames.get());
            session.applyPendingSeek();
            session.resume();
            session.write(new byte[200], 200);
            assertEquals(5, session.positionSeconds(), 0.001);
        }
    }

    @Test
    public void rejectsInvalidTargetsAndNonReopenableStreams() {
        var fixture = new Fixture();
        try (var session = fixture.session()) {
            assertFalse(session.requestSeek(1));
            session.start();
            assertFalse(session.requestSeek(Double.NaN));
            assertFalse(session.requestSeek(Double.POSITIVE_INFINITY));
        }
        try (var session = new PlaybackSession(fixture.open(), fixture.line,
                fixture.format, new PcmSpectrumAnalyzer(512))) {
            session.start();
            assertFalse(session.requestSeek(1));
        }
    }

    private static int readSample(AudioInputStream audio) throws IOException {
        var sample = new byte[2];
        assertEquals(2, audio.read(sample));
        return (sample[0] & 0xff) | ((sample[1] & 0xff) << 8);
    }

    private static final class Fixture {
        final AudioFormat format = new AudioFormat(100, 16, 1, true, false);
        final AtomicLong frames = new AtomicLong();
        final AtomicBoolean running = new AtomicBoolean();
        final AtomicInteger flushes = new AtomicInteger();
        final List<TrackedAudio> streams = new ArrayList<>();
        Runnable onOpen;
        final SourceDataLine line = (SourceDataLine) Proxy.newProxyInstance(
                SourceDataLine.class.getClassLoader(), new Class<?>[]{SourceDataLine.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getLongFramePosition" -> frames.get();
                    case "getFormat" -> format;
                    case "available", "getBufferSize" -> 4096;
                    case "write" -> {
                        int length = (int) arguments[2];
                        frames.addAndGet(length / format.getFrameSize());
                        yield length;
                    }
                    case "start" -> {
                        running.set(true);
                        yield null;
                    }
                    case "stop", "close" -> {
                        running.set(false);
                        yield null;
                    }
                    case "flush" -> {
                        flushes.incrementAndGet();
                        yield null;
                    }
                    default -> null;
                });

        PlaybackSession session() {
            return new PlaybackSession(open(), line, format, new PcmSpectrumAnalyzer(512), this::open);
        }

        TrackedAudio open() {
            byte[] pcm = new byte[2000];
            for (int frame = 0; frame < 1000; frame++) {
                pcm[frame * 2] = (byte) frame;
                pcm[frame * 2 + 1] = (byte) (frame >> 8);
            }
            var audio = new TrackedAudio(pcm, format);
            streams.add(audio);
            if (onOpen != null) {
                onOpen.run();
            }
            return audio;
        }
    }

    private static final class TrackedAudio extends AudioInputStream {
        boolean closed;

        TrackedAudio(byte[] bytes, AudioFormat format) {
            super(new ByteArrayInputStream(bytes), format, bytes.length / format.getFrameSize());
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
