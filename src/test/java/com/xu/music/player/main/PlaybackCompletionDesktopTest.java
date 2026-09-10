package com.xu.music.player.main;

import com.xu.music.player.constant.Constant;
import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.lyric.CenteredLyrics;
import com.xu.music.player.player.Player;
import com.xu.music.player.player.SdlFftPlayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** 显式启用桌面和音频测试后运行；静音播放人工样本，不打开主窗口或访问用户数据库。 */
public class PlaybackCompletionDesktopTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void flacTailDoesNotBlockAutomaticManualOrSelectedPlayback() throws Exception {
        assumeTrue(Boolean.getBoolean("musicplayer.desktopTests") && Boolean.getBoolean("musicplayer.audioTests"));
        Display display = new Display();
        Shell shell = new Shell(display);
        var savedList = new LinkedHashMap<>(Constant.PLAYING_LIST);
        var savedIndex = Constant.PLAYING_INDEX;
        var savedSong = Constant.PLAYING_SONG;
        var savedLength = Constant.PLAYING_SONG_LENGTH;
        var savedPlaying = Constant.MUSIC_PLAYER_PLAYING_STATE;
        var savedLyric = Constant.PLAYING_LYRIC;
        var app = new MusicPlayer();
        try {
            set(app, "display", display);
            set(app, "shell", shell);
            set(app, "foot", shell);
            set(app, "player", SdlFftPlayer.create());
            set(app, "volumePercentage", 0);
            for (var name : new String[]{"start", "timeLabel1", "timeLabel2", "audioFormatLabel"}) {
                set(app, name, new Label(shell, SWT.NONE));
            }
            set(app, "progress", new Canvas(shell, SWT.NONE));
            set(app, "lyrics", new CenteredLyrics(shell));
            set(app, "playbackMode", new PlaybackModeControl(shell));
            var table = new Table(shell, SWT.NONE);
            set(app, "lists", table);
            Constant.PLAYING_LIST.clear();
            Constant.PLAYING_INDEX = null;
            for (int index = 0; index < 2; index++) {
                byte[] original;
                try (var input = getClass().getResourceAsStream("/audio/stereo-48000-16bit.flac")) {
                    assertNotNull(input);
                    original = input.readAllBytes();
                }
                byte[] tagged = Arrays.copyOf(original, original.length + 1);
                tagged[original.length] = (byte) 0xff;
                var file = temporary.newFile("tail-" + index + ".flac");
                Files.write(file.toPath(), tagged);
                var song = new SongEntity();
                song.setId("test-" + index);
                song.setName("test-" + index);
                song.setSongPath(file.getAbsolutePath());
                song.setLength(0.1);
                Constant.PLAYING_LIST.put(index, song);
                new TableItem(table, SWT.NONE);
            }

            select(app, 0);
            awaitFinished(display, app, null, 1);
            Player previous = (Player) get(app, "player");

            // 列表双击调用的同一入口：播放结束后再次选择最后一首，必须能重新加载并结束。
            select(app, 1);
            awaitFinished(display, app, previous, 1);
            previous = (Player) get(app, "player");

            // 手动下一曲从末尾回到首曲，再由自然完成回调顺序切到第二首。
            var next = MusicPlayer.class.getDeclaredMethod("playRelative", int.class, boolean.class);
            next.setAccessible(true);
            next.invoke(app, 1, false);
            assertEquals(Integer.valueOf(0), Constant.PLAYING_INDEX);
            awaitFinished(display, app, previous, 1);
        } finally {
            set(app, "closing", true);
            ((AsyncPlaybackLoader) get(app, "playbackLoader")).close();
            ((Player) get(app, "player")).close();
            shell.dispose();
            display.dispose();
            Constant.PLAYING_LIST.clear();
            Constant.PLAYING_LIST.putAll(savedList);
            Constant.PLAYING_INDEX = savedIndex;
            Constant.PLAYING_SONG = savedSong;
            Constant.PLAYING_SONG_LENGTH = savedLength;
            Constant.MUSIC_PLAYER_PLAYING_STATE = savedPlaying;
            Constant.PLAYING_LYRIC = savedLyric;
        }
    }

    private static void awaitFinished(Display display, MusicPlayer app, Player previous, int index) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (!display.readAndDispatch()) Thread.sleep(2);
            var player = (Player) get(app, "player");
            if (Integer.valueOf(index).equals(Constant.PLAYING_INDEX)
                    && player != previous && !player.audioFormatDescription().isEmpty()
                    && !player.playing() && !Constant.MUSIC_PLAYER_PLAYING_STATE) return;
        }
        fail("FLAC 结束后应完成切歌并自然停止，当前索引=" + Constant.PLAYING_INDEX);
    }

    private static void select(MusicPlayer app, int index) throws Exception {
        var method = MusicPlayer.class.getDeclaredMethod("playSelected", int.class);
        method.setAccessible(true);
        method.invoke(app, index);
    }

    private static void set(MusicPlayer app, String name, Object value) throws Exception {
        var field = MusicPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(app, value);
    }

    private static Object get(MusicPlayer app, String name) throws Exception {
        var field = MusicPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(app);
    }
}
