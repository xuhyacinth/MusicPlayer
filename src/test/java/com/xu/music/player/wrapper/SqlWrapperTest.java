package com.xu.music.player.wrapper;

import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.wrapper.sql.Helper;
import org.junit.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class SqlWrapperTest {

    @Test
    public void updateWrapperRoutesOperationsToMatchingHelperMethods() {
        var song = new SongEntity();
        song.setId("1");
        var helper = new RecordingHelper();

        new UpdateWrapper<>(song, "song", helper).insert();
        new UpdateWrapper<>(song, "song", helper).update();
        new UpdateWrapper<>(song, "song", helper).delete();

        assertEquals(List.of("insert", "update", "delete"), helper.operations);
    }

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

    @Test
    public void rejectsUnsafeTableName() {
        assertThrows(DataBaseError.class,
                () -> new QueryWrapper<>(SongEntity.class, "song; drop table song").command());
    }

    @Test
    public void rejectsUnsafeFieldName() {
        assertThrows(DataBaseError.class,
                () -> new QueryWrapper<>(SongEntity.class, "song")
                        .eq("author) or 1 = 1 --", "value"));
    }

    private static class RecordingHelper implements Helper {

        private final List<String> operations = new ArrayList<>();

        @Override
        public Connection getConn() {
            return null;
        }

        @Override
        public int insert(String sql, Object... params) {
            operations.add("insert");
            return 1;
        }

        @Override
        public int update(String sql, Object... params) {
            operations.add("update");
            return 1;
        }

        @Override
        public int delete(String sql, Object... params) {
            operations.add("delete");
            return 1;
        }

        @Override
        public <T> List<T> select(String sql, Class<T> cls, Object... params) {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> select(String sql, Object... params) {
            return List.of();
        }
    }
}
