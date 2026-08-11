package com.xu.music.player.main;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackRequestGateTest {

    @Test
    public void acceptsCurrentSnapshotWhenNothingIsPlaying() {
        var gate = new PlaybackRequestGate();
        gate.beginRequest();
        var generation = gate.snapshot();

        assertTrue(gate.accepts(generation, false));
    }

    @Test
    public void rejectsSnapshotAfterNewerRequestBegins() {
        var gate = new PlaybackRequestGate();
        var completedGeneration = gate.beginRequest();
        gate.beginRequest();

        assertFalse(gate.accepts(completedGeneration, false));
    }

    @Test
    public void rejectsCurrentSnapshotWhileReplacementPlayerIsPlaying() {
        var gate = new PlaybackRequestGate();
        var generation = gate.beginRequest();

        assertFalse(gate.accepts(generation, true));
    }
}
