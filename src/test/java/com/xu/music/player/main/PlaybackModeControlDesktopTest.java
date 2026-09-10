package com.xu.music.player.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** 不启动主窗口或访问数据库，验证图标按钮的循环切换及误操作边界。 */
public class PlaybackModeControlDesktopTest {
    @Test public void iconCyclesOnLeftClickWithoutMenu() {
        assumeTrue(Boolean.getBoolean("musicplayer.desktopTests"));
        Display display = new Display();
        Shell shell = new Shell(display);
        try {
            var control = new PlaybackModeControl(shell);
            control.setBounds(28, 18, 32, 32);
            var button = (Canvas) shell.getChildren()[0];
            assertEquals(PlaybackMode.SEQUENTIAL, control.mode());
            assertNull(button.getMenu());
            assertEquals(32, button.getSize().x);
            assertEquals(32, button.getSize().y);
            assertTrue(button.getBounds().x + button.getBounds().width < 84);
            for (var mode : new PlaybackMode[]{PlaybackMode.RANDOM, PlaybackMode.REPEAT_ONE,
                    PlaybackMode.SEQUENTIAL, PlaybackMode.RANDOM}) {
                click(button, 1, 16, 16);
                assertEquals(mode, control.mode());
                assertTrue(button.getToolTipText().contains(mode.label()));
            }
            click(button, 3, 16, 16);
            assertEquals(PlaybackMode.RANDOM, control.mode());
            click(button, 1, 40, 40);
            assertEquals(PlaybackMode.RANDOM, control.mode());
            var event = mouse(1, 16, 16);
            button.notifyListeners(SWT.MouseDown, event);
            button.notifyListeners(SWT.MouseExit, new Event());
            button.notifyListeners(SWT.MouseUp, event);
            assertEquals(PlaybackMode.RANDOM, control.mode());
            var key = new Event();
            key.character = ' ';
            button.notifyListeners(SWT.KeyDown, key);
            assertEquals(PlaybackMode.REPEAT_ONE, control.mode());
            key = new Event();
            key.keyCode = SWT.CR;
            button.notifyListeners(SWT.KeyDown, key);
            assertEquals(PlaybackMode.SEQUENTIAL, control.mode());
        } finally {
            shell.dispose();
            display.dispose();
        }
    }

    private void click(Canvas button, int mouseButton, int x, int y) {
        button.notifyListeners(SWT.MouseDown, mouse(mouseButton, 16, 16));
        button.notifyListeners(SWT.MouseUp, mouse(mouseButton, x, y));
    }

    private Event mouse(int mouseButton, int x, int y) {
        var event = new Event();
        event.button = mouseButton;
        event.x = x;
        event.y = y;
        return event;
    }
}
