package com.xu.music.player.controller

import com.xu.music.player.constant.Constant
import com.xu.music.player.entity.SongEntity
import com.xu.music.player.player.MediaPlayer
import com.xu.music.player.player.Player
import com.xu.music.player.sql.SQLiteHelper
import com.xu.music.player.tray.MusicPlayerTray
import com.xu.music.player.utils.CommUtils
import com.xu.music.player.window.SongChoose
import com.xu.music.player.wrapper.QueryWrapper
import javafx.fxml.FXML
import javafx.css.PseudoClass
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.canvas.Canvas
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom
import java.util.*
import cn.hutool.core.collection.CollUtil
import cn.hutool.core.io.FileUtil
import cn.hutool.core.util.StrUtil

/**
 * 主窗口
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class MusicPlayerController {

    private lateinit var stage: Stage

    private val log = LoggerFactory.getLogger(MusicPlayerController::class.java)

    /** 定时器，用于刷新 UI 和更新进度 */
    private var timer = Timer(true)

    /** 当前播放歌曲所处的时间位置（秒） */
    private var position = 0.0

    /** 音频播放核心组件 */
    private var player: Player = MediaPlayer()

    /** 歌曲列表 */
    @field:FXML
    private lateinit var lists: TableView<SongEntity>

    /** 歌词列表 */
    @field:FXML
    private lateinit var lyrics: ListView<LyricLine>

    /** 频谱画布 */
    @field:FXML
    private lateinit var spectrumCanvas: Canvas

    /** 进度条组件 */
    @field:FXML
    private lateinit var progress: ProgressBar

    /** 当前播放时间标签 */
    @field:FXML
    private lateinit var timeLabel1: Label

    /** 总播放时间标签 */
    @field:FXML
    private lateinit var timeLabel2: Label

    /** 播放/暂停控制按钮 */
    @field:FXML
    private lateinit var start: ImageView

    /** 已解析歌词行 */
    private val lyricLines = ArrayList<LyricLine>()

    /** 当前高亮歌词 */
    private var currentLyric: LyricLine? = null

    /** 程序化选中时避免递归触发播放 */
    private var syncingSelection = false

    /** 频谱前景色候选 */
    private val spectrumColors = arrayOf(
        "#4169E1", "#DC143C", "#228B22", "#8B4513", "#FF8C00", "#9932CC", "#2E8B57", "#B22222",
        "#4682B4", "#DAA520", "#006400", "#FF69B4", "#8B008B", "#556B2F", "#FF4500", "#191970"
    )

    @field:FXML
    private lateinit var indexColumn: TableColumn<SongEntity, Int>

    @field:FXML
    private lateinit var nameColumn: TableColumn<SongEntity, String>

    @field:FXML
    private lateinit var foot: VBox

    @field:FXML
    private lateinit var prev: ImageView

    @field:FXML
    private lateinit var nextButton: ImageView

    /** FXML 注入完成后绑定动态行为，不在此处访问数据库或弹出窗口。 */
    @FXML
    private fun initialize() {
        indexColumn.cellValueFactory = { SimpleObjectProperty(it.value.index ?: 0) }
        nameColumn.cellValueFactory = { SimpleObjectProperty(it.value.name) }
        lists.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (!syncingSelection && newValue != null) {
                next(lists.selectionModel.selectedIndex.toString(), true)
            }
        }
        val current = PseudoClass.getPseudoClass("current")
        lyrics.cellFactory = { _ ->
            object : ListCell<LyricLine>() {
                override fun updateItem(item: LyricLine?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty) null else item?.text
                    pseudoClassStateChanged(current, !empty && item != null && item == currentLyric)
                }
            }
        }
        spectrumCanvas.widthProperty().bind(foot.widthProperty().subtract(40.0))
    }

    /** 窗口就绪后加载歌曲并启动刷新，保留空列表时自动导入的行为。 */
    fun attach(stage: Stage) {
        this.stage = stage
        initPlayer()
        startSpectrumTimer()
    }

    @FXML
    private fun togglePlayback() {
        if (!player.playing() && !player.pausing()) return
        if (!player.pausing()) {
            start.image = CommUtils.getImage("stop.png")
            player.pause()
        } else {
            start.image = CommUtils.getImage("start.png")
            player.resume(0)
        }
    }

    @FXML
    private fun pressPrevious() {
        prev.image = CommUtils.getImage("lastsong-2.png")
    }

    @FXML
    private fun playPrevious() {
        prev.image = CommUtils.getImage("lastsong-1.png")
        next(null, false)
    }

    @FXML
    private fun pressNext() {
        nextButton.image = CommUtils.getImage("nextsong-2.png")
    }

    @FXML
    private fun playNext() {
        nextButton.image = CommUtils.getImage("nextsong-1.png")
        next(null, true)
    }

    @FXML
    private fun changeSpectrumColor(event: MouseEvent) {
        if (event.clickCount >= 2) {
            Constant.SPECTRUM_FOREGROUND_COLOR = spectrumColors[SecureRandom().nextInt(spectrumColors.size)]
        }
    }

    /**
     * 扫描初始化歌曲信息
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun initPlayer() {
        val wrapper = QueryWrapper<SongEntity>(SongEntity::class.java, "song")
        var list = wrapper.list()

        if (CollUtil.isEmpty(list)) {
            // 当本地没有存放数据时，自动唤起文件选择窗口添加歌曲
            val choice = SongChoose()
            java.awt.Toolkit.getDefaultToolkit().beep()
            choice.open(stage)
            list = wrapper.list()
        }

        if (CollUtil.isEmpty(list)) {
            return
        }

        initSongTable(list)
    }

    /**
     * 打开文件选择窗口添加歌曲，导入后刷新列表
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    @FXML
    private fun addSongs() {
        try {
            val choice = SongChoose()
            if (choice.open(stage)) {
                // 导入成功，刷新歌曲列表
                val wrapper = QueryWrapper<SongEntity>(SongEntity::class.java, "song")
                val list = wrapper.list()
                if (!CollUtil.isEmpty(list)) {
                    initSongTable(list)
                }
            }
        } catch (e: Exception) {
            log.error("添加歌曲失败！", e)
            Alert(Alert.AlertType.ERROR, "添加歌曲失败: ${e.message}").show()
        }
    }

    private fun initSongTable(list: List<SongEntity>) {
        lists.items.clear()
        Constant.PLAYING_LIST.clear()
        list.forEachIndexed { i, entity ->
            Constant.PLAYING_LIST[i] = entity
        }
        lists.items.addAll(list)
    }

    /**
     * 播放指定或上一首/下一首歌曲
     *
     * @param index 歌曲索引
     * @param next   是否下一首
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun next(index: String?, next: Boolean) {
        if (CollUtil.isEmpty(Constant.PLAYING_LIST)) {
            val result = Alert(
                Alert.AlertType.WARNING, "未发现歌曲，现在添加歌曲？", ButtonType.YES, ButtonType.NO
            )
            if (result.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                initPlayer()
            } else {
                Alert(Alert.AlertType.INFORMATION, "未发现歌曲，不能播放歌曲。").show()
                return
            }
        }

        if (StrUtil.isNotBlank(index)) {
            Constant.PLAYING_INDEX = index!!.toInt()
        } else {
            if (null == Constant.PLAYING_INDEX) {
                Constant.PLAYING_INDEX = 0
            } else {
                Constant.PLAYING_INDEX = Constant.PLAYING_INDEX!! + if (next) 1 else -1
            }
            if (Constant.PLAYING_INDEX!! > Constant.PLAYING_LIST.size - 1) {
                Constant.PLAYING_INDEX = 0
            }
            if (Constant.PLAYING_INDEX!! < 0) {
                Constant.PLAYING_INDEX = Constant.PLAYING_LIST.size - 1
            }
        }

        val song = Constant.PLAYING_LIST[Constant.PLAYING_INDEX]
        Constant.PLAYING_SONG = song
        Constant.PLAYING_SONG_LENGTH = song?.length ?: 0.0

        // 播放前检查文件是否存在；不存在则提示删除
        if (song == null || song.songPath.isNullOrBlank() || !Files.exists(Paths.get(song.songPath!!))) {
            handleMissingSong(song)
            return
        }

        try {
            // 注册播放结束自动下一曲
            (player as? MediaPlayer)?.onEndOfMedia = { next(null, true) }
            player.load(song.songPath)
            player.play()
            Constant.MUSIC_PLAYER_PLAYING_STATE = true
        } catch (e: Exception) {
            log.error("选择歌曲播放异常！", e)
        }

        initLyric()
        updateSongListsColor(song)
    }

    /**
     * 处理歌曲文件不存在：弹框确认后从数据库删除该歌曲并刷新列表
     *
     * @param song 文件不存在的歌曲
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun handleMissingSong(song: SongEntity?) {
        if (song == null || song.id.isNullOrBlank()) {
            return
        }
        val name = song.name ?: song.songPath ?: "未知歌曲"
        val result = Alert(
            Alert.AlertType.WARNING,
            "歌曲文件不存在: $name，是否从列表删除该歌曲？",
            ButtonType.YES, ButtonType.NO
        ).showAndWait().orElse(ButtonType.NO)

        if (result != ButtonType.YES) {
            return
        }

        try {
            val sqliteHelper = SQLiteHelper()
            sqliteHelper.delete("delete from song where id = ?", song.id)
            log.info("已删除文件不存在的歌曲: {} (id={})", name, song.id)

            // 从内存列表和界面移除
            val removedIndex = Constant.PLAYING_INDEX
            Constant.PLAYING_LIST.values.remove(song)
            lists.items.remove(song)

            // 若删除的是当前播放歌曲，停止播放
            if (Constant.PLAYING_SONG == song) {
                player.stop()
                Constant.PLAYING_SONG = null
            }

            // 修正播放索引：删除索引前的项索引不变，删除后的项前移一位
            if (!Constant.PLAYING_LIST.isEmpty()) {
                if (removedIndex != null) {
                    if (removedIndex >= Constant.PLAYING_LIST.size) {
                        Constant.PLAYING_INDEX = Constant.PLAYING_LIST.size - 1
                    }
                }
            } else {
                // 列表为空则清空播放状态
                Constant.PLAYING_SONG = null
                Constant.PLAYING_INDEX = null
                start.image = CommUtils.getImage("stop.png")
                timeLabel1.text = "00:00"
                timeLabel2.text = "00:00"
            }
        } catch (e: Exception) {
            log.error("删除歌曲失败！", e)
            Alert(Alert.AlertType.ERROR, "删除歌曲失败: ${e.message}").show()
        }
    }

    private fun updateSongListsColor(entity: SongEntity) {
        start.image = CommUtils.getImage("start.png")
        // 优先使用播放器真实时长，未知时回退到数据库存储的时长
        val realDuration = player.duration()
        timeLabel2.text = if (realDuration > 0) CommUtils.format(realDuration.toInt())
        else CommUtils.format(entity.length?.toInt() ?: 0)

        // 高亮当前播放歌曲（程序化选中，避免递归触发播放）
        syncingSelection = true
        try {
            Constant.PLAYING_INDEX?.let { index ->
                lists.selectionModel.select(index)
                lists.scrollTo(index)
            }
        } finally {
            syncingSelection = false
        }
    }

    /**
     * 解析 LRC 时间
     *
     * @param timeStr 时间字符串
     * @return 秒数，解析失败返回 -1
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun parseLrcTime(timeStr: String): Double {
        return try {
            val t = timeStr.replace("[", "").replace("]", "").trim()
            val parts = t.split(":")
            if (parts.size < 2) {
                return -1.0
            }
            parts[0].toDouble() * 60 + parts[1].toDouble()
        } catch (e: Exception) {
            -1.0
        }
    }

    /**
     * 更新歌词高亮
     *
     * @param currentPosition 当前播放位置（秒）
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun updateLyric(currentPosition: Double) {
        if (!Constant.PLAYING_LYRIC) {
            return
        }
        if (lyricLines.isEmpty()) {
            return
        }

        var highlightIndex = -1
        var maxTime = -1.0

        // 寻找小于等于当前播放进度的最大歌词时间戳
        for (i in lyricLines.indices) {
            val t = lyricLines[i].time
            if (t >= 0 && t <= currentPosition) {
                if (t > maxTime) {
                    maxTime = t
                    highlightIndex = i
                }
            }
        }

        // 高亮当前行，清除其它行高亮
        val newCurrent = if (highlightIndex == -1) null else lyricLines[highlightIndex]
        if (newCurrent != currentLyric) {
            currentLyric = newCurrent
            lyrics.refresh()
        }

        // 自动滚动，将当前歌词行置于视口偏上
        if (highlightIndex != -1) {
            lyrics.scrollTo(highlightIndex)
        }
    }

    /**
     * 初始化歌词
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun initLyric() {
        val song = Constant.PLAYING_SONG ?: return
        if (StrUtil.isBlank(song.lyricPath)) {
            return
        }

        Constant.PLAYING_LYRIC = false
        val path = Paths.get(song.lyricPath!!)
        if (!Files.exists(path)) {
            return
        }

        Constant.PLAYING_LYRIC = true
        lyricLines.clear()
        lyrics.items.clear()
        val lyric = FileUtil.readUtf8Lines(path.toFile())
        for (s in lyric) {
            val parts = s.split("(?<=\\])".toRegex(), limit = 2)
            if (parts.size < 2) {
                continue
            }

            val lyricTime = parseLrcTime(parts[0])
            lyricLines.add(LyricLine(lyricTime, parts[1]))
        }
        lyrics.items.addAll(lyricLines)
        currentLyric = null
        lyrics.refresh()
    }

    /**
     * 启动频谱刷新定时器
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun startSpectrumTimer() {
        timer.cancel()
        position = 0.0
        timer = Timer(true)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Platform.runLater {
                    // 使用 MediaPlayer 的真实播放位置
                    position = player.position()
                    // 频谱面板
                    redrawSpectrum()
                    // 歌词
                    updateLyric(position)
                    // 进度条：使用播放器真实总时长
                    val total = player.duration()
                    if (total > 0) {
                        progress.progress = (position / total).coerceIn(0.0, 1.0)
                        // 总时长就绪后同步更新总时间标签
                        timeLabel2.text = CommUtils.format(total.toInt())
                    }
                    // 实时播放时间
                    timeLabel1.text = CommUtils.format(position.toInt())
                }
            }
        }, 0, 100)
    }

    /**
     * 绘制频谱柱状图
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    private fun redrawSpectrum() {
        if (!::spectrumCanvas.isInitialized || !::progress.isInitialized) {
            return
        }
        val gc = spectrumCanvas.graphicsContext2D
        val canvasWidth = spectrumCanvas.width
        val canvasHeight = spectrumCanvas.height
        gc.clearRect(0.0, 0.0, canvasWidth, canvasHeight)

        if (!player.playing() || player.pausing()) {
            return
        }

        val length = (canvasWidth / 25.0).toInt()

        if (CollUtil.isEmpty(MediaPlayer.TRANS)) {
            return
        }

        val transSnapshot = MediaPlayer.TRANS.toTypedArray()
        if (transSnapshot.size < 2) {
            return
        }

        // AudioSpectrumListener 已给出真实频段幅值(dB)，无需再减半
        val validDataLen = transSnapshot.size
        if (length <= 0) {
            return
        }

        // 对数域频率映射边界
        val minFreqBin = 1.0
        val maxFreqBin = validDataLen - 1.0

        gc.fill = Color.web(Constant.SPECTRUM_FOREGROUND_COLOR)

        for (i in 0 until length) {
            // 采用对数分布算法将频率等比例压进柱形条中（偏重于低音频段，符合人耳听觉）
            val ratioStart = i.toDouble() / length
            val ratioEnd = (i + 1).toDouble() / length

            var binStart = (minFreqBin * Math.pow(maxFreqBin / minFreqBin, ratioStart)).toInt()
            var binEnd = (minFreqBin * Math.pow(maxFreqBin / minFreqBin, ratioEnd)).toInt()

            if (binEnd <= binStart) {
                binEnd = binStart + 1
            }
            if (binEnd > validDataLen) {
                binEnd = validDataLen
            }

            var sum = 0.0
            var count = 0
            for (b in binStart until binEnd) {
                if (b < validDataLen) {
                    val obj = transSnapshot[b]
                    if (obj != null) {
                        sum += obj
                        count++
                    }
                }
            }

            // dB(-80~0) 归一化到 0~1：加 80 再除 80
            val avgDb = if (count > 0) sum / count else -80.0
            val normalized = ((avgDb + 80.0) / 80.0).coerceIn(0.0, 1.0)

            // 平方根增强低音量可见性，再映射到画布高度
            var barHeight = (Math.sqrt(normalized) * canvasHeight).toInt()
            if (barHeight > canvasHeight) {
                barHeight = canvasHeight.toInt()
            }

            gc.fillRect(i * 26.0 + 1, canvasHeight - barHeight, 22.0, barHeight.toDouble())
        }
    }

    /**
     * 释放定时器、托盘和播放资源，由应用入口统一管理窗口退出
     *
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    fun dispose() {
        timer.cancel()
        MusicPlayerTray.dispose()
        player.stop()
    }

    /**
     * 歌词行
     *
     * @param time 时间（秒）
     * @param text 歌词文本
     * @date 2024年6月4日19点07分
     * @since SWT-V1.0.0.0
     */
    data class LyricLine(val time: Double, val text: String)
}
