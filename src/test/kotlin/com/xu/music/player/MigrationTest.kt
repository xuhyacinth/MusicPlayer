package com.xu.music.player

import com.xu.music.player.constant.Constant
import com.xu.music.player.controller.MusicPlayerController
import com.xu.music.player.entity.SongEntity
import com.xu.music.player.player.Player
import com.xu.music.player.wrapper.InsertWrapper
import com.xu.music.player.wrapper.QueryWrapper
import com.xu.music.player.wrapper.UpdateWrapper
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyCode
import javafx.scene.layout.StackPane
import javafx.scene.control.TextField
import javafx.scene.control.Slider
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.stage.Popup
import javafx.stage.StageStyle
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/** 所有数据库写入仅在 Maven 的 target 目录中执行。 */
class MigrationTest {
    @BeforeEach
    fun prepareDatabase() {
        val expected = System.getProperty("musicplayer.test.directory")
        assertNotNull(expected, "请通过 Maven 运行测试，避免操作项目数据库")
        assertEquals(Path.of(expected).toRealPath(), Path.of("").toRealPath())
        Files.createDirectories(Path.of("sqlite/db"))
        val schema = javaClass.getResourceAsStream("/song-schema.sql")!!.bufferedReader().use { it.readText() }
        DriverManager.getConnection("jdbc:sqlite:sqlite/db/MusicPlayer.db").use { connection ->
            connection.createStatement().use {
                it.executeUpdate("drop table if exists song")
                it.executeUpdate(schema)
            }
        }
    }

    @Test
    fun `tray uses Swing Chinese menus and preserves checkbox actions and disposal`(): Unit = onFx {
        org.junit.jupiter.api.Assumptions.assumeTrue(java.awt.SystemTray.isSupported())
        val stage = Stage()
        com.xu.music.player.tray.MusicPlayerTray.tray(stage)
        val task = FutureTask {
            val type = com.xu.music.player.tray.MusicPlayerTray::class.java
            val field = type.getDeclaredField("trayPopup").apply { isAccessible = true }
            val popup = field.get(null) as com.xu.music.player.tray.SwingTrayPopup
            try {
                val items = popup.menu.components.filterIsInstance<javax.swing.JMenuItem>()
                assertEquals("显示主窗口", items.first().text)
                assertEquals("关闭", items.last().text)
                assertTrue(items.all { it.font.canDisplayUpTo(it.text) == -1 })
                assertTrue(java.awt.SystemTray.getSystemTray().trayIcons.all { it.popupMenu == null })
                val lock = items.filterIsInstance<javax.swing.JCheckBoxMenuItem>()
                    .firstOrNull { it.text == "锁定歌词位置（鼠标穿透）" }
                lock?.let {
                    val locked = com.xu.music.player.taskbar.TaskbarLyrics::class.java
                        .getDeclaredField("locked").apply { isAccessible = true }
                    assertTrue(it.isSelected)
                    it.doClick(0)
                    assertFalse(it.isSelected)
                    assertFalse(locked.getBoolean(null))
                    it.doClick(0)
                    assertTrue(it.isSelected)
                    assertTrue(locked.getBoolean(null))
                }
                val pointer = java.awt.MouseInfo.getPointerInfo()
                val config = pointer.device.defaultConfiguration
                val bounds = config.bounds
                val insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(config)
                val available = java.awt.Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                    bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom)
                val icon = java.awt.SystemTray.getSystemTray().trayIcons.single()
                val trigger = java.awt.event.MouseEvent(java.awt.Canvas(), java.awt.event.MouseEvent.MOUSE_RELEASED,
                    0, 0, -30000, -30000, 1, true,
                    java.awt.event.MouseEvent.BUTTON3)
                icon.mouseListeners.forEach { it.mouseReleased(trigger) }
                assertTrue(popup.menu.isVisible)
                assertTrue(bounds.contains(popup.owner.bounds))
                // 故意传入远离鼠标的事件坐标，验证托盘入口使用真实鼠标逻辑坐标。
                if (pointer.location == java.awt.MouseInfo.getPointerInfo().location) {
                    assertEquals(com.xu.music.player.tray.SwingTrayPopup.placement(pointer.location,
                        popup.menu.preferredSize, available), popup.owner.bounds)
                }
                val image = BufferedImage(popup.menu.width, popup.menu.height, BufferedImage.TYPE_INT_ARGB)
                val graphics = image.createGraphics()
                try { popup.menu.printAll(graphics) } finally { graphics.dispose() }
                ImageIO.write(image, "png", Path.of("swing-tray-menu-preview.png").toFile())
                popup.menu.isVisible = false
                assertFalse(popup.owner.isVisible)
                popup.showAt(java.awt.Point(bounds.x + 50, bounds.y + 50))
                assertTrue(popup.menu.isVisible)
            } finally {
                com.xu.music.player.tray.MusicPlayerTray.dispose()
                assertFalse(popup.owner.isDisplayable)
                assertNull(field.get(null))
            }
        }
        java.awt.EventQueue.invokeLater(task)
        try { task.get(10, TimeUnit.SECONDS) } finally { stage.close() }
    }

    @Test
    fun `Kotlin wrappers preserve song fields and quote names`() {
        val song = SongEntity(id = "song-1", name = "歌手's 歌曲", index = 1,
            songPath = "C:/音乐/歌曲.wav", length = 123.0)
        assertEquals(1, InsertWrapper(song, "song").insert())
        val result = QueryWrapper(SongEntity::class.java, "song").eq("id", song.id).list().single()
        assertEquals(song.copy(flag = 0), result)
        assertEquals(1, QueryWrapper(SongEntity::class.java, "song").like("name", "'s").list().size)
        assertEquals(1, QueryWrapper(SongEntity::class.java, "song").likeLeft("name", "歌曲").list().size)
        assertEquals(1, QueryWrapper(SongEntity::class.java, "song").likeRight("name", "歌手").list().size)
        assertEquals(1, QueryWrapper(SongEntity::class.java, "song").eq(false, "id", "absent").list().size)
    }

    @Test
    fun `update and delete retain their condition scope`() {
        InsertWrapper(SongEntity(id = "first", name = "第一首"), "song").insert()
        UpdateWrapper(SongEntity(id = "second", name = "第二首"), "song").insert()
        assertEquals(1, UpdateWrapper(SongEntity(name = "已修改"), "song").eq("id", "first").update())
        val songs = QueryWrapper(SongEntity::class.java, "song").last("order by id").list()
        assertEquals(listOf("已修改", "第二首"), songs.map { it.name })
        assertEquals(1, UpdateWrapper(SongEntity(), "song").eq("id", "first").delete(""))
        assertEquals("second", QueryWrapper(SongEntity::class.java, "song").apply("id = 'second'").list().single().id)
    }

    @Test
    fun `FXML loads layout CSS images and resize bindings without database initialization`() = onFx {
        val loader = loader()
        val root = loader.load<Parent>()
        val controller = loader.getController<MusicPlayerController>()
        try {
            Scene(root, 900.0, 486.0)
            root.applyCss()
            root.layout()
            val table = loader.namespace["lists"] as TableView<*>
            assertEquals(listOf("序号", "歌曲"), table.columns.map { it.text })
            assertTrue(table.items.isEmpty())
            assertNotNull((root.lookup(".add-song-button") as Button).onAction)
            val footer = loader.namespace["foot"] as StackPane
            assertEquals(Color.web("#f0f0f0"), footer.background.fills.first().fill)
            val canvas = loader.namespace["spectrumCanvas"] as Canvas
            assertEquals(footer.width, canvas.width, 0.01)
            assertEquals(70.0, footer.height, 0.01)
            assertEquals(6.0, (loader.namespace["progress"] as ProgressBar).height, 0.01)
            footer.resize(1100.0, footer.height)
            assertEquals(1100.0, canvas.width, 0.01)
            for (name in listOf("prev", "start", "nextButton")) {
                val image = (loader.namespace[name] as ImageView).image
                assertNotNull(image)
                assertFalse(image.isError)
                assertTrue(image.width > 0)
            }
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun `FXML player events load songs navigate pause and release resources`() {
        val firstFile = Files.createTempFile(Path.of("."), "first-", ".wav").toAbsolutePath()
        val secondFile = Files.createTempFile(Path.of("."), "second-", ".wav").toAbsolutePath()
        val lrc = Files.createTempFile(Path.of("."), "lyrics-", ".lrc").toAbsolutePath()
        Files.writeString(lrc, "[00:00.00]第一行歌词\n[00:05.00]第二行歌词")
        InsertWrapper(SongEntity(id = "one", name = "测试歌曲一", author = "Beyond", index = 1,
            songPath = firstFile.toString(), lyricPath = lrc.toString()), "song").insert()
        InsertWrapper(SongEntity(id = "two", name = "测试歌曲二", index = 2,
            songPath = secondFile.toString(), lyricPath = lrc.toString()), "song").insert()
        try {
            onFx {
                val loader = loader()
                val root = loader.load<Parent>()
                val controller = loader.getController<MusicPlayerController>()
                val stage = Stage()
                stage.scene = Scene(root, 900.0, 486.0)
                var active = false
                var paused = false
                var stopped = false
                var loaded: String? = null
                var seekPosition = -1.0
                var volume = 1.0f
                val fake = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, args ->
                    when (method.name) {
                        "loadAsync" -> {
                            loaded = args[0] as String
                            @Suppress("UNCHECKED_CAST")
                            (args[1] as () -> Unit).invoke()
                            null
                        }
                        "load" -> { loaded = args[0] as String; null }
                        "play" -> { active = true; null }
                        "pause" -> { active = false; paused = true; null }
                        "resume" -> { active = true; paused = false; null }
                        "stop" -> { active = false; stopped = true; null }
                        "seek" -> { seekPosition = args[0] as Double; null }
                        "volume" -> { volume = args[0] as Float; null }
                        "playing" -> active
                        "pausing" -> paused
                        "position" -> 5.0
                        "duration" -> 120.0
                        else -> null
                    }
                } as Player
                MusicPlayerController::class.java.getDeclaredField("player").apply { isAccessible = true }.set(controller, fake)
                try {
                    assertEquals(StageStyle.DECORATED, stage.style)
                    stage.show()
                    controller.attach(stage)
                    @Suppress("UNCHECKED_CAST")
                    val table = loader.namespace["lists"] as TableView<SongEntity>
                    assertEquals(2, table.items.size)
                    table.selectionModel.select(0)
                    assertNull(loaded)
                    root.applyCss()
                    root.layout()
                    val row = table.lookupAll(".table-row-cell").filterIsInstance<javafx.scene.control.TableRow<*>>()
                        .first { !it.isEmpty && it.index == 0 }
                    row.fireEvent(mouse(MouseEvent.MOUSE_CLICKED, 2))
                    assertEquals(firstFile.toString(), loaded)
                    assertTrue(active)
                    val lyrics = loader.namespace["lyrics"] as ListView<*>
                    assertEquals(2, lyrics.items.filterIsInstance<MusicPlayerController.LyricLine>().count { it.time >= 0.0 })
                    val play = loader.namespace["start"] as ImageView
                    play.fireEvent(mouse(MouseEvent.MOUSE_CLICKED))
                    assertTrue(paused)
                    play.fireEvent(mouse(MouseEvent.MOUSE_CLICKED))
                    assertFalse(paused)
                    val next = loader.namespace["nextButton"] as ImageView
                    next.fireEvent(mouse(MouseEvent.MOUSE_PRESSED))
                    next.fireEvent(mouse(MouseEvent.MOUSE_RELEASED))
                    assertEquals(secondFile.toString(), loaded)
                    next.fireEvent(mouse(MouseEvent.MOUSE_RELEASED))
                    assertEquals(firstFile.toString(), loaded)
                    val prev = loader.namespace["prev"] as ImageView
                    prev.fireEvent(mouse(MouseEvent.MOUSE_PRESSED))
                    prev.fireEvent(mouse(MouseEvent.MOUSE_RELEASED))
                    assertEquals(secondFile.toString(), loaded)
                    Constant.SPECTRUM_FOREGROUND_COLOR = "#000000"
                    (loader.namespace["foot"] as StackPane).fireEvent(mouse(MouseEvent.MOUSE_CLICKED, 2))
                    assertNotEquals("#000000", Constant.SPECTRUM_FOREGROUND_COLOR)
                    root.applyCss()
                    root.layout()
                    val search = loader.namespace["songSearch"] as TextField
                    search.text = " bEyOnD "
                    assertEquals(listOf("one"), table.items.map { it.id })
                    assertEquals(secondFile.toString(), loaded)
                    assertTrue(active)
                    assertNull(Constant.PLAYING_INDEX)
                    next.fireEvent(mouse(MouseEvent.MOUSE_RELEASED))
                    assertEquals(firstFile.toString(), loaded)
                    search.text = "不存在的歌曲"
                    assertTrue(table.items.isEmpty())
                    next.fireEvent(mouse(MouseEvent.MOUSE_RELEASED))
                    assertEquals(firstFile.toString(), loaded)
                    search.text = "歌曲二"
                    table.selectionModel.select(0)
                    assertEquals(firstFile.toString(), loaded)
                    table.fireEvent(KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false))
                    assertEquals(secondFile.toString(), loaded)
                    search.fireEvent(KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false))
                    assertEquals("", search.text)
                    search.text = "歌曲二"
                    (loader.namespace["clearSearch"] as Button).fire()
                    assertEquals(2, table.items.size)
                    assertEquals("two", table.selectionModel.selectedItem.id)
                    assertEquals(1, Constant.PLAYING_INDEX)

                    val track = loader.namespace["progressTrack"] as StackPane
                    fun seekAt(fraction: Double) {
                        val point = track.localToScene(track.width * fraction, 10.0)
                        track.fireEvent(MouseEvent(MouseEvent.MOUSE_CLICKED,
                            point.x, point.y, point.x, point.y, MouseButton.PRIMARY, 1,
                            false, false, false, false, false, false, false, false, false, false, null))
                    }
                    play.fireEvent(mouse(MouseEvent.MOUSE_CLICKED))
                    assertTrue(paused)
                    seekAt(0.51)
                    assertEquals(61.2, seekPosition, 0.1)
                    assertTrue(paused)
                    assertFalse(active)
                    assertEquals("01:01", (loader.namespace["timeLabel1"] as Label).text)
                    assertEquals(0.51, (loader.namespace["progress"] as ProgressBar).progress, 0.001)
                    fun assertLyricColors(currentText: String) {
                        root.applyCss()
                        root.layout()
                        val cells = lyrics.lookupAll(".list-cell")
                            .filterIsInstance<javafx.scene.control.ListCell<*>>()
                            .filter { !it.isEmpty && !it.text.isNullOrEmpty() }
                        assertEquals(2, cells.size)
                        for (cell in cells) {
                            val current = cell.text == currentText
                            if (current) {
                                val bounds = cell.localToScene(cell.boundsInLocal)
                                val viewport = lyrics.localToScene(lyrics.boundsInLocal)
                                assertEquals(viewport.centerY, bounds.centerY, 1.0, "当前歌词应位于视口中央")
                            }
                            assertEquals(Color.web(if (current) "#0078d7" else "#202020"), cell.textFill)
                            assertEquals(Color.web(if (current) "#d3d3d3" else "#ffffff"),
                                cell.background.fills.first().fill)
                        }
                    }
                    val lyricField = MusicPlayerController::class.java.getDeclaredField("currentLyric").apply { isAccessible = true }
                    assertEquals("第二行歌词", (lyricField.get(controller) as MusicPlayerController.LyricLine).text)
                    assertLyricColors("第二行歌词")
                    seekAt(0.0)
                    assertEquals(0.0, seekPosition, 0.001)
                    assertEquals("第一行歌词", (lyricField.get(controller) as MusicPlayerController.LyricLine).text)
                    assertLyricColors("第一行歌词")
                    play.fireEvent(mouse(MouseEvent.MOUSE_CLICKED))
                    seekAt(1.2)
                    assertEquals(120.0, seekPosition, 0.001)
                    assertTrue(active)
                    assertFalse(paused)

                    val volumeButton = loader.namespace["volumeButton"] as Button
                    val popup = loader.namespace["volumePopup"] as Popup
                    volumeButton.fire()
                    assertTrue(popup.isShowing)
                    val slider = loader.namespace["volumeSlider"] as Slider
                    slider.value = 35.0
                    assertEquals(0.35f, volume)
                    assertEquals("35%", (loader.namespace["volumeLabel"] as Label).text)
                    saveSnapshot(popup.content.single() as Parent, "volume-preview.png")
                    slider.value = 0.0
                    assertEquals(0.0f, volume)
                    volumeButton.fire()
                    assertFalse(popup.isShowing)
                    volumeButton.fire()
                    assertTrue(popup.isShowing)
                    root.fireEvent(KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false))
                    assertFalse(popup.isShowing)
                    volumeButton.fire()
                    assertTrue(popup.isShowing)
                    volumeButton.fireEvent(mouse(MouseEvent.MOUSE_PRESSED))
                    assertTrue(popup.isShowing)
                    root.fireEvent(mouse(MouseEvent.MOUSE_PRESSED))
                    assertFalse(popup.isShowing)
                    seekAt(0.26)
                    lyrics.requestFocus()
                    root.applyCss()
                    saveSnapshot(root, "fxml-preview.png")
                    val playlistWidth = table.width
                    root.resize(1280.0, 720.0)
                    root.layout()
                    assertEquals(1280.0, (loader.namespace["foot"] as StackPane).width, 0.01)
                    assertTrue(track.width > 800.0)
                    assertEquals(playlistWidth, table.width, 1.0)
                    assertLyricColors("第二行歌词")
                    saveSnapshot(root, "fxml-wide-preview.png")
                    root.resize(640.0, 360.0)
                    root.layout()
                    assertTrue(track.width >= 100.0)
                    assertEquals(playlistWidth, table.width, 1.0)
                    assertTrue(volumeButton.localToScene(volumeButton.boundsInLocal).maxX <= 640.0)
                    assertLyricColors("第二行歌词")
                    saveSnapshot(root, "fxml-small-preview.png")
                    volumeButton.fire()
                    assertTrue(popup.isShowing)
                } finally {
                    controller.dispose()
                    assertTrue(stopped)
                    assertFalse((loader.namespace["volumePopup"] as Popup).isShowing)
                    stage.close()
                    Constant.PLAYING_LIST.clear()
                    Constant.PLAYING_INDEX = null
                    Constant.PLAYING_SONG = null
                    Constant.PLAYING_LYRIC = false
                    Constant.SPECTRUM_FOREGROUND_COLOR = "#4169E1"
                }
            }
        } finally {
            Files.deleteIfExists(firstFile)
            Files.deleteIfExists(secondFile)
            Files.deleteIfExists(lrc)
        }
    }

    @Test
    fun `native WAV playback reaches end and disposes resources`() {
        val wav = Files.createTempFile(Path.of("."), "playback-", ".wav")
        val format = javax.sound.sampled.AudioFormat(44100f, 16, 1, true, false)
        javax.sound.sampled.AudioInputStream(java.io.ByteArrayInputStream(ByteArray(88200)), format, 44100).use {
            javax.sound.sampled.AudioSystem.write(it, javax.sound.sampled.AudioFileFormat.Type.WAVE, wav.toFile())
        }
        val player = com.xu.music.player.player.MediaPlayer()
        val completed = java.util.concurrent.CompletableFuture<Unit>()
        try {
            onFx {
                player.load(wav.toFile())
                val native = player.javaClass.getDeclaredField("mediaPlayer").apply { isAccessible = true }
                    .get(player) as javafx.scene.media.MediaPlayer
                native.setOnError { completed.completeExceptionally(native.error) }
                player.onEndOfMedia = { completed.complete(Unit) }
                player.volume(0f)
                player.play()
            }
            completed.get(15, TimeUnit.SECONDS)
            onFx {
                assertFalse(player.playing())
                assertEquals(1.0, player.duration(), 0.1)
                assertTrue(player.position() > 0.0)
            }
        } finally {
            onFx {
                player.stop()
                assertFalse(player.playing())
                assertEquals(0.0, player.position())
            }
            // 媒体后端异步释放文件句柄，测试音频由下一次 Maven clean 清理。
        }
    }

    @Test
    fun `native seek preserves pause state and volume survives reload`() {
        val wav = Files.createTempFile(Path.of("."), "seek-", ".wav")
        val format = javax.sound.sampled.AudioFormat(44100f, 16, 1, true, false)
        javax.sound.sampled.AudioInputStream(java.io.ByteArrayInputStream(ByteArray(441000)), format, 220500).use {
            javax.sound.sampled.AudioSystem.write(it, javax.sound.sampled.AudioFileFormat.Type.WAVE, wav.toFile())
        }
        val player = com.xu.music.player.player.MediaPlayer()
        lateinit var native: javafx.scene.media.MediaPlayer
        try {
            onFx {
                player.volume(0.27f)
                player.load(wav.toFile())
                native = player.javaClass.getDeclaredField("mediaPlayer").apply { isAccessible = true }
                    .get(player) as javafx.scene.media.MediaPlayer
                assertEquals(0.27, native.volume, 0.001)
                player.play()
            }
            awaitFx { native.status == javafx.scene.media.MediaPlayer.Status.PLAYING }
            onFx { player.pause() }
            awaitFx { native.status == javafx.scene.media.MediaPlayer.Status.PAUSED }
            onFx { player.seek(2.0) }
            awaitFx { kotlin.math.abs(player.position() - 2.0) < 0.2 }
            onFx {
                assertTrue(player.pausing())
                assertFalse(player.playing())
                assertEquals(javafx.scene.media.MediaPlayer.Status.PAUSED, native.status)
                player.seek(-1.0)
            }
            awaitFx { player.position() < 0.1 }
            onFx {
                player.resume(0)
                player.seek(3.0)
            }
            awaitFx { native.status == javafx.scene.media.MediaPlayer.Status.PLAYING && player.position() >= 2.9 }
            onFx {
                assertTrue(player.playing())
                assertFalse(player.pausing())
                player.volume(0f)
                player.load(wav.toFile())
                native = player.javaClass.getDeclaredField("mediaPlayer").apply { isAccessible = true }
                    .get(player) as javafx.scene.media.MediaPlayer
                assertEquals(0.0, native.volume)
                player.volume(2f)
                assertEquals(1.0, native.volume)
            }
        } finally {
            onFx { player.stop() }
        }
    }

    @Test
    fun `FLAC decoding stops at declared samples and releases temporary WAV`() {
        val source = Path.of(javaClass.getResource("/playback-tone.flac")!!.toURI()).toFile()
        val task = FutureTask { com.xu.music.player.player.PreparedAudio.prepare(source) }
        Thread(task, "flac-regression").apply { isDaemon = true; start() }
        try {
            val prepared = task.get(5, TimeUnit.SECONDS)
            try {
                javax.sound.sampled.AudioSystem.getAudioInputStream(prepared.file).use { wav ->
                    assertEquals(15435L, wav.frameLength)
                    assertEquals(2, wav.format.channels)
                    assertEquals(16, wav.format.sampleSizeInBits)
                }
            } finally { prepared.close() }
            assertFalse(prepared.file.exists())
            assertTrue(source.exists())
        } finally { task.cancel(true) }
    }

    @Test
    fun `FLAC unknown sample count and truncated input fail instead of hanging`() {
        val bytes = javaClass.getResourceAsStream("/playback-tone.flac")!!.use { it.readBytes() }
        val unknown = bytes.copyOf()
        unknown[21] = (unknown[21].toInt() and 0xf0).toByte()
        for (i in 22..25) unknown[i] = 0
        for (content in listOf(unknown, bytes.copyOf(bytes.size - 500))) {
            val file = Files.createTempFile("invalid-flac-", ".flac")
            Files.write(file, content)
            val task = FutureTask { com.xu.music.player.player.PreparedAudio.prepare(file.toFile()) }
            Thread(task, "invalid-flac-regression").apply { isDaemon = true; start() }
            try {
                assertThrows(java.util.concurrent.ExecutionException::class.java) { task.get(5, TimeUnit.SECONDS) }
            } finally {
                task.cancel(true)
                Files.deleteIfExists(file)
            }
        }
    }

    @Test
    fun `async FLAC load ignores queued stale result and stop cancels callbacks`() {
        val path = Path.of(javaClass.getResource("/playback-tone.flac")!!.toURI()).toString()
        val player = com.xu.music.player.player.MediaPlayer()
        val callbacks = java.util.concurrent.CopyOnWriteArrayList<String>()
        val errors = java.util.concurrent.CopyOnWriteArrayList<Exception>()
        val ready = CountDownLatch(1)
        var abandoned: com.xu.music.player.player.PreparedAudio? = null
        val pending = player.javaClass.getDeclaredField("pendingAudio").apply { isAccessible = true }
        val lock = player.javaClass.getDeclaredField("loadLock").apply { isAccessible = true }.get(player)
        try {
            onFx {
                player.volume(0f)
                player.loadAsync(path, { callbacks.add("old") }, { errors.add(it) })
                // 暂缓 FX 队列，确定旧任务已完成解码，再制造切歌竞态。
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                while (abandoned == null && System.nanoTime() < deadline) {
                    abandoned = synchronized(lock) { pending.get(player) as com.xu.music.player.player.PreparedAudio? }
                    Thread.sleep(5)
                }
                assertNotNull(abandoned)
                player.loadAsync(path, {
                    assertTrue(Platform.isFxApplicationThread())
                    callbacks.add("latest")
                    ready.countDown()
                }, { errors.add(it); ready.countDown() })
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            assertTrue(errors.isEmpty(), errors.toString())
            assertEquals(listOf("latest"), callbacks)
            assertFalse(abandoned!!.file.exists())
            onFx {
                assertEquals(0.35, player.duration(), 0.02)
                player.loadAsync(path, { callbacks.add("cancelled") }, { errors.add(it) })
                player.stop()
            }
            Thread.sleep(300)
            onFx { assertFalse(player.playing()) }
            onFx {
                assertNull(player.javaClass.getDeclaredField("preparedAudio").apply { isAccessible = true }.get(player))
            }
            assertEquals(listOf("latest"), callbacks)
            assertTrue(errors.isEmpty(), errors.toString())
        } finally { onFx { player.stop() } }
    }

    @Test
    fun `async invalid audio reports failure on FX thread and remains stopped`() {
        val file = Files.createTempFile(Path.of("."), "invalid-audio-", ".wav").toAbsolutePath()
        file.toFile().deleteOnExit()
        Files.writeString(file, "not audio")
        val player = com.xu.music.player.player.MediaPlayer()
        val failed = CountDownLatch(1)
        val loaded = java.util.concurrent.atomic.AtomicBoolean()
        val callbackOnFx = java.util.concurrent.atomic.AtomicBoolean()
        try {
            onFx {
                player.loadAsync(file.toString(), { loaded.set(true) }, {
                    callbackOnFx.set(Platform.isFxApplicationThread())
                    failed.countDown()
                })
            }
            assertTrue(failed.await(10, TimeUnit.SECONDS))
            assertTrue(callbackOnFx.get())
            assertFalse(loaded.get())
            onFx { assertFalse(player.playing()); assertFalse(player.pausing()) }
        } finally {
            onFx { player.stop() }
            // JavaFX 读取损坏媒体时可能持有句柄至进程退出；文件仅位于测试目录。
            file.toFile().delete()
        }
    }

    @Test
    fun `controller loading failure and disposed callbacks cannot publish stale songs`() {
        val first = Files.createTempFile(Path.of("."), "async-one-", ".wav").toAbsolutePath()
        val second = Files.createTempFile(Path.of("."), "async-two-", ".wav").toAbsolutePath()
        InsertWrapper(SongEntity(id = "one", name = "异步歌曲一", index = 1, songPath = first.toString()), "song").insert()
        InsertWrapper(SongEntity(id = "two", name = "异步歌曲二", index = 2, songPath = second.toString()), "song").insert()
        try {
            onFx {
                val loader = loader()
                val root = loader.load<Parent>()
                val controller = loader.getController<MusicPlayerController>()
                val stage = Stage().apply { scene = Scene(root, 900.0, 486.0) }
                val successes = mutableListOf<() -> Unit>()
                val failures = mutableListOf<(Exception) -> Unit>()
                var plays = 0
                val fake = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, args ->
                    when (method.name) {
                        "loadAsync" -> {
                            @Suppress("UNCHECKED_CAST")
                            successes.add(args[1] as () -> Unit)
                            @Suppress("UNCHECKED_CAST")
                            failures.add(args[2] as (Exception) -> Unit)
                            null
                        }
                        "play" -> { plays++; null }
                        "playing", "pausing" -> false
                        "position", "duration" -> 0.0
                        else -> null
                    }
                } as Player
                MusicPlayerController::class.java.getDeclaredField("player").apply { isAccessible = true }.set(controller, fake)
                try {
                    stage.show()
                    controller.attach(stage)
                    val table = loader.namespace["lists"] as TableView<*>
                    val status = loader.namespace["playbackStatus"] as Label
                    fun choose(index: Int) {
                        table.selectionModel.select(index)
                        table.fireEvent(KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false))
                    }
                    choose(0)
                    assertTrue(status.text.startsWith("正在加载"))
                    root.applyCss()
                    root.layout()
                    saveSnapshot(root, "loading-preview.png")
                    assertNull(Constant.PLAYING_SONG)
                    assertEquals(0, plays)
                    choose(1)
                    successes[0]()
                    failures[0](IllegalStateException("过期错误"))
                    assertEquals(0, plays)
                    assertTrue(status.text.contains("异步歌曲二"))
                    successes[1]()
                    assertEquals(1, plays)
                    assertEquals("two", Constant.PLAYING_SONG?.id)
                    assertEquals("", status.text)
                    choose(0)
                    failures[2](IllegalArgumentException("测试无效音频"))
                    assertTrue(status.text.startsWith("播放失败"))
                    assertNull(Constant.PLAYING_SONG)
                    assertFalse(Constant.MUSIC_PLAYER_PLAYING_STATE)
                    choose(1)
                    controller.dispose()
                    successes[3]()
                    assertEquals(1, plays)
                    assertNull(Constant.PLAYING_SONG)
                } finally {
                    controller.dispose()
                    stage.close()
                    Constant.PLAYING_LIST.clear()
                    Constant.PLAYING_INDEX = null
                    Constant.PLAYING_SONG = null
                    Constant.PLAYING_LYRIC = false
                    Constant.MUSIC_PLAYER_PLAYING_STATE = false
                }
            }
        } finally {
            Files.deleteIfExists(first)
            Files.deleteIfExists(second)
        }
    }

    @Test
    fun `lyrics stay centered at first middle last and single line after resize and seek`() {
        val lrc = Files.createTempFile(Path.of("."), "centered-lyrics-", ".lrc").toAbsolutePath()
        Files.writeString(lrc, (0 until 100).joinToString("\n") { index ->
            "[%02d:%02d.00]居中歌词第%d行".format(index / 6, index % 6 * 10, index + 1)
        })
        try {
            onFx {
                val loader = loader()
                val root = loader.load<Parent>()
                val controller = loader.getController<MusicPlayerController>()
                val stage = Stage().apply { scene = Scene(root, 900.0, 486.0) }
                val lyrics = loader.namespace["lyrics"] as ListView<*>
                val initialize = MusicPlayerController::class.java.getDeclaredMethod("initLyric").apply { isAccessible = true }
                val update = MusicPlayerController::class.java.getDeclaredMethod("updateLyric", Double::class.javaPrimitiveType)
                    .apply { isAccessible = true }
                fun assertCentered(text: String) {
                    repeat(2) {
                        root.applyCss()
                        root.layout()
                    }
                    val current = lyrics.lookupAll(".list-cell")
                        .filterIsInstance<javafx.scene.control.ListCell<*>>()
                        .single { !it.isEmpty && it.text == text }
                    val bounds = current.localToScene(current.boundsInLocal)
                    val viewport = lyrics.localToScene(lyrics.boundsInLocal)
                    assertEquals(viewport.centerY, bounds.centerY, 1.0, text)
                    assertEquals(Color.web("#0078d7"), current.textFill, "$text ${current.pseudoClassStates}")
                    assertEquals(Color.web("#d3d3d3"), current.background.fills.first().fill)
                    lyrics.lookupAll(".list-cell").filterIsInstance<javafx.scene.control.ListCell<*>>()
                        .filter { !it.isEmpty && !it.text.isNullOrEmpty() && it.text != text }
                        .forEach {
                            assertEquals(Color.web("#202020"), it.textFill)
                            assertEquals(Color.WHITE, it.background.fills.first().fill)
                        }
                }
                try {
                    stage.show()
                    Constant.PLAYING_SONG = SongEntity(id = "center", lyricPath = lrc.toString())
                    initialize.invoke(controller)
                    for ((width, height) in listOf(900.0 to 486.0, 1280.0 to 720.0, 640.0 to 360.0)) {
                        root.resize(width, height)
                        for (index in listOf(0, 1, 47, 48, 99, 0, 99)) {
                            update.invoke(controller, index * 10.0)
                            assertCentered("居中歌词第${index + 1}行")
                        }
                    }
                    for (index in 0 until 100) {
                        update.invoke(controller, index * 10.0)
                        assertCentered("居中歌词第${index + 1}行")
                    }
                    for ((width, height) in listOf(1280.0 to 720.0, 640.0 to 360.0, 900.0 to 486.0)) {
                        root.resize(width, height)
                        assertCentered("居中歌词第100行")
                    }
                    root.resize(900.0, 486.0)
                    update.invoke(controller, 470.0)
                    assertCentered("居中歌词第48行")
                    saveSnapshot(root, "lyrics-center-preview.png")
                    Files.writeString(lrc, "[00:00.00]单行歌词")
                    initialize.invoke(controller)
                    update.invoke(controller, 0.0)
                    assertCentered("单行歌词")
                } finally {
                    controller.dispose()
                    stage.close()
                    Constant.PLAYING_SONG = null
                    Constant.PLAYING_LYRIC = false
                }
            }
        } finally { Files.deleteIfExists(lrc) }
    }

    @Test
    fun `repeated playback ticks do not move the current lyric between pulses`() {
        val lrc = Files.createTempFile(Path.of("."), "lyric-stability-", ".lrc").toAbsolutePath()
        Files.writeString(lrc, (0 until 100).joinToString("\n") { index ->
            "[%02d:%02d.00]稳定歌词第%d行".format(index / 6, index % 6 * 10, index + 1)
        })
        val update = MusicPlayerController::class.java.getDeclaredMethod("updateLyric", Double::class.javaPrimitiveType)
            .apply { isAccessible = true }
        val (root, controller, lyrics) = onFx {
            val loader = loader()
            val root = loader.load<Parent>()
            val controller = loader.getController<MusicPlayerController>()
            Stage().apply { scene = Scene(root, 900.0, 486.0); show() }
            Constant.PLAYING_SONG = SongEntity(id = "stability", lyricPath = lrc.toString())
            MusicPlayerController::class.java.getDeclaredMethod("initLyric").apply { isAccessible = true }.invoke(controller)
            Triple(root, controller, loader.namespace["lyrics"] as ListView<*>)
        }
        try {
            for (index in listOf(47, 48, 99, 0)) {
                var baseline: Double? = null
                repeat(15) { tick ->
                    val y = onFx {
                        update.invoke(controller, index * 10.0 + tick * 0.1)
                        root.applyCss()
                        root.layout()
                        val cell = lyrics.lookupAll(".list-cell")
                            .filterIsInstance<javafx.scene.control.ListCell<*>>()
                            .single { !it.isEmpty && it.text == "稳定歌词第${index + 1}行" }
                        val center = cell.localToScene(cell.boundsInLocal).centerY
                        assertEquals(lyrics.localToScene(lyrics.boundsInLocal).centerY, center, 1.0)
                        center
                    }
                    if (baseline == null) baseline = y
                    else assertEquals(baseline!!, y, 0.01, "第${index + 1}行第${tick + 1}次刷新不应移动")
                    Thread.sleep(30)
                }
            }
        } finally {
            onFx {
                controller.dispose()
                (root.scene.window as Stage).close()
                Constant.PLAYING_SONG = null
                Constant.PLAYING_LYRIC = false
            }
            Files.deleteIfExists(lrc)
        }
    }

    private fun awaitFx(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            if (onFx(condition)) return
            Thread.sleep(50)
        }
        fail<Unit>("等待 JavaFX 媒体状态超时")
    }

    private fun loader() = FXMLLoader(MusicPlayer::class.java.getResource("view/music-player.fxml"))

    private fun mouse(type: javafx.event.EventType<MouseEvent>, count: Int = 1) = MouseEvent(
        type, 0.0, 0.0, 0.0, 0.0, MouseButton.PRIMARY, count,
        false, false, false, false, false, false, false, false, false, false, null
    )

    private fun saveSnapshot(root: Parent, file: String) {
        val image = root.snapshot(null, null)
        val buffered = BufferedImage(image.width.toInt(), image.height.toInt(), BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until buffered.height) for (x in 0 until buffered.width) {
            buffered.setRGB(x, y, image.pixelReader.getArgb(x, y))
        }
        ImageIO.write(buffered, "png", Path.of(file).toFile())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun startToolkit() {
            val started = CountDownLatch(1)
            Platform.startup { Platform.setImplicitExit(false); started.countDown() }
            assertTrue(started.await(15, TimeUnit.SECONDS), "JavaFX 初始化超时")
        }

        @JvmStatic
        @AfterAll
        fun stopToolkit() { Platform.exit() }

        private fun <T> onFx(action: () -> T): T {
            val future = FutureTask(action)
            Platform.runLater(future)
            return future.get(20, TimeUnit.SECONDS)
        }
    }
}
