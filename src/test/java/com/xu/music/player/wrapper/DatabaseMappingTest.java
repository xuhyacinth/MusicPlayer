package com.xu.music.player.wrapper;

import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.wrapper.sql.NewHelper;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseMappingTest {

    @Test
    public void sqliteTextTimestampMapsToDate() throws Exception {
        var path = Files.createTempFile("music-player-", ".db");
        try {
            var helper = new NewHelper(path);
            helper.update("create table song (id text, create_time text)");
            helper.update("insert into song(id, create_time) values(?, ?)",
                    "1", "2024-06-06 20:47:02");

            var songs = helper.select("select * from song", SongEntity.class);

            assertEquals(1, songs.size());
            assertNotNull(songs.getFirst().getCreateTime());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void deletingSongRecordDoesNotDeleteAudioFile() throws Exception {
        var database = Files.createTempFile("music-player-", ".db");
        var audio = Files.createTempFile("music-player-", ".wav");
        try {
            var helper = new NewHelper(database);
            helper.update("create table song (id text primary key, song_path text)");
            helper.update("insert into song(id, song_path) values(?, ?)", "missing", audio.toString());
            helper.update("insert into song(id, song_path) values(?, ?)", "keep", "keep.wav");

            var song = new SongEntity();
            song.setId("missing");

            var deleted = new UpdateWrapper<>(song, "song", helper)
                    .eq("id", song.getId())
                    .delete();

            assertEquals(1, deleted);
            assertEquals(1, helper.select("select * from song where id = ?", SongEntity.class, "keep").size());
            assertTrue(Files.isRegularFile(audio));
        } finally {
            Files.deleteIfExists(database);
            Files.deleteIfExists(audio);
        }
    }

    @Test
    public void bundledPlaylistReferencesExistingFiles() {
        var songs = List.of("1", "2", "3", "4").stream()
                .flatMap(id -> new QueryWrapper<>(SongEntity.class, "song")
                        .eq("id", id)
                        .list()
                        .stream())
                .toList();

        assertEquals(4, songs.size());
        for (var song : songs) {
            assertTrue(song.getSongPath(), Files.isRegularFile(Path.of(song.getSongPath())));
            if (song.getLyricPath() != null && !song.getLyricPath().isBlank()) {
                assertTrue(song.getLyricPath(), Files.isRegularFile(Path.of(song.getLyricPath())));
            }
        }
    }
}
