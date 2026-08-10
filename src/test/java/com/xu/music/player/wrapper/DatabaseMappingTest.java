package com.xu.music.player.wrapper;

import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.wrapper.sql.NewHelper;
import org.junit.Test;

import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
