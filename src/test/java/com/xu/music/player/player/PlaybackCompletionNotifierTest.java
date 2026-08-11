package com.xu.music.player.player;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class PlaybackCompletionNotifierTest {

    @Test
    public void currentNaturalCompletionCallsListenerOnce() {
        var notifier = new PlaybackCompletionNotifier();
        var calls = new AtomicInteger();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(true, true);

        assertEquals(1, calls.get());
    }

    @Test
    public void nonEofDoesNotCallListener() {
        var notifier = new PlaybackCompletionNotifier();
        var calls = new AtomicInteger();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(false, true);

        assertEquals(0, calls.get());
    }

    @Test
    public void replacedCompletionDoesNotCallListener() {
        var notifier = new PlaybackCompletionNotifier();
        var calls = new AtomicInteger();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(true, false);

        assertEquals(0, calls.get());
    }
}
