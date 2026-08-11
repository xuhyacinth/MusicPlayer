package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

final class SongFileAvailability {

    private SongFileAvailability() {
    }

    static boolean isPlayable(SongEntity song) {
        if (song == null || song.getSongPath() == null || song.getSongPath().isBlank()) {
            return false;
        }
        try {
            return Files.isRegularFile(Path.of(song.getSongPath()));
        } catch (InvalidPathException exception) {
            return false;
        }
    }
}
