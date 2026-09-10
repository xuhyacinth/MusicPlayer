package com.xu.music.player.controller

import com.xu.music.player.constant.Constant
import com.xu.music.player.entity.SongEntity
import com.xu.music.player.player.MediaPlayer
import com.xu.music.player.player.Player
import com.xu.music.player.sql.SQLiteHelper
import com.xu.music.player.taskbar.TaskbarLyrics
import com.xu.music.player.tray.MusicPlayerTray
import com.xu.music.player.utils.CommUtils
import com.xu.music.player.window.SongChoose
import com.xu.music.player.wrapper.QueryWrapper
import javafx.fxml.FXML
import javafx.css.PseudoClass
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.skin.ListViewSkin
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.shape.SVGPath
import javafx.stage.Popup
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

    /** 首尾留白只参与显示，不参与时间匹配。 */
    private val lyricSpacer = LyricLine(-1.0, "")
    private lateinit var lyricSkin: CenteredLyricSkin

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
    private lateinit var foot: StackPane

    @field:FXML
    private lateinit var prev: ImageView

    @field:FXML
    private lateinit var nextButton: ImageView

    private var allSongs: List<SongEntity> = emptyList()
    private var requestedSong: SongEntity? = null
    private var requestId = 0L
    private var disposed = false

    @field:FXML
    private lateinit var playbackStatus: Label

    @field:FXML
    private lateinit var songSearch: TextField

    @field:FXML
    private lateinit var clearSearch: Button

    @field:FXML
    private lateinit var playlistPlaceholder: Label

    @field:FXML
    private lateinit var progressTrack: StackPane

    @field:FXML
    private lateinit var volumeButton: Button

    @field:FXML
    private lateinit var volumePopup: Popup

    @field:FXML
    private lateinit var volumeSlider: Slider

    @field:FXML
    private lateinit var volumeLabel: Label

    @field:FXML
    private lateinit var volumeTooltip: Tooltip

    @field:FXML
    private lateinit var volumeIcon: SVGPath

    /** FXML 注入完成后绑定动态行为，不在此处访问数据库或弹出窗口。 */
    @FXML
    private fun initialize() {
        indexColumn.cellValueFactory = { SimpleObjectProperty(it.value.index ?: 0) }
        nameColumn.cellValueFactory = { SimpleObjectProperty(it.value.name) }
        lists.setRowFactory {
            TableRow<SongEntity>().apply {
                setOnMouseClicked { event ->
                    if (!isEmpty && event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                        next(index.toString(), true)
                        event.consume()
                    }
                }
            }
        }
        lists.setOnKeyPressed { event ->
            if (event.code == KeyCode.ENTER && lists.selectionModel.selectedItem != null) {
                next(lists.selectionModel.selectedIndex.toString(), true)
                event.consume()
            }
        }
        val current = PseudoClass.getPseudoClass("current")
        lyrics.cellFactory = { _ ->
            object : ListCell<LyricLine>() {
                override fun updateItem(item: LyricLine?, empty: Boolean) {
                    super.updateItem(item, empty)
                    val spacer = item === lyricSpacer
                    text = if (empty || spacer) null else item?.text
                    isMouseTransparent = spacer
                    pseudoClassStateChanged(PseudoClass.getPseudoClass("spacer"), spacer)
                    pseudoClassStateChanged(current, !empty && !spacer && item != null && item == currentLyric)
                }

            }
        }
        lyricSkin = CenteredLyricSkin()
        lyrics.skin = lyricSkin
        lists.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        songSearch.textProperty().addListener { _, _, _ -> applyCurrentSearch() }
        songSearch.setOnKeyPressed { event ->
            if (event.code == KeyCode.ESCAPE) {
                clearSearch()
                event.consume()
            }
        }
        clearSearch.visibleProperty().bind(songSearch.textProperty().isNotEmpty)
        clearSearch.managedProperty().bind(clearSearch.visibleProperty())
        volumeSlider.valueProperty().addListener { _, _, value ->
            val percentage = value.toDouble().toInt()
            player.volume((value.toDouble() / 100.0).toFloat())
            volumeLabel.text = "$percentage%"
            volumeTooltip.text = "音量 $percentage%，点击调节"
            volumeIcon.content = if (percentage == 0) {
                "M 2 8 L 7 8 L 13 3 L 13 21 L 7 16 L 2 16 Z M 17 8 L 24 16 M 24 8 L 17 16"
            } else {
                "M 2 8 L 7 8 L 13 3 L 13 21 L 7 16 L 2 16 Z M 16 8 Q 20 12 16 16 M 19 4 Q 27 12 19 20"
            }
        }
        Tooltip.install(progressTrack, Tooltip("点击调整播放进度"))
        spectrumCanvas.widthProperty().bind(foot.widthProperty())
        spectrumCanvas.heightProperty().bind(foot.heightProperty())
        spectrumCanvas.widthProperty().addListener { _, _, _ -> redrawSpectrum() }
        spectrumCanvas.heightProperty().addListener { _, _, _ -> redrawSpectrum() }
    }

    /** 窗口就绪后加载歌曲并启动刷新，保留空列表时自动导入的行为。 */
    fun attach(stage: Stage) {
        this.stage = stage
        initPlayer()
        startSpectrumTimer()
    }

    @FXML
    private fun clearSearch() {
        songSearch.clear()
        songSearch.requestFocus()
    }

    @FXML
    private fun toggleVolume() {
        if (volumePopup.isShowing) {
            volumePopup.hide()
            return
        }
        val bounds = volumeButton.localToScreen(volumeButton.boundsInLocal) ?: return
        volumePopup.show(volumeButton, bounds.centerX - 36.0, bounds.minY - 190.0)
    }

    @FXML
    private fun seekPlayback(event: MouseEvent) {
        if (event.button != MouseButton.PRIMARY) return
        event.consume()
        if ((!player.playing() && !player.pausing()) || progressTrack.width <= 0.0) return
        val total = player.duration()
        if (!total.isFinite() || total <= 0.0) return
        val fraction = (event.x / progressTrack.width).coerceIn(0.0, 1.0)
        position = total * fraction
        player.seek(position)
        progress.progress = fraction
        timeLabel1.text = CommUtils.format(position.toInt())
        timeLabel2.text = CommUtils.format(total.toInt())
        updateLyric(position)
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
        allSongs = list.toList()
        applyCurrentSearch()
    }

    /** 搜索只更新可见播放队列，不中断当前歌曲，也不触发选中播放。 */
    private fun applyCurrentSearch() {
        val keyword = songSearch.text.trim()
        val visible = allSongs.filter {
            keyword.isEmpty() || it.name.orEmpty().contains(keyword, ignoreCase = true) ||
                it.author.orEmpty().contains(keyword, ignoreCase = true)
        }
        lists.items.setAll(visible)
        Constant.PLAYING_LIST.clear()
        visible.forEachIndexed { index, song -> Constant.PLAYING_LIST[index] = song }
        Constant.PLAYING_INDEX = visible.indexOfFirst { it.id == Constant.PLAYING_SONG?.id }
            .takeIf { it >= 0 }
        lists.selectionModel.clearSelection()
        Constant.PLAYING_INDEX?.let { lists.selectionModel.select(it) }
        playlistPlaceholder.text = if (keyword.isEmpty()) "暂无歌曲，请点击添加" else "没有匹配的歌曲"
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
        if (disposed) return
        if (Constant.PLAYING_LIST.isEmpty() && allSongs.isNotEmpty()) return
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

        if (Constant.PLAYING_LIST.isEmpty()) return

        val currentIndex = lists.items.indexOfFirst { it.id == (requestedSong ?: Constant.PLAYING_SONG)?.id }
        val targetIndex = index?.toIntOrNull() ?: if (currentIndex < 0) 0
            else Math.floorMod(currentIndex + if (next) 1 else -1, lists.items.size)
        val song = Constant.PLAYING_LIST[targetIndex] ?: return
        val id = ++requestId
        requestedSong = song
        player.stop()
        Constant.PLAYING_SONG = null
        Constant.PLAYING_INDEX = null
        Constant.PLAYING_SONG_LENGTH = 0.0
        Constant.MUSIC_PLAYER_PLAYING_STATE = false
        Constant.PLAYING_LYRIC = false
        lyricLines.clear()
        lyrics.items.clear()
        currentLyric = null
        TaskbarLyrics.update(Constant.PLAYING_SONG, null)
        progress.progress = 0.0
        timeLabel1.text = "00:00"
        timeLabel2.text = "00:00"
        start.image = CommUtils.getImage("stop.png")

        if (song.songPath.isNullOrBlank() || !Files.exists(Paths.get(song.songPath!!))) {
            requestedSong = null
            playbackStatus.text = "歌曲文件不存在"
            handleMissingSong(song)
            return
        }

        playbackStatus.tooltip = null
        playbackStatus.text = "正在加载：${song.name ?: "未知歌曲"}"
        (player as? MediaPlayer)?.onEndOfMedia = { next(null, true) }
        player.loadAsync(song.songPath!!, onLoaded = {
            if (!disposed && id == requestId) {
                requestedSong = null
                Constant.PLAYING_SONG = song
                Constant.PLAYING_SONG_LENGTH = song.length ?: 0.0
                Constant.PLAYING_INDEX = lists.items.indexOfFirst { it.id == song.id }.takeIf { it >= 0 }
                player.play()
                Constant.MUSIC_PLAYER_PLAYING_STATE = true
                playbackStatus.text = ""
                initLyric()
                updateSongListsColor(song)
            }
        }, onError = { error ->
            if (!disposed && id == requestId) {
                requestedSong = null
                Constant.PLAYING_SONG = null
                Constant.PLAYING_INDEX = null
                Constant.PLAYING_SONG_LENGTH = 0.0
                Constant.MUSIC_PLAYER_PLAYING_STATE = false
                Constant.PLAYING_LYRIC = false
                lyricLines.clear()
                lyrics.items.clear()
                currentLyric = null
                TaskbarLyrics.update(Constant.PLAYING_SONG, null)
                progress.progress = 0.0
                timeLabel1.text = "00:00"
                timeLabel2.text = "00:00"
                start.image = CommUtils.getImage("stop.png")
                playbackStatus.text = "播放失败：${song.name ?: "未知歌曲"}"
                playbackStatus.tooltip = Tooltip(error.message ?: "无法加载音频")
                log.error("[PLAYBACK-LOAD] 歌曲加载失败: {}", song.songPath, error)
            }
        })
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

            if (Constant.PLAYING_SONG == song) {
                player.stop()
                Constant.PLAYING_SONG = null
                Constant.PLAYING_LYRIC = false
                Constant.MUSIC_PLAYER_PLAYING_STATE = false
                lyricLines.clear()
                lyrics.items.clear()
                currentLyric = null
                TaskbarLyrics.update(Constant.PLAYING_SONG, null)
                progress.progress = 0.0
                start.image = CommUtils.getImage("stop.png")
                timeLabel1.text = "00:00"
                timeLabel2.text = "00:00"
            }
            allSongs = allSongs.filterNot { it.id == song.id }
            applyCurrentSearch()
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

        // 高亮当前播放歌曲；单纯选中不再触发播放。
        Constant.PLAYING_INDEX?.let { index ->
            lists.selectionModel.select(index)
            lists.scrollTo(index)
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
        if (!Constant.PLAYING_LYRIC || lyricLines.isEmpty()) {
            TaskbarLyrics.update(Constant.PLAYING_SONG, null)
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

        TaskbarLyrics.update(Constant.PLAYING_SONG, currentLyric?.text)
        if (highlightIndex != -1) {
            lyricSkin.centerOn(highlightIndex)
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
        lyricSkin.resetLines()
        currentLyric = null
        TaskbarLyrics.update(Constant.PLAYING_SONG, null)
        lyrics.refresh()
    }

    /** 按实际行高居中，首尾补足空白行，使第一句和最后一句也能居中。 */
    private inner class CenteredLyricSkin : ListViewSkin<LyricLine>(lyrics) {
        private var targetIndex = 0
        private var spacerCount = 0
        private var centerPending = true
        private var previousHeight = -1.0
        private var previousWidth = -1.0

        fun resetLines() {
            spacerCount = 0
            lyrics.items.setAll(lyricLines)
            targetIndex = -1
            centerOn(0)
        }

        fun centerOn(index: Int) {
            // 同一句的播放进度刷新不重复滚动，避免像素取整导致上下振荡。
            if (targetIndex == index) return
            targetIndex = index
            centerPending = true
            lyrics.requestLayout()
        }

        override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
            super.layoutChildren(x, y, w, h)
            if (lyricLines.isEmpty() || targetIndex !in lyricLines.indices) return
            val resized = h != previousHeight || w != previousWidth
            if (!centerPending && !resized) return
            previousHeight = h
            previousWidth = w
            centerPending = false
            val flow = virtualFlow
            flow.layout()
            if (lyrics.fixedCellSize <= 0.0) {
                val cell = flow.getCell(spacerCount)
                cell.applyCss()
                lyrics.fixedCellSize = snapSizeY(cell.prefHeight(-1.0))
            }
            val rowHeight = lyrics.fixedCellSize
            val padding = kotlin.math.ceil(flow.viewportLength / (2.0 * rowHeight)).toInt()
            if (padding != spacerCount) {
                spacerCount = padding
                lyrics.items.setAll(List(padding) { lyricSpacer } + lyricLines + List(padding) { lyricSpacer })
                super.layoutChildren(x, y, w, h)
                flow.layout()
            }
            val index = targetIndex + spacerCount
            flow.scrollTo(index)
            flow.layout()
            val current = flow.getVisibleCell(index) ?: return
            val targetY = snapPositionY((flow.viewportLength - current.height) / 2.0)
            val offset = current.layoutY - targetY
            if (kotlin.math.abs(offset) > 0.1) flow.scrollPixels(offset)
            // 大跨度跳转会复用缓存行，同步可见行的高亮状态。
            val first = flow.firstVisibleCell?.index ?: return
            val last = flow.lastVisibleCell?.index ?: return
            val active = PseudoClass.getPseudoClass("current")
            for (visibleIndex in first..last) {
                val cell = flow.getVisibleCell(visibleIndex) ?: continue
                cell.pseudoClassStateChanged(active, !cell.isEmpty && cell.item !== lyricSpacer && cell.item != null && cell.item == currentLyric)
            }
        }
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
                    if (disposed) return@runLater
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
        disposed = true
        requestId++
        requestedSong = null
        timer.cancel()
        volumePopup.hide()
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
