package com.xu.music.player

import com.xu.music.player.tray.SwingTrayPopup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.roundToInt

class TrayPopupPlacementTest {
    @Test
    fun `logical cursor stays adjacent without double scaling and flips at work area edges`() {
        val size = Dimension(180, 200)
        val area = Rectangle(0, 0, 1920, 1040)
        for (scale in listOf(1.0, 1.25, 1.5, 2.0)) {
            val physical = Point((1200 * scale).roundToInt(), (600 * scale).roundToInt())
            val logical = Point((physical.x / scale).roundToInt(), (physical.y / scale).roundToInt())
            assertEquals(Rectangle(1200, 600, 180, 200), SwingTrayPopup.placement(logical, size, area))
        }
        assertEquals(Rectangle(1720, 600, 180, 200), SwingTrayPopup.placement(Point(1900, 600), size, area))
        assertEquals(Rectangle(1200, 800, 180, 200), SwingTrayPopup.placement(Point(1200, 1000), size, area))
        assertEquals(Rectangle(1720, 800, 180, 200), SwingTrayPopup.placement(Point(1900, 1000), size, area))
        assertEquals(Rectangle(0, 0, 180, 200), SwingTrayPopup.placement(Point(-20, -20), size, area))
        val leftScreen = Rectangle(-1920, -200, 1920, 1040)
        assertEquals(Rectangle(-200, 600, 180, 200), SwingTrayPopup.placement(Point(-20, 800), size, leftScreen))
    }
}
