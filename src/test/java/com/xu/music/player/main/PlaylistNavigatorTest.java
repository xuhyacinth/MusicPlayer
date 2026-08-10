package com.xu.music.player.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlaylistNavigatorTest {

    @Test
    public void previousWrapsFromFirstToLast() {
        assertEquals(3, PlaylistNavigator.move(0, 4, -1));
    }

    @Test
    public void nextWrapsFromLastToFirst() {
        assertEquals(0, PlaylistNavigator.move(3, 4, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPlaylistIsRejected() {
        PlaylistNavigator.move(0, 0, 1);
    }
}
