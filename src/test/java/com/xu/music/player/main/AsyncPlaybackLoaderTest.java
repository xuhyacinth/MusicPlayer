package com.xu.music.player.main;

import com.xu.music.player.player.Player;
import com.xu.music.player.player.SdlFftPlayer;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AsyncPlaybackLoaderTest {
    @Test public void blockedLoadDoesNotBlockCallerAndLatestRequestWins() throws Exception {
        var entered = new CountDownLatch(1);
        var unblock = new CountDownLatch(1);
        var ready = new CountDownLatch(1);
        var generation = new AtomicInteger(1);
        var created = new AtomicInteger();
        var staleClosed = new AtomicInteger();
        var stalePlayed = new AtomicInteger();
        var loadedThread = new AtomicReference<Thread>();
        var failure = new AtomicReference<Exception>();
        Thread caller = Thread.currentThread();
        try (var loader = new AsyncPlaybackLoader(() -> {
            int id = created.incrementAndGet();
            return player(method -> {
                if (id == 1 && method.equals("load")) {
                    loadedThread.set(Thread.currentThread());
                    entered.countDown();
                    if (!unblock.await(5, TimeUnit.SECONDS)) throw new IOException("等待超时");
                }
                if (id == 1 && method.equals("close")) staleClosed.incrementAndGet();
                if (id == 1 && method.equals("play")) stalePlayed.incrementAndGet();
            });
        })) {
            loader.load("first", () -> generation.get() == 1, value -> {},
                    value -> fail("旧请求不能发布"), failure::set);
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertNotSame(caller, loadedThread.get());
                generation.incrementAndGet();
                loader.load("second", () -> generation.get() == 2, value -> {},
                        value -> ready.countDown(), failure::set);
            } finally { unblock.countDown(); }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            assertNull(failure.get());
            assertEquals(1, staleClosed.get());
            assertEquals(0, stalePlayed.get());
            assertEquals(2, created.get());
        }
    }

    @Test public void failureReleasesCandidateAndReportsCause() throws Exception {
        var closed = new AtomicInteger();
        var reported = new AtomicReference<Exception>();
        var done = new CountDownLatch(1);
        try (var loader = new AsyncPlaybackLoader(() -> player(method -> {
            if (method.equals("load")) throw new IOException("音频损坏");
            if (method.equals("close")) closed.incrementAndGet();
        }))) {
            loader.load("broken", () -> true, value -> {}, value -> fail(), error -> {
                reported.set(error); done.countDown();
            });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals("音频损坏", reported.get().getMessage());
            assertEquals(1, closed.get());
        }
    }

    @Test public void invalidatedDuringPreparationNeverStartsAudio() throws Exception {
        var current = new AtomicBoolean(true);
        var closed = new CountDownLatch(1);
        var played = new AtomicInteger();
        try (var loader = new AsyncPlaybackLoader(() -> player(method -> {
            if (method.equals("play")) played.incrementAndGet();
            if (method.equals("close")) closed.countDown();
        }))) {
            loader.load("song", current::get, value -> current.set(false), value -> fail(), error -> fail());
            assertTrue(closed.await(5, TimeUnit.SECONDS));
            assertEquals(0, played.get());
        }
    }

    @Test public void previousDeviceIsClosedBeforeOpeningNextSong() throws Exception {
        var created = new AtomicInteger();
        var firstReady = new CountDownLatch(1);
        var secondReady = new CountDownLatch(1);
        var firstClosed = new AtomicBoolean();
        var closeObserved = new AtomicBoolean();
        try (var loader = new AsyncPlaybackLoader(() -> {
            int id = created.incrementAndGet();
            return player(method -> {
                if (id == 1 && method.equals("close")) firstClosed.set(true);
                if (id == 2 && method.equals("load")) closeObserved.set(firstClosed.get());
            });
        })) {
            loader.load("one", () -> true, value -> {}, value -> firstReady.countDown(), error -> fail());
            assertTrue(firstReady.await(5, TimeUnit.SECONDS));
            loader.load("two", () -> true, value -> {}, value -> secondReady.countDown(), error -> fail());
            assertTrue(secondReady.await(5, TimeUnit.SECONDS));
            assertTrue("旧音频设备须先释放", closeObserved.get());
        }
    }

    @Test public void playersHaveIndependentLoadingLocks() {
        assertNotSame(SdlFftPlayer.create(), SdlFftPlayer.create());
    }

    private static Player player(Invocation invocation) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> { invocation.call(method.getName()); return null; });
    }

    private interface Invocation { void call(String method) throws Exception; }
}
