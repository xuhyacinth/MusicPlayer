package com.xu.music.player.main

import cn.hutool.core.collection.CollUtil
import cn.hutool.core.io.FileUtil
import cn.hutool.core.util.StrUtil
import com.xu.music.player.constant.Constant
import com.xu.music.player.entity.SongEntity
import com.xu.music.player.player.Player
import com.xu.music.player.player.SdlFftPlayer
import com.xu.music.player.player.SdlFftPlayer.Companion.create
import com.xu.music.player.tray.MusicPlayerTray
import com.xu.music.player.utils.Utils.draw
import com.xu.music.player.utils.Utils.format
import com.xu.music.player.utils.Utils.getColor
import com.xu.music.player.utils.Utils.getFont
import com.xu.music.player.utils.Utils.getImage
import com.xu.music.player.utils.Utils.tips
import com.xu.music.player.window.SongChoose
import com.xu.music.player.wrapper.QueryWrapper
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.ControlAdapter
import org.eclipse.swt.events.ControlEvent
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.MouseAdapter
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.MouseTrackAdapter
import org.eclipse.swt.events.PaintEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.ProgressBar
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableColumn
import org.eclipse.swt.widgets.TableItem
import org.eclipse.swt.widgets.Tray
import org.slf4j.LoggerFactory
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom
import java.util.*
import java.util.stream.IntStream
import kotlin.math.abs

/**
 * 主页面
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class MusicPlayer {

    val log = LoggerFactory.getLogger("MusicPlayer")

    private val spectrum: MutableList<Int> = LinkedList()

    private var timer = Timer(true)

    private var position = 0.0

    protected var shell: Shell? = null

    // 播放器
    private var player: Player? = null
    private var display: Display? = null

    // 播放器托盘
    private var tray: Tray? = null
    private var lists: Table? = null

    // 歌词
    private var lyrics: Table? = null

    // 频谱面板
    private var foot: Composite? = null

    // 进度条
    private var progress: ProgressBar? = null
    private var timeLabel1: Label? = null

    // 界面移动
    private var clickX = 0
    private var clickY = 0
    private var timeLabel2: Label? = null

    // 双击播放
    private var chose = true
    private var start: Label? = null

    // 界面移动
    private var click = false

    fun open() {
        display = Display.getDefault()
        createContents()
        shell!!.open()
        shell!!.layout()
        while (!shell!!.isDisposed) {
            if (!display!!.readAndDispatch()) {
                display!!.sleep()
            }
        }
    }

    /**
     * Create contents of the window.
     */
    protected fun createContents() {
        shell = Shell(SWT.NONE)
        shell!!.image = getImage("main.png")
        shell!!.size = Point(1000, 645)
        shell!!.setSize(900, 486)
        shell!!.text = "MusicPlayer"
        //        shell.setLocation((display.getClientArea().width - shell.getSize().x) / 2,
//                (display.getClientArea().height - shell.getSize().y) / 2);
        shell!!.layout = FillLayout(SWT.HORIZONTAL)
        shell!!.backgroundMode = SWT.INHERIT_DEFAULT

        // 初始化播放器
        player = create()

        // 托盘引入
        tray = display!!.systemTray
        val trayutil = MusicPlayerTray(shell!!, tray)
        trayutil.tray()

        val composite = Composite(shell, SWT.NONE)
        composite.backgroundMode = SWT.INHERIT_FORCE
        composite.layout = FillLayout(SWT.HORIZONTAL)

        val sashForm = SashForm(composite, SWT.VERTICAL)

        val top = Composite(sashForm, SWT.NONE)
        top.backgroundMode = SWT.INHERIT_FORCE

        val exit = Label(top, SWT.NONE)
        exit.image = getImage("exit-1.png")
        exit.setBounds(845, 10, 32, 32)

        val mini = Label(top, SWT.NONE)
        mini.image = getImage("mini-1.png")
        mini.setBounds(798, 10, 32, 32)

        val combo = Combo(top, SWT.NONE)
        combo.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                combo.clearSelection()
            }
        })
        combo.addModifyListener { arg0: ModifyEvent? -> }
        combo.setBounds(283, 21, 330, 25)
        combo.isVisible = false

        val center = Composite(sashForm, SWT.NONE)
        center.backgroundMode = SWT.INHERIT_FORCE
        center.layout = FillLayout(SWT.HORIZONTAL)

        val sashForm1 = SashForm(center, SWT.NONE)

        val composite1 = Composite(sashForm1, SWT.NONE)
        composite1.backgroundMode = SWT.INHERIT_FORCE
        composite1.layout = FillLayout(SWT.HORIZONTAL)

        lists = Table(composite1, SWT.FULL_SELECTION)
        lists!!.headerVisible = true

        val tableColumn = TableColumn(lists, SWT.NONE)
        tableColumn.width = 41
        tableColumn.text = "序号"

        val song = TableColumn(lists, SWT.NONE)
        song.width = 117
        song.text = "歌曲"

        val composite2 = Composite(sashForm1, SWT.NONE)
        composite2.backgroundMode = SWT.INHERIT_FORCE
        composite2.layout = FillLayout(SWT.HORIZONTAL)

        lyrics = Table(composite2, SWT.NONE)

        val lyric1 = TableColumn(lyrics, SWT.CENTER)
        lyric1.text = "歌词"

        val lyric2 = TableColumn(lyrics, SWT.CENTER)
        lyric2.width = 738
        lyric2.text = "歌词"

        foot = Composite(sashForm, SWT.NONE)
        foot!!.backgroundMode = SWT.INHERIT_FORCE

        val prev = Label(foot, SWT.NONE)
        prev.image = getImage("lastsong-1.png")
        prev.setBounds(33, 18, 32, 32)

        val next = Label(foot, SWT.NONE)
        next.image = getImage("nextsong-1.png")
        next.setBounds(165, 18, 32, 32)

        start = Label(foot, SWT.NONE)
        start!!.addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: MouseEvent) {
                if (!player!!.playing()) {
                    return
                }

                if (player!!.pausing()) {
                    start!!.image = getImage("start.png")
                    player!!.pause()
                } else {
                    start!!.image = getImage("stop.png")
                    player!!.resume(0)
                }
            }
        })
        start!!.image = getImage("stop.png")
        start!!.setBounds(98, 18, 32, 32)

        progress = ProgressBar(foot, SWT.NONE)
        progress!!.isEnabled = false
        progress!!.setBounds(238, 25, 610, 17)
        // 设置进度条的最大长度
        progress!!.maximum = 100
        progress!!.selection = 0
        // 设置进度的条最小程度
        progress!!.minimum = 0

        timeLabel1 = Label(foot, SWT.NONE)
        timeLabel1!!.font = getFont("Consolas", 9, SWT.NORMAL)
        timeLabel1!!.isEnabled = false
        timeLabel1!!.setBounds(238, 4, 73, 20)

        timeLabel2 = Label(foot, SWT.RIGHT)
        timeLabel2!!.font = getFont("Consolas", 9, SWT.NORMAL)
        timeLabel2!!.isEnabled = false
        timeLabel2!!.setBounds(775, 4, 73, 20)

        sashForm.setWeights(1, 5, 1)
        sashForm1.setWeights(156, 728)

        // 界面移动
        top.addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: MouseEvent) {
                click = true
                clickX = e.x
                clickY = e.y
            }

            override fun mouseUp(e: MouseEvent) {
                click = false
            }
        })
        top.addMouseMoveListener { arg0: MouseEvent ->
            if (click) {
                shell!!.setLocation(shell!!.location.x - clickX + arg0.x, shell!!.location.y - clickY + arg0.y)
            }
        }

        // 缩小
        mini.addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: MouseEvent) {
                mini.image = getImage("mini-2.png")
            }

            override fun mouseUp(e: MouseEvent) {
                mini.image = getImage("mini-1.png")
                shell!!.minimized = true
            }
        })

        mini.addMouseTrackListener(object : MouseTrackAdapter() {
            override fun mouseExit(e: MouseEvent) {
                mini.image = getImage("mini-1.png")
            }

            override fun mouseHover(e: MouseEvent) {
                mini.image = getImage("mini-2.png")
                mini.toolTipText = "最小化"
            }
        })

        // 退出
        exit.addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: MouseEvent) {
                exit.image = getImage("exit-2.png")
            }

            override fun mouseUp(e: MouseEvent) {
                exit.image = getImage("exit-1.png")
                exit()
            }
        })
        exit.addMouseTrackListener(object : MouseTrackAdapter() {
            override fun mouseExit(e: MouseEvent) {
                exit.image = getImage("exit-1.png")
            }

            override fun mouseHover(e: MouseEvent) {
                exit.image = getImage("exit-2.png")
                exit.toolTipText = "退出"
            }
        })

        // 双击播放
        lists!!.addMouseListener(object : MouseAdapter() {
            override fun mouseDoubleClick(e: MouseEvent) {
                chose = true
            }
        })
        lists!!.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (chose) {
                    val items = lists!!.selection
                    val id = items[0].getText(0).trim { it <= ' ' }
                    // 下一曲
                    next(id, true)
                }
            }
        })

        // 上一曲
        prev.addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: MouseEvent) {
                prev.image = getImage("lastsong-2.png")
            }

            override fun mouseUp(e: MouseEvent) {
                next(null, false) // 上一曲
                prev.image = getImage("lastsong-1.png")
            }
        })

        // 下一曲
        next.addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: MouseEvent) {
                next.image = getImage("nextsong-2.png")
            }

            override fun mouseUp(e: MouseEvent) {
                next(null, true) // 下一曲
                next.image = getImage("nextsong-1.png")
            }
        })

        foot!!.addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                sashForm.setWeights(1, 5, 1)
                sashForm1.setWeights(156, 728)
            }
        })

        foot!!.addMouseListener(object : MouseAdapter() {
            override fun mouseDoubleClick(e: MouseEvent) {
                Constant.SPECTRUM_FOREGROUND_COLOR = Constant.COLORS[SecureRandom().nextInt(Constant.COLORS.size)]
            }
        })

        // 添加绘图监听器
        foot!!.addPaintListener { listener: PaintEvent ->
            if (!player!!.playing() && player!!.pausing()) {
                return@addPaintListener
            }
            val gc = listener.gc

            val width = listener.width
            val height = listener.height
            val length = width / 25
            if (spectrum.size >= length) {
                for (i in 0 until length) {
                    draw(gc, i * 26, height, 26, spectrum[i])
                }
            }
        }

        sashForm.addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                sashForm.setWeights(1, 5, 1)
                sashForm1.setWeights(156, 728)
            }
        })

        composite1.addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                sashForm.setWeights(1, 5, 1)
                sashForm1.setWeights(156, 728)
            }
        })

        composite2.addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                sashForm.setWeights(1, 5, 1)
                sashForm1.setWeights(156, 728)
            }
        })

        sashForm1.addControlListener(object : ControlAdapter() {
            override fun controlResized(e: ControlEvent) {
                sashForm.setWeights(1, 5, 1)
                sashForm1.setWeights(156, 728)
            }
        })

        initPlayer(shell, lists)
    }

    fun initPlayer(shell: Shell?, table: Table?) {
        val wrapper = QueryWrapper(SongEntity::class.java, "song")
        var list = wrapper.list()

        if (CollUtil.isEmpty(list)) {
            val choice = SongChoose()
            Toolkit.getDefaultToolkit().beep()
            choice.open(shell)
            list = wrapper.list()
        }

        if (CollUtil.isEmpty(list)) {
            return
        }

        initSongTable(list, table)
    }

    private fun initSongTable(list: List<SongEntity?>, table: Table?) {
        table!!.removeAll()
        var item: TableItem
        IntStream.range(0, list.size).forEach { i: Int -> list[i]?.let { Constant.PLAYING_LIST[i] = it } }

        var index = 0
        for (entity in list) {
            item = TableItem(table, SWT.NONE)
            item.setText(arrayOf(index.toString(), entity!!.name))
            index++
        }
    }

    private fun next(index: String?, next: Boolean) {
        if (CollUtil.isEmpty(Constant.PLAYING_LIST)) {
            val msg = tips(shell, null, "未发现歌曲，现在添加歌曲？")
            if (msg.open() == SWT.YES) {
                initPlayer(shell, lists)
            } else {
                tips(shell, null, "未发现歌曲，不能播放歌曲。").open()
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
                Constant.PLAYING_INDEX = Constant.PLAYING_LIST.size + 1
            }
        }

        Constant.PLAYING_SONG = Constant.PLAYING_LIST[Constant.PLAYING_INDEX]
        Constant.PLAYING_SONG_LENGTH = Constant.PLAYING_SONG!!.length!!
        try {
            player!!.load(Constant.PLAYING_SONG!!.songPath)
            player!!.play()
            Constant.MUSIC_PLAYER_PLAYING_STATE = true
        } catch (e: Exception) {
            log.error("选择歌曲播放异常！", e)
        }

        initLyric()
        spectrum(foot, timeLabel2)
        updateSongListsColor(lists, Constant.PLAYING_SONG)
    }

    private fun updateSongListsColor(table: Table?, entity: SongEntity?) {
        start!!.image = getImage("start.png")
        timeLabel1!!.text = entity!!.length?.let { format(it.toInt()) }

        val items = table!!.items
        var i = 0
        val len = items.size
        while (i < len) {
            if (i == Constant.PLAYING_INDEX) {
                items[i].background = getColor(SWT.COLOR_GRAY)
            } else {
                items[i].background = getColor(SWT.COLOR_WHITE)
            }
            i++
        }

        if (entity.index!! <= 7) {
            table.topIndex = entity.index!!
        } else {
            table.topIndex = entity.index!! - 7
        }
    }

    private fun updateLyric(time: String) {
        if (!Constant.PLAYING_LYRIC) {
            return
        }

        val items = lyrics!!.items
        var index = 0
        for (item in items) {
            item.background = getColor(SWT.COLOR_WHITE)
            if (StrUtil.equals(time, item.getText(0))) {
                item.background = getColor(SWT.COLOR_GRAY)
                index++
            }
        }

        if (index <= 7) {
            lyrics!!.topIndex = index
        } else {
            lyrics!!.topIndex = index - 7
        }
    }

    private fun initLyric() {
        Constant.PLAYING_LYRIC = false
        val path = Paths.get(Constant.PLAYING_SONG!!.lyricPath)
        if (!Files.exists(path)) {
            return
        }

        Constant.PLAYING_LYRIC = true
        lyrics!!.clearAll()
        val lyric = FileUtil.readUtf8Lines(path.toFile())
        for (s in lyric) {
            val parts = s.split("(?<=\\])".toRegex(), limit = 2).toTypedArray()
            if (parts.size < 2) {
                continue
            }

            val item = TableItem(lyrics, SWT.NONE)
            item.setText(arrayOf(parts[0], parts[1]))
        }
    }

    private fun spectrum(comp: Composite?, label: Label?) {
        timer.cancel()
        position = 0.0
        timer = Timer(true)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                display!!.asyncExec {
                    // 频谱面板
                    if (!comp!!.isDisposed) {
                        update()
                        comp.redraw()
                    }
                    // 歌词
                    //updateLyric("[" + Utils.format(position));
                    // 进度条
                    progress!!.selection =
                        (position.toInt() / (Constant.PLAYING_SONG!!.length?.div(100)!!)).toInt()
                    // 实时播放时间
                    label!!.text = format(position.toInt())
                }
            }
        }, 0, 100)
    }

    fun update() {
        if (CollUtil.isEmpty(SdlFftPlayer.TRANS) || SdlFftPlayer.TRANS.isEmpty()) {
            return
        }

        if (!player!!.playing()) {
            return
        }

        position += 0.1
        spectrum.clear()
        var i = 0
        val len = SdlFftPlayer.TRANS.size
        while (i < len) {
            val v = SdlFftPlayer.TRANS.peek()
            if (null == v) {
                i++
                continue
            }
            spectrum.add(abs(v.toInt()))
            i++
        }
    }

    private fun exit() {
        tray!!.dispose()
        System.exit(0)
        player!!.stop()
        shell!!.dispose()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val window = MusicPlayer()
            window.open()
        }
    }

}
