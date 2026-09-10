package com.xu.music.player.taskbar

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser
import java.awt.Rectangle
import java.awt.Window

/** 仅查询任务栏和设置自己的窗口，不向 Explorer 注入或注册工具栏。 */
internal class WindowsTaskbar {
    private val user32 = User32.INSTANCE

    fun visibleBounds(): Rectangle? {
        val taskbar = user32.FindWindow("Shell_TrayWnd", null) ?: return null
        if (!user32.IsWindowVisible(taskbar)) return null
        val rect = RECT()
        if (!user32.GetWindowRect(taskbar, rect)) return null
        val monitor = WinUser.MONITORINFO()
        if (!user32.GetMonitorInfo(user32.MonitorFromWindow(taskbar, WinUser.MONITOR_DEFAULTTOPRIMARY), monitor).booleanValue()) return null
        val screen = monitor.rcMonitor.toRectangle()
        val bar = rect.toRectangle().intersection(screen)
        // 自动隐藏后仅剩边缘触发条，不在桌面或全屏应用上显示歌词。
        if (bar.height < 16 || bar.width <= bar.height || isFullscreen(screen)) return null
        return bar
    }

    private fun isFullscreen(screen: Rectangle): Boolean {
        val foreground = user32.GetForegroundWindow() ?: return false
        val name = CharArray(256)
        user32.GetClassName(foreground, name, name.size)
        if (Native.toString(name) in setOf("Progman", "WorkerW", "Shell_TrayWnd")) return false
        val rect = RECT()
        return user32.GetWindowRect(foreground, rect) && rect.toRectangle().contains(screen)
    }

    fun configure(window: Window, locked: Boolean) {
        val handle = HWND(Native.getWindowPointer(window))
        val previousStyle = user32.GetWindowLong(handle, WinUser.GWL_EXSTYLE)
        var style = (previousStyle or WS_EX_NOACTIVATE or WS_EX_TOOLWINDOW or WS_EX_LAYERED) and WS_EX_APPWINDOW.inv()
        style = if (locked) style or WinUser.WS_EX_TRANSPARENT else style and WinUser.WS_EX_TRANSPARENT.inv()
        var flags = WinUser.SWP_NOMOVE or WinUser.SWP_NOSIZE or WinUser.SWP_NOACTIVATE
        if (style != previousStyle) {
            Native.setLastError(0)
            val previous = user32.SetWindowLong(handle, WinUser.GWL_EXSTYLE, style)
            check(previous != 0 || Native.getLastError() == 0) { "设置歌词窗口属性失败：${Native.getLastError()}" }
            flags = flags or WinUser.SWP_FRAMECHANGED
        }
        check(user32.SetWindowPos(handle, HWND(Pointer.createConstant(-1L)), 0, 0, 0, 0, flags)) {
            "设置歌词窗口层级失败：${Native.getLastError()}"
        }
    }

    private fun RECT.toRectangle() = Rectangle(left, top, right - left, bottom - top)

    companion object {
        const val WS_EX_NOACTIVATE = 0x08000000
        const val WS_EX_TOOLWINDOW = 0x00000080
        const val WS_EX_LAYERED = 0x00080000
        const val WS_EX_APPWINDOW = 0x00040000
    }
}
