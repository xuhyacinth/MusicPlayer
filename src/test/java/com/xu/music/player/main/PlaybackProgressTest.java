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
}
