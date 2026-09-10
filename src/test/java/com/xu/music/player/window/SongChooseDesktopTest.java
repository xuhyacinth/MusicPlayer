package com.xu.music.player.window;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** 验证真实文件对话框的筛选配置，不打开选择窗口或访问歌曲数据库。 */
public class SongChooseDesktopTest {
    @Test public void defaultsToAllAudioFormatsAndKeepsNamedIndividualFilters() {
        assumeTrue(Boolean.getBoolean("musicplayer.desktopTests"));
        Display display = new Display();
        Shell shell = new Shell(display);
        try {
            var dialog = SongChoose.createDialog(shell);
            String[] extensions = dialog.getFilterExtensions();
            assertEquals(0, dialog.getFilterIndex());
            assertEquals(Set.of("*.mp3", "*.MP3", "*.wav", "*.WAV", "*.flac", "*.FLAC", "*.pcm", "*.PCM"),
                    Arrays.stream(extensions[0].split(";")).collect(Collectors.toSet()));
            assertArrayEquals(new String[]{"*.mp3;*.MP3", "*.wav;*.WAV", "*.flac;*.FLAC", "*.pcm;*.PCM"},
                    Arrays.copyOfRange(extensions, 1, extensions.length));
            assertArrayEquals(new String[]{"音频文件（MP3、WAV、FLAC、PCM）",
                    "MP3 音频", "WAV 音频", "FLAC 音频", "PCM 音频"}, dialog.getFilterNames());
            assertEquals(dialog.getFilterNames().length, extensions.length);
            assertEquals(SWT.OPEN | SWT.MULTI, dialog.getStyle() & (SWT.OPEN | SWT.MULTI));
        } finally {
            shell.dispose();
            display.dispose();
        }
    }
}
