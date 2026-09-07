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
import javafx.scene.layout.VBox
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
            val footer = loader.namespace["foot"] as VBox
            assertEquals(Color.web("#f8f8f8"), footer.background.fills.first().fill)
            val canvas = loader.namespace["spectrumCanvas"] as Canvas
            assertEquals(footer.width - 40.0, canvas.width, 0.01)
            footer.resize(1100.0, footer.height)
            assertEquals(1060.0, canvas.width, 0.01)
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
        InsertWrapper(SongEntity(id = "one", name = "测试歌曲一", index = 1,
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
                val fake = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, args ->
                    when (method.name) {
                        "load" -> { loaded = args[0] as String; null }
                        "play" -> { active = true; null }
                        "pause" -> { active = false; paused = true; null }
                        "resume" -> { active = true; paused = false; null }
                        "stop" -> { active = false; stopped = true; null }
                        "playing" -> active
                        "pausing" -> paused
                        "position" -> 5.0
                        "duration" -> 120.0
                        else -> null
                    }
                } as Player
                MusicPlayerController::class.java.getDeclaredField("player").apply { isAccessible = true }.set(controller, fake)
                try {
                    controller.attach(stage)
                    @Suppress("UNCHECKED_CAST")
                    val table = loader.namespace["lists"] as TableView<SongEntity>
                    assertEquals(2, table.items.size)
                    table.selectionModel.select(0)
                    assertEquals(firstFile.toString(), loaded)
                    assertTrue(active)
                    val lyrics = loader.namespace["lyrics"] as ListView<*>
                    assertEquals(2, lyrics.items.size)
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
                    (loader.namespace["foot"] as VBox).fireEvent(mouse(MouseEvent.MOUSE_CLICKED, 2))
                    assertNotEquals("#000000", Constant.SPECTRUM_FOREGROUND_COLOR)
                    root.applyCss()
                    root.layout()
                    saveSnapshot(root, "fxml-preview.png")
                } finally {
                    controller.dispose()
                    assertTrue(stopped)
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
