package com.xu.music.player.taskbar

import com.sun.jna.Platform
import com.xu.music.player.entity.SongEntity
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.EventQueue
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JComponent
import javax.swing.JWindow
import javax.swing.Timer
import javax.swing.plaf.basic.BasicGraphicsUtils
import kotlin.math.roundToInt

/** 主屏任务栏上的独立透明窗口，所有窗口操作限制在 AWT 事件线程。 */
object TaskbarLyrics {
    val supported: Boolean get() = Platform.isWindows() && !GraphicsEnvironment.isHeadless()
    @Volatile private var text = ""
    private var enabled = false
    private var locked = true
    private var offset = DEFAULT_OFFSET
    private var lyricWidth = DEFAULT_WIDTH
    private var window: JWindow? = null
    private var native: WindowsTaskbar? = null
    private var timer: Timer? = null
    private var reportError: (String) -> Unit = {}
    private val log = LoggerFactory.getLogger(TaskbarLyrics::class.java)

    fun update(song: SongEntity?, lyric: String?) {
        val next = displayText(song, lyric)
        if (text == next) return
        text = next
        onAwt { if (enabled) refresh() }
    }

    fun setEnabled(value: Boolean, onError: (String) -> Unit = {}) = onAwt {
        enabled = value && supported
        reportError = onError
        if (enabled) {
            refresh()
            if (enabled) {
                if (timer == null) timer = Timer(500) { refresh() }
                timer?.start()
            }
        } else {
            timer?.stop()
            window?.isVisible = false
        }
    }

    fun setLocked(value: Boolean) = onAwt {
        locked = value
        if (enabled) refresh()
    }

    fun changeWidth(delta: Int) = onAwt {
        lyricWidth = (lyricWidth + delta).coerceIn(120, 800)
        if (enabled) refresh()
    }

    fun resetPosition() = onAwt {
        offset = DEFAULT_OFFSET
        lyricWidth = DEFAULT_WIDTH
        if (enabled) refresh()
    }

    fun dispose() = onAwt {
        enabled = false
        timer?.stop()
        timer = null
        window?.dispose()
        window = null
        native = null
        text = ""
        reportError = {}
    }

    private fun refresh() {
        try {
            if (!enabled) return
            val bridge = native ?: WindowsTaskbar().also { native = it }
            val bar = bridge.visibleBounds()
            if (bar == null || (locked && text.isBlank())) {
                window?.isVisible = false
                return
            }
            val view = window ?: createWindow().also { window = it }
            val scale = view.graphicsConfiguration.defaultTransform.scaleX
            val bounds = placement(bar, scale, offset, lyricWidth)
            if (bounds == null) {
                view.isVisible = false
                return
            }
            if (view.bounds != bounds) view.bounds = bounds
            offset = bounds.x - (bar.x / scale).roundToInt()
            bridge.configure(view, locked)
            view.isVisible = true
            view.repaint()
        } catch (error: Exception) {
            fail(error)
        } catch (error: LinkageError) {
            fail(error)
        }
    }

    private fun fail(error: Throwable) {
        enabled = false
        timer?.stop()
        window?.isVisible = false
        log.error("[TASKBAR-LYRICS] 任务栏歌词不可用", error)
        reportError(error.message ?: "无法创建任务栏歌词窗口")
    }

    private fun createWindow(): JWindow = JWindow().apply {
        name = "MusicPlayerTaskbarLyrics"
        type = Window.Type.UTILITY
        focusableWindowState = false
        isAutoRequestFocus = false
        background = Color(0, 0, 0, 0)
        isAlwaysOnTop = true
        contentPane.add(LyricText())
        // 提前创建原生句柄，首次显示前就设置不激活和穿透，避免抢走焦点。
        addNotify()
    }

    private class LyricText : JComponent() {
        init {
            font = Font("Microsoft YaHei UI", Font.PLAIN, 14)
            val mouse = object : MouseAdapter() {
                private var startX = 0
                private var startOffset = 0
                override fun mousePressed(event: MouseEvent) {
                    startX = event.xOnScreen
                    startOffset = offset
                }
                override fun mouseDragged(event: MouseEvent) {
                    if (locked) return
                    offset = (startOffset + event.xOnScreen - startX).coerceAtLeast(0)
                    refresh()
                }
                override fun mouseWheelMoved(event: MouseWheelEvent) {
                    if (!locked) changeWidth(-event.wheelRotation * 24)
                }
            }
            addMouseListener(mouse)
            addMouseMotionListener(mouse)
            addMouseWheelListener(mouse)
        }

        override fun paintComponent(graphics: Graphics) {
            val g = graphics.create() as Graphics2D
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g.font = font
                if (!locked) {
                    g.color = Color(0, 100, 180, 140)
                    g.fillRoundRect(0, 0, width, height, 8, 8)
                    g.color = Color(120, 190, 255)
                    g.drawRoundRect(0, 0, width - 1, height - 1, 8, 8)
                }
                val value = text.ifBlank { "拖动调整位置，滚轮调整宽度" }
                val metrics = g.fontMetrics
                val shown = BasicGraphicsUtils.getClippedString(this, metrics, value, width - 16)
                val x = (width - metrics.stringWidth(shown)) / 2
                val y = (height - metrics.height) / 2 + metrics.ascent
                g.color = Color(0, 0, 0, 210)
                for ((dx, dy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) g.drawString(shown, x + dx, y + dy)
                g.color = Color(235, 235, 235)
                g.drawString(shown, x, y)
            } finally { g.dispose() }
        }
    }

    private fun onAwt(action: () -> Unit) {
        if (EventQueue.isDispatchThread()) action() else EventQueue.invokeLater(action)
    }

    internal fun displayText(song: SongEntity?, lyric: String?): String {
        if (song == null) return ""
        lyric?.trim()?.takeIf { it.isNotEmpty() }?.let { return it.replace('\n', ' ').replace('\r', ' ') }
        val name = song.name.orEmpty().trim()
        val author = song.author.orEmpty().trim()
        // 导入的名称可能已经包含歌手，避免重复拼接。
        return when {
            name.isEmpty() -> author
            author.isEmpty() || author in name.split(" - ") -> name
            else -> "$name - $author"
        }
    }

    /** 任务栏坐标是物理像素，AWT 窗口使用主屏的逻辑像素。右侧为托盘预留空间。 */
    internal fun placement(bar: Rectangle, scale: Double, offset: Int, width: Int): Rectangle? {
        val available = (bar.width / scale).roundToInt() - 240
        if (available < 120) return null
        val actualWidth = width.coerceIn(120, minOf(800, available))
        val left = offset.coerceIn(0, available - actualWidth)
        return Rectangle((bar.x / scale).roundToInt() + left, (bar.y / scale).roundToInt(),
            actualWidth, (bar.height / scale).roundToInt())
    }

    private const val DEFAULT_OFFSET = 180
    private const val DEFAULT_WIDTH = 320
}
