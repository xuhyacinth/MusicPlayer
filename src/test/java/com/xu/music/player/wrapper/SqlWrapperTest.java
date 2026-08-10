package com.xu.music.player.wrapper;

import com.xu.music.player.entity.SongEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlWrapperTest {

    @Test
    public void insertKeepsApostropheAsParameter() {
        var song = new SongEntity();
        song.setName("Don't Stop");

        var command = new InsertWrapper<>(song, "song").command();

        assertEquals("insert into song(name) values(?)", command.sql());
        assertEquals(List.of("Don't Stop"), command.parameters());
        assertFalse(command.sql().contains("Don't Stop"));
    }

    @Test
    public void queryConditionsUseParameters() {
        var command = new QueryWrapper<>(SongEntity.class, "song")
                .eq("author", "O'Connor")
                .like("name", "Live")
                .command();

        assertTrue(command.sql().contains("author = ?"));
        assertTrue(command.sql().contains("name like ?"));
        assertEquals(List.of("O'Connor", "%Live%"), command.parameters());
    }
}
