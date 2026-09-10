package com.xu.music.player.lyric;

import java.util.stream.IntStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** 显式开启的原生桌面测试，不读取或修改歌曲数据库。 */
public class CenteredLyricsDesktopTest {
    @Test public void firstLastResizeAndSameLineRemainStable() {
        assumeTrue(Boolean.getBoolean("musicplayer.desktopTests"));
        Display display = new Display();
        Shell shell = new Shell(display, SWT.TOOL | SWT.NO_FOCUS);
        try {
            shell.setText("SWT 歌词居中验证");
            shell.setLayout(new FillLayout());
            shell.setBounds(80, 80, 720, 420);
            var lyrics = new CenteredLyrics(shell);
            lyrics.setLines(IntStream.range(0, 40).mapToObj(i -> new LrcLine(i, "", "第 " + i + " 句歌词")).toList());
            shell.open(); shell.layout();
            while (display.readAndDispatch()) {}
            Table table = (Table) shell.getChildren()[0];
            for (int height : new int[]{420, 280, 565}) {
                shell.setSize(720, height); shell.layout();
                while (display.readAndDispatch()) {}
                for (int index : new int[]{0, 20, 39, 1}) {
                    assertEquals("第 " + index + " 句歌词", lyrics.update(index));
                    int padding = CenteredLyrics.padding(table.getClientArea().height, table.getItemHeight());
                    var active = table.getItem(padding + index);
                    var bounds = active.getBounds(1);
                    assertTrue("歌词未居中: " + bounds + ", 区域: " + table.getClientArea(),
                            Math.abs(bounds.y + bounds.height / 2.0 - table.getClientArea().height / 2.0)
                                    <= table.getItemHeight() / 2.0 + 2);
                    assertEquals(display.getSystemColor(SWT.COLOR_BLUE), active.getForeground());
                    int other = index == 0 ? 1 : 0;
                    assertEquals(table.getForeground(), table.getItem(padding + other).getForeground());
                }
            }
            lyrics.update(20);
            table.setTopIndex(0);
            for (int tick = 0; tick < 10; tick++) lyrics.update(20 + tick / 10.0);
            assertEquals("同一句不能反复重置滚动位置", 0, table.getTopIndex());
            lyrics.update(21);
            assertEquals(21, table.getTopIndex());
            lyrics.setLines(java.util.List.of());
            assertEquals(0, table.getItemCount());
            assertNull(lyrics.update(100));
        } finally { shell.dispose(); display.dispose(); }
    }
}
