package com.xu.music.player.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlaybackProgressTest {

    @Test
    public void calculatesBoundedPercentage() {
        assertEquals(50, PlaybackProgress.percentage(5.0, 10.0));
        assertEquals(0, PlaybackProgress.percentage(1.0, 0.0));
        assertEquals(100, PlaybackProgress.percentage(11.0, 10.0));
        assertEquals(0, PlaybackProgress.percentage(-1.0, 10.0));
    }

    @Test
    public void prefersReportedDurationAndFallsBackToStoredValue() {
        assertEquals(12.5, PlaybackProgress.duration(12.5, 20.0), 0.0);
        assertEquals(20.0, PlaybackProgress.duration(0.0, 20.0), 0.0);
        assertEquals(0.0, PlaybackProgress.duration(0.0, null), 0.0);
    }
}
