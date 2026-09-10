package com.xu.music.player.taskbar;

import com.xu.music.player.entity.SongEntity;
import java.awt.Rectangle;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskbarLyricsTest {
    @Test public void currentLyricWinsAndMissingLyricsUseSongWithoutDuplicateAuthor() {
        var song = new SongEntity();
        song.setName("双宿双栖"); song.setAuthor("梦涵");
        assertEquals("双宿双栖 - 梦涵", TaskbarLyrics.displayText(song, null));
        assertEquals("当前 歌词", TaskbarLyrics.displayText(song, " 当前\n歌词 "));
        song.setName("梦涵 - 双宿双栖");
        assertEquals("梦涵 - 双宿双栖", TaskbarLyrics.displayText(song, " "));
        assertEquals("", TaskbarLyrics.displayText(null, "旧歌词"));
    }

    @Test public void placementConvertsScaleAndReservesTray() {
        for (double scale : new double[]{1, 1.25, 1.5, 2}) {
            var bar = new Rectangle(0, (int) (1000 * scale), (int) (1600 * scale), (int) (48 * scale));
            assertEquals(new Rectangle(180, 1000, 320, 48), TaskbarLyrics.placement(bar, scale, 180, 320));
            var clamped = TaskbarLyrics.placement(bar, scale, 9000, 9000);
            assertEquals(1360, clamped.x + clamped.width);
            assertEquals(800, clamped.width);
        }
        assertNull(TaskbarLyrics.placement(new Rectangle(0, 0, 300, 48), 1, 0, 320));
        assertEquals(new Rectangle(-1600, 1000, 120, 48),
                TaskbarLyrics.placement(new Rectangle(-1600, 1000, 1600, 48), 1, -20, 10));
    }

    @Test public void styleDoesNotActivateAndLockOnlyChangesMouseTransparency() {
        int locked = WindowsTaskbar.windowStyle(0x00040000, true);
        int unlocked = WindowsTaskbar.windowStyle(locked, false);
        assertEquals(0, locked & 0x00040000);
        assertEquals(0x08080080, unlocked);
        assertEquals(0x20, locked ^ unlocked);
        assertEquals(locked, WindowsTaskbar.windowStyle(locked, true));
    }
}
