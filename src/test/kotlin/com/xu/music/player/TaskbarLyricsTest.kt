package com.xu.music.player

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser
import com.xu.music.player.entity.SongEntity
import com.xu.music.player.taskbar.TaskbarLyrics
import com.xu.music.player.taskbar.WindowsTaskbar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.nio.file.Path
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.JWindow

class TaskbarLyricsTest {
    @Test
    fun `taskbar text follows lyric then song and clears when stopped`() {
        val song = SongEntity(name = "双宿双栖", author = "梦涵")
        assertEquals("当前歌词", TaskbarLyrics.displayText(song, " 当前歌词 "))
        assertEquals("双宿双栖 - 梦涵", TaskbarLyrics.displayText(song, null))
        assertEquals("双宿双栖 - 梦涵", TaskbarLyrics.displayText(song, "  "))
        assertEquals("双宿双栖", TaskbarLyrics.displayText(song.copy(author = null), null))
        assertEquals("", TaskbarLyrics.displayText(null, "上一首的歌词"))
        assertEquals("双宿双栖 - 梦涵", TaskbarLyrics.displayText(song.copy(name = "双宿双栖 - 梦涵"), null))
        assertEquals("梦涵 - 双宿双栖", TaskbarLyrics.displayText(song.copy(name = "梦涵 - 双宿双栖"), null))
        assertEquals("单行 歌词", TaskbarLyrics.displayText(song, "单行\n歌词"))
    }

    @Test
    fun `taskbar geometry uses logical pixels and clamps width and horizontal dragging`() {
        for (scale in listOf(1.0, 1.25, 1.5, 2.0)) {
            val bar = Rectangle(0, (1000 * scale).toInt(), (1920 * scale).toInt(), (48 * scale).toInt())
            assertEquals(Rectangle(180, 1000, 320, 48), TaskbarLyrics.placement(bar, scale, 180, 320))
            assertEquals(Rectangle(880, 1000, 800, 48), TaskbarLyrics.placement(bar, scale, 3000, 2000))
            assertEquals(Rectangle(0, 1000, 120, 48), TaskbarLyrics.placement(bar, scale, -100, 10))
        }
        assertNull(TaskbarLyrics.placement(Rectangle(0, 0, 300, 48), 1.0, 180, 320))
    }

    @Test
    fun `native lyric window remains non activating and toggles click through`() {
        assumeTrue(TaskbarLyrics.supported)
        onAwt {
            val window = JWindow().apply { setSize(200, 40); addNotify() }
            try {
                val bridge = WindowsTaskbar()
                val hwnd = HWND(Native.getWindowPointer(window))
                bridge.configure(window, true)
                val locked = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
                for (flag in listOf(WindowsTaskbar.WS_EX_NOACTIVATE, WindowsTaskbar.WS_EX_TOOLWINDOW,
                    WindowsTaskbar.WS_EX_LAYERED, WinUser.WS_EX_TRANSPARENT)) {
                    assertNotEquals(0, locked and flag)
                }
                assertEquals(0, locked and WindowsTaskbar.WS_EX_APPWINDOW)
                bridge.configure(window, false)
                val unlocked = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
                assertEquals(0, unlocked and WinUser.WS_EX_TRANSPARENT)
                assertNotEquals(0, unlocked and WindowsTaskbar.WS_EX_NOACTIVATE)
            } finally { window.dispose() }
        }
    }

    @Test
    fun `taskbar overlay enables resizes hides and disposes without changing foreground`() {
        assumeTrue(TaskbarLyrics.supported)
        assumeTrue(onAwt { WindowsTaskbar().visibleBounds() != null }, "需要可见的 Windows 主任务栏")
        try {
            onAwt {
                TaskbarLyrics.dispose()
                TaskbarLyrics.resetPosition()
                TaskbarLyrics.setLocked(true)
                TaskbarLyrics.update(SongEntity(name = "双宿双栖", author = "梦涵"), "正在播放的任务栏歌词")
                val foreground = User32.INSTANCE.GetForegroundWindow()
                TaskbarLyrics.setEnabled(true) { fail<Unit>(it) }
                val window = currentWindow()!!
                assertTrue(window.isShowing)
                assertEquals(foreground, User32.INSTANCE.GetForegroundWindow())
                assertFalse(window.focusableWindowState)
                val bar = WindowsTaskbar().visibleBounds()!!
                val nativeBounds = RECT()
                assertTrue(User32.INSTANCE.GetWindowRect(HWND(Native.getWindowPointer(window)), nativeBounds))
                assertEquals(bar.y.toDouble(), nativeBounds.top.toDouble(), 2.0)
                assertEquals((bar.y + bar.height).toDouble(), nativeBounds.bottom.toDouble(), 2.0)
                val width = window.width
                TaskbarLyrics.changeWidth(40)
                assertEquals(width + 40, window.width)
                TaskbarLyrics.setLocked(false)
                assertEquals(0, User32.INSTANCE.GetWindowLong(HWND(Native.getWindowPointer(window)), WinUser.GWL_EXSTYLE) and WinUser.WS_EX_TRANSPARENT)
                val component = window.contentPane.getComponent(0)
                val originalX = window.x
                component.dispatchEvent(MouseEvent(component, MouseEvent.MOUSE_PRESSED, 0, 0, 10, 10, 100, 100, 1, false, MouseEvent.BUTTON1))
                component.dispatchEvent(MouseEvent(component, MouseEvent.MOUSE_DRAGGED, 0, 0, 60, 10, 150, 100, 1, false, MouseEvent.BUTTON1))
                assertEquals(originalX + 50, window.x)
                component.dispatchEvent(MouseWheelEvent(component, MouseEvent.MOUSE_WHEEL, 0, 0, 10, 10, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1))
                assertEquals(width + 64, window.width)
                TaskbarLyrics.setLocked(true)
                TaskbarLyrics.update(null, null)
                assertFalse(window.isShowing)
                TaskbarLyrics.update(SongEntity(name = "双宿双栖", author = "梦涵"), null)
                assertTrue(window.isShowing)
                val image = BufferedImage(window.width, window.height, BufferedImage.TYPE_INT_ARGB)
                val graphics = image.createGraphics()
                try { window.contentPane.printAll(graphics) } finally { graphics.dispose() }
                ImageIO.write(image, "png", Path.of("taskbar-lyrics-preview.png").toFile())
                TaskbarLyrics.setEnabled(false)
                assertFalse(window.isShowing)
                TaskbarLyrics.dispose()
                assertFalse(window.isDisplayable)
                assertNull(currentWindow())
            }
        } finally { onAwt { TaskbarLyrics.dispose(); TaskbarLyrics.setLocked(true) } }
    }

    private fun currentWindow(): JWindow? = TaskbarLyrics::class.java.getDeclaredField("window")
        .apply { isAccessible = true }.get(TaskbarLyrics) as JWindow?

    private fun <T> onAwt(action: () -> T): T {
        val task = FutureTask(action)
        EventQueue.invokeLater(task)
        return task.get(10, TimeUnit.SECONDS)
    }
}
