package com.xu.music.player.lyric;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class CenteredLyricsTest {
    @Test public void timelineHandlesBeforeFirstBoundaryDuplicateAndBackwardSeek() {
        var lines = List.of(new LrcLine(1, "", "一"), new LrcLine(2, "", "二"),
                new LrcLine(2, "", "翻译"), new LrcLine(4, "", "尾句"));
        assertEquals(-1, CenteredLyrics.indexAt(lines, 0));
        assertEquals(0, CenteredLyrics.indexAt(lines, 1));
        assertEquals(0, CenteredLyrics.indexAt(lines, 1.999));
        assertEquals(2, CenteredLyrics.indexAt(lines, 2));
        assertEquals(3, CenteredLyrics.indexAt(lines, 100));
        assertEquals(0, CenteredLyrics.indexAt(lines, 1.1));
        assertEquals(-1, CenteredLyrics.indexAt(List.of(), 1));
    }

    @Test public void paddingCentersWithinHalfARowAcrossViewportSizes() {
        for (int height = 18; height < 800; height++) {
            int padding = CenteredLyrics.padding(height, 18);
            assertTrue(Math.abs(padding * 18 + 9 - height / 2.0) <= 9);
        }
        assertEquals(0, CenteredLyrics.padding(0, 18));
    }
}
