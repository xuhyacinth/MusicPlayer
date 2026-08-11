package com.xu.music.player.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

    @Test
    public void forwardSkipsUnavailableEntries() {
        assertEquals(2, PlaylistNavigator.findPlayable(0, 4, 1, index -> index == 2).getAsInt());
    }

    @Test
    public void backwardSkipsUnavailableEntries() {
        assertEquals(1, PlaylistNavigator.findPlayable(3, 4, -1, index -> index == 1).getAsInt());
    }

    @Test
    public void noMatchesChecksPredicateExactlySizeTimes() {
        int[] calls = {0};

        assertFalse(PlaylistNavigator.findPlayable(1, 3, 1, index -> {
            calls[0]++;
            return false;
        }).isPresent());
        assertEquals(3, calls[0]);
    }

    @Test
    public void noCurrentForwardStartsAtFirst() {
        assertEquals(0, PlaylistNavigator.findPlayable(null, 4, 1, index -> index == 0).getAsInt());
    }

    @Test
    public void noCurrentBackwardStartsAtLast() {
        assertEquals(3, PlaylistNavigator.findPlayable(null, 4, -1, index -> index == 3).getAsInt());
    }
}
