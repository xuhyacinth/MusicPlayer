package com.xu.music.player.taskbar;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.Set;

/** 只查询任务栏并配置自己的窗口，不修改或注入 Explorer。 */
final class WindowsTaskbar {
    private final User32 user32 = User32.INSTANCE;

    Rectangle visibleBounds() {
        HWND taskbar = user32.FindWindow("Shell_TrayWnd", null);
        if (taskbar == null || !user32.IsWindowVisible(taskbar)) return null;
        RECT rect = new RECT();
        if (!user32.GetWindowRect(taskbar, rect)) return null;
        var monitor = new WinUser.MONITORINFO();
        if (!user32.GetMonitorInfo(user32.MonitorFromWindow(taskbar,
                WinUser.MONITOR_DEFAULTTOPRIMARY), monitor).booleanValue()) return null;
        Rectangle screen = rectangle(monitor.rcMonitor);
        Rectangle bar = rectangle(rect).intersection(screen);
        if (bar.height < 16 || bar.width <= bar.height || isFullscreen(screen)) return null;
        return bar;
    }

    private boolean isFullscreen(Rectangle screen) {
        HWND foreground = user32.GetForegroundWindow();
        if (foreground == null) return false;
        char[] name = new char[256];
        user32.GetClassName(foreground, name, name.length);
        if (Set.of("Progman", "WorkerW", "Shell_TrayWnd").contains(Native.toString(name))) return false;
        RECT rect = new RECT();
        return user32.GetWindowRect(foreground, rect) && rectangle(rect).contains(screen);
    }

    void configure(Window window, boolean locked) {
        HWND handle = new HWND(Native.getWindowPointer(window));
        int previousStyle = user32.GetWindowLong(handle, WinUser.GWL_EXSTYLE);
        int style = windowStyle(previousStyle, locked);
        int flags = WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE;
        if (style != previousStyle) {
            Native.setLastError(0);
            int previous = user32.SetWindowLong(handle, WinUser.GWL_EXSTYLE, style);
            if (previous == 0 && Native.getLastError() != 0)
                throw new IllegalStateException("设置歌词窗口属性失败：" + Native.getLastError());
            flags |= WinUser.SWP_FRAMECHANGED;
        }
        // HWND_TOPMOST 是指针宽度的 -1，不能先构造成无符号 32 位数。
        if (!user32.SetWindowPos(handle, new HWND(Pointer.createConstant(-1L)), 0, 0, 0, 0, flags))
            throw new IllegalStateException("设置歌词窗口层级失败：" + Native.getLastError());
    }

    static int windowStyle(int previous, boolean locked) {
        int style = (previous | 0x08000000 | 0x00000080 | 0x00080000) & ~0x00040000;
        return locked ? style | WinUser.WS_EX_TRANSPARENT : style & ~WinUser.WS_EX_TRANSPARENT;
    }

    private static Rectangle rectangle(RECT rect) {
        return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }
}
