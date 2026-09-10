package com.xu.music.player.main;

import java.util.OptionalInt;
import java.util.function.IntPredicate;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackModeTest {
    private OptionalInt advance(Integer current, int size, PlaybackMode mode, IntPredicate playable) {
        return PlaylistNavigator.findPlayable(current, size, 1, mode, true, playable, bound -> 0);
    }

    @Test public void sequentialAdvancesAndSkipsMissingFiles() {
        assertEquals(3, advance(0, 5, PlaybackMode.SEQUENTIAL, i -> i == 3).getAsInt());
        assertEquals(0, advance(null, 5, PlaybackMode.SEQUENTIAL, i -> true).getAsInt());
    }

    @Test public void sequentialStopsAtEndInsteadOfWrapping() {
        assertTrue(advance(3, 4, PlaybackMode.SEQUENTIAL, i -> true).isEmpty());
        assertTrue(advance(1, 4, PlaybackMode.SEQUENTIAL, i -> i == 0).isEmpty());
        assertTrue(advance(0, 1, PlaybackMode.SEQUENTIAL, i -> true).isEmpty());
    }

    @Test public void repeatOneReplaysOnlyCurrentPlayableSong() {
        assertEquals(2, advance(2, 4, PlaybackMode.REPEAT_ONE, i -> true).getAsInt());
        assertTrue(advance(2, 4, PlaybackMode.REPEAT_ONE, i -> i != 2).isEmpty());
        assertTrue(advance(null, 4, PlaybackMode.REPEAT_ONE, i -> true).isEmpty());
    }

    @Test public void manualRepeatOneStillMovesBothDirections() {
        assertEquals(3, PlaylistNavigator.findPlayable(2, 4, 1, PlaybackMode.REPEAT_ONE,
                false, i -> true, bound -> 0).getAsInt());
        assertEquals(1, PlaylistNavigator.findPlayable(2, 4, -1, PlaybackMode.REPEAT_ONE,
                false, i -> true, bound -> 0).getAsInt());
    }

    @Test public void manualSequentialPreservesBoundaryNavigation() {
        assertEquals(3, PlaylistNavigator.findPlayable(0, 4, -1, PlaybackMode.SEQUENTIAL,
                false, i -> true, bound -> 0).getAsInt());
        assertEquals(0, PlaylistNavigator.findPlayable(3, 4, 1, PlaybackMode.SEQUENTIAL,
                false, i -> true, bound -> 0).getAsInt());
    }

    @Test public void randomSelectsEveryEligibleSongButNeverCurrentOrMissing() {
        int[] expected = {0, 3, 4};
        for (int pick = 0; pick < expected.length; pick++) {
            int choice = pick;
            var result = PlaylistNavigator.findPlayable(2, 5, 1, PlaybackMode.RANDOM, true,
                    i -> i != 1, bound -> { assertEquals(3, bound); return choice; });
            assertEquals(expected[pick], result.getAsInt());
        }
    }

    @Test public void randomManualNavigationAlsoAvoidsCurrentSong() {
        for (int direction : new int[]{-1, 1}) {
            assertEquals(1, PlaylistNavigator.findPlayable(0, 2, direction, PlaybackMode.RANDOM,
                    false, i -> true, bound -> 0).getAsInt());
        }
    }

    @Test public void randomCanRepeatWhenOnlyCurrentSongIsPlayable() {
        assertEquals(2, advance(2, 4, PlaybackMode.RANDOM, i -> i == 2).getAsInt());
        assertEquals(0, advance(0, 1, PlaybackMode.RANDOM, i -> true).getAsInt());
        assertEquals(0, advance(null, 1, PlaybackMode.RANDOM, i -> true).getAsInt());
    }

    @Test public void emptyAndUnavailableListsStopForEveryMode() {
        for (var mode : PlaybackMode.values()) {
            assertTrue(advance(null, 0, mode, i -> true).isEmpty());
            assertTrue(advance(1, 3, mode, i -> false).isEmpty());
        }
    }

    @Test public void changingModeAffectsNextCompletion() {
        assertTrue(advance(2, 3, PlaybackMode.SEQUENTIAL, i -> true).isEmpty());
        assertEquals(2, advance(2, 3, PlaybackMode.REPEAT_ONE, i -> true).getAsInt());
        assertEquals(0, advance(2, 3, PlaybackMode.RANDOM, i -> true).getAsInt());
    }
}
