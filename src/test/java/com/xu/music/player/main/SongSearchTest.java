package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SongSearchTest {

    @Test
    public void blankKeywordReturnsAllSongs() {
        SongEntity first = song("Beyond - 长城", "Beyond");
        SongEntity second = song("酷爱", "张敬轩");

        List<SongEntity> result = SongSearch.filter(List.of(first, second), "   ");

        assertEquals(List.of(first, second), result);
    }

    @Test
    public void keywordMatchesSongName() {
        SongEntity match = song("酷爱", "张敬轩");
        SongEntity other = song("加减乘除", "梦涵");

        List<SongEntity> result = SongSearch.filter(List.of(match, other), "酷");

        assertEquals(List.of(match), result);
    }

    @Test
    public void keywordMatchesAuthor() {
        SongEntity match = song("长城", "Beyond");
        SongEntity other = song("酷爱", "张敬轩");

        List<SongEntity> result = SongSearch.filter(List.of(match, other), "beyond");

        assertEquals(List.of(match), result);
    }

    @Test
    public void keywordMatchingIgnoresCase() {
        SongEntity match = song("Dream Song", "Singer");
        SongEntity other = song("Night Track", "Artist");

        List<SongEntity> result = SongSearch.filter(List.of(match, other), "dream");

        assertEquals(List.of(match), result);
    }

    @Test
    public void nullFieldsDoNotFailMatching() {
        SongEntity empty = song(null, null);
        SongEntity match = song(null, "张敬轩");

        List<SongEntity> result = SongSearch.filter(List.of(empty, match), "敬轩");

        assertEquals(List.of(match), result);
    }

    private SongEntity song(String name, String author) {
        SongEntity song = new SongEntity();
        song.setName(name);
        song.setAuthor(author);
        return song;
    }
}
