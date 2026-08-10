package com.xu.music.player.player;

import org.junit.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PlaybackSessionTest {

    @Test
    public void oldCompletionCannotDetachReplacementSession() {
        var slot = new PlaybackSessionSlot();
        var oldSession = sessionWithLine(0, new AtomicInteger(), new AtomicBoolean());
        var replacement = sessionWithLine(0, new AtomicInteger(), new AtomicBoolean());
        slot.replace(oldSession);
        slot.replace(replacement);

        assertFalse(slot.complete(oldSession));
        assertSame(replacement, slot.current());
    }

    @Test
    public void positionUsesFrameRateAndCloseIsIdempotent() {
        var closeCount = new AtomicInteger();
        var session = sessionWithLine(44_100, closeCount, new AtomicBoolean());

        assertEquals(1.0, session.positionSeconds(), 0.001);
        session.close();
        session.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void pauseStopsTheAudioLine() {
        var running = new AtomicBoolean(true);
        var session = sessionWithLine(0, new AtomicInteger(), running);
        session.start();

        session.pause();

        assertFalse(running.get());
        assertTrue(session.paused());
    }

    private static PlaybackSession sessionWithLine(
            long frames, AtomicInteger closeCount, AtomicBoolean running) {
        var format = new AudioFormat(44_100, 16, 2, true, false);
        var line = (SourceDataLine) Proxy.newProxyInstance(
                SourceDataLine.class.getClassLoader(),
                new Class<?>[]{SourceDataLine.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getLongFramePosition" -> frames;
                    case "getFormat" -> format;
                    case "start" -> {
                        running.set(true);
                        yield null;
                    }
                    case "stop" -> {
                        running.set(false);
                        yield null;
                    }
                    case "close" -> {
                        closeCount.incrementAndGet();
                        yield null;
                    }
                    default -> primitiveDefault(method.getReturnType());
                });
        var audio = new AudioInputStream(InputStream.nullInputStream(), format, 0);
        return new PlaybackSession(audio, line, format, new PcmSpectrumAnalyzer(512));
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        return 0D;
    }
}
