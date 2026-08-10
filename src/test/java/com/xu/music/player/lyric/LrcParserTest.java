package com.xu.music.player.lyric;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LrcParserTest {

    @Test
    public void parsesAndSortsTimedLines() {
        var lines = LrcParser.parse(List.of("[00:02.50]后", "[00:01.00]前", "[ar:作者]"));

        assertEquals(List.of(
                new LrcLine(1.0, "[00:01.00]", "前"),
                new LrcLine(2.5, "[00:02.50]", "后")), lines);
    }

    @Test
    public void ignoresMalformedTime() {
        assertTrue(LrcParser.parse(List.of("[bad]歌词", "纯文本")).isEmpty());
    }
}
