package com.xu.music.player.main;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackRequestGateTest {

    @Test
    public void acceptsCurrentSnapshotWhenNothingIsPlaying() {
        var gate = new PlaybackRequestGate();
        var generation = gate.beginRequest();

        assertEquals(generation, gate.snapshot());
        assertTrue(gate.accepts(generation, false));
    }

    @Test
    public void rejectsSnapshotAfterNewerRequestBegins() {
        var gate = new PlaybackRequestGate();
        var completedGeneration = gate.beginRequest();
        var currentGeneration = gate.beginRequest();

        assertTrue(currentGeneration > completedGeneration);
        assertEquals(currentGeneration, gate.snapshot());
        assertFalse(gate.accepts(completedGeneration, false));
        assertTrue(gate.accepts(currentGeneration, false));
    }

    @Test
    public void rejectsCurrentSnapshotWhileReplacementPlayerIsPlaying() {
        var gate = new PlaybackRequestGate();
        var generation = gate.beginRequest();

        assertFalse(gate.accepts(generation, true));
    }

    @Test
    public void failedNewRequestInvalidatesBoundOldCompletion() {
        var gate = new PlaybackRequestGate();
        var oldGeneration = gate.beginRequest();
        var accepted = new AtomicBoolean(true);
        Runnable oldCompletion = () -> accepted.set(gate.accepts(oldGeneration, false));

        gate.beginRequest();
        oldCompletion.run();

        assertFalse(accepted.get());
    }
}
