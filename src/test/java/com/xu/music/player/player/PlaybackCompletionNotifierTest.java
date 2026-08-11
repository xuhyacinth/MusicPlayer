package com.xu.music.player.player;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class PlaybackCompletionNotifierTest {

    @Test
    public void currentNaturalCompletionCallsListenerOnce() {
        var notifier = new PlaybackCompletionNotifier();
        var firstCalls = new AtomicInteger();
        var secondCalls = new AtomicInteger();
        notifier.setListener(firstCalls::incrementAndGet);
        var completionListener = notifier.snapshot();
        notifier.setListener(secondCalls::incrementAndGet);

        notifier.notifyIfNatural(completionListener, true, true);

        assertEquals(1, firstCalls.get());
        assertEquals(0, secondCalls.get());
    }

    @Test
    public void nonEofDoesNotCallListener() {
        var notifier = new PlaybackCompletionNotifier();
        var calls = new AtomicInteger();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(notifier.snapshot(), false, true);

        assertEquals(0, calls.get());
    }

    @Test
    public void replacedCompletionDoesNotCallListener() {
        var notifier = new PlaybackCompletionNotifier();
        var calls = new AtomicInteger();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(notifier.snapshot(), true, false);

        assertEquals(0, calls.get());
    }
}
