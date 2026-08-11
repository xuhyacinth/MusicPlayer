package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class PlaylistSnapshotTest {

    @Test
    public void removingEarlierRecordRestoresCurrentIdAtNewIndex() {
        SongEntity removed = song("removed");
        SongEntity current = song("current");
        SongEntity later = song("later");

        PlaylistSnapshot beforeRemoval = PlaylistSnapshot.from(List.of(removed, current, later), current.getId());
        PlaylistSnapshot restored = PlaylistSnapshot.from(
                List.of(current, later),
                beforeRemoval.songs().get(beforeRemoval.playingIndex()).getId());

        assertEquals(Integer.valueOf(1), beforeRemoval.playingIndex());
        assertEquals(current, restored.songs().get(0));
        assertEquals(Integer.valueOf(0), restored.playingIndex());
    }

    @Test
    public void missingCurrentReturnsNull() {
        PlaylistSnapshot snapshot = PlaylistSnapshot.from(List.of(song("first")), "missing");

        assertNull(snapshot.playingIndex());
    }

    @Test
    public void nullCurrentDoesNotSelectNullIdSong() {
        SongEntity songWithoutId = song(null);

        PlaylistSnapshot snapshot = PlaylistSnapshot.from(List.of(songWithoutId), null);

        assertNull(snapshot.playingIndex());
    }

    @Test
    public void songsMapIsUnmodifiable() {
        PlaylistSnapshot snapshot = PlaylistSnapshot.from(List.of(song("first")), null);

        assertThrows(UnsupportedOperationException.class, () -> snapshot.songs().put(1, song("second")));
    }

    private SongEntity song(String id) {
        SongEntity song = new SongEntity();
        song.setId(id);
        return song;
    }
}
