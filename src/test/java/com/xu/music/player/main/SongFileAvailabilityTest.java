package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SongFileAvailabilityTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void regularFileIsPlayable() throws Exception {
        File file = temporaryFolder.newFile("song.mp3");

        assertTrue(SongFileAvailability.isPlayable(songWithPath(file.getPath())));
    }

    @Test
    public void unavailablePathsAreNotPlayable() {
        assertFalse(SongFileAvailability.isPlayable(songWithPath("missing.mp3")));
        assertFalse(SongFileAvailability.isPlayable(songWithPath("  ")));
        assertFalse(SongFileAvailability.isPlayable(songWithPath("invalid\0path")));
        assertFalse(SongFileAvailability.isPlayable(null));
    }

    private SongEntity songWithPath(String path) {
        SongEntity song = new SongEntity();
        song.setSongPath(path);
        return song;
    }
}
