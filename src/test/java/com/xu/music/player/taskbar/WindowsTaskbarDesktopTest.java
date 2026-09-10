package com.xu.music.player.taskbar;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import java.awt.EventQueue;
import java.awt.Window;
import javax.swing.JWindow;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class WindowsTaskbarDesktopTest {
    @Test public void nativeFlagsPreserveForegroundAndLockCanToggle() throws Exception {
        assumeTrue(Boolean.getBoolean("musicplayer.desktopTests") && TaskbarLyrics.supported());
        EventQueue.invokeAndWait(() -> {
            var user32 = User32.INSTANCE;
            var foreground = user32.GetForegroundWindow();
            var window = new JWindow();
            try {
                window.setType(Window.Type.UTILITY);
                window.setFocusableWindowState(false);
                window.setAutoRequestFocus(false);
                window.setBackground(new java.awt.Color(0, 0, 0, 0));
                window.setBounds(10, 10, 120, 32);
                window.addNotify();
                var bridge = new WindowsTaskbar();
                bridge.configure(window, true);
                window.setVisible(true);
                var handle = new HWND(Native.getWindowPointer(window));
                int locked = user32.GetWindowLong(handle, WinUser.GWL_EXSTYLE);
                assertEquals(0x080800A0, locked & 0x080C00A0);
                assertEquals(foreground, user32.GetForegroundWindow());
                bridge.configure(window, false);
                int unlocked = user32.GetWindowLong(handle, WinUser.GWL_EXSTYLE);
                assertEquals(0, unlocked & WinUser.WS_EX_TRANSPARENT);
                assertEquals(foreground, user32.GetForegroundWindow());
                assertNotNull(user32.FindWindow("Shell_TrayWnd", null));
            } finally { window.dispose(); }
        });
    }
}
