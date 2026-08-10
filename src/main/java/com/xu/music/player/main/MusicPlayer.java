package com.xu.music.player.main;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import com.xu.music.player.constant.Constant;
import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.hander.MusicPlayerError;
import com.xu.music.player.lyric.LrcParser;
import com.xu.music.player.player.Player;
import com.xu.music.player.player.SdlFftPlayer;
import com.xu.music.player.tray.MusicPlayerTray;
import com.xu.music.player.utils.Utils;
import com.xu.music.player.window.SongChoose;
import com.xu.music.player.wrapper.QueryWrapper;

import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tray;

/**
 * 主页面
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class MusicPlayer {

    private static final Logger log = LoggerFactory.getLogger(MusicPlayer.class);

    // SWT UI 刷新任务
    private Runnable refreshTask;

    // SWT 主窗口实例
    protected Shell shell;
    // 音频播放核心组件
    private Player player = null;
    // SWT 显示对象（用于事件循环分配）
    private Display display;
    // 任务栏托盘
    private Tray tray;
    // 歌曲列表格
    private Table lists;
    // 歌词列表格
    private Table lyrics;
    // 底部控制与频谱展示面板
    private Composite foot;
    // 进度条组件
    private ProgressBar progress;
    // 当前播放时间标签
    private Label timeLabel1;
    // 界面拖拽时记录的 X, Y 轴坐标
    private int clickX, clickY;
    // 总播放时间标签
    private Label timeLabel2;
    // 双击选择状态标志位
    private boolean chose = true;
    // 播放/暂停控制按钮
    private Label start;
    // 是否按下了界面以进行拖拽移动
    private boolean click = false;

    /**
     * 程序的主入口，初始化并展示播放器
     */
    public static void main(String[] args) {
        try {
            MusicPlayer window = new MusicPlayer();
            window.open();
        } catch (Exception e) {
            log.error("播放异常！", e);
        }
    }

    /**
     * 开启事件循环，保持窗口打开与互动响应
     */
    public void open() {
        display = Display.getDefault();
        createContents();
        shell.open();
        shell.layout();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the window.
     * 构建窗口主要内容与 UI 布局。
     */
    protected void createContents() {
        shell = new Shell(SWT.NONE);
        shell.setImage(Utils.getImage("main.png"));
        // 修复：保留正常的播放器窗口尺寸配置并删除无效重复行
        // shell.setSize(new Point(1000, 645));
        shell.setSize(900, 486);
        shell.setText("MusicPlayer");
        // 初始化窗口到屏幕中间
        // shell.setLocation((display.getClientArea().width - shell.getSize().x) / 2,
        // (display.getClientArea().height - shell.getSize().y) / 2);
        shell.setLayout(new FillLayout(SWT.HORIZONTAL));
        shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

        // 初始化播放器
        player = SdlFftPlayer.create();

        // 托盘引入
        tray = display.getSystemTray();
        MusicPlayerTray trayutil = new MusicPlayerTray(shell, tray, this::exit);
        trayutil.tray();

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setBackgroundMode(SWT.INHERIT_FORCE);
        composite.setLayout(new FillLayout(SWT.HORIZONTAL));

        SashForm sashForm = new SashForm(composite, SWT.VERTICAL);

        Composite top = new Composite(sashForm, SWT.NONE);
        top.setBackgroundMode(SWT.INHERIT_FORCE);

        Label exit = new Label(top, SWT.NONE);
        exit.setImage(Utils.getImage("exit-1.png"));
        exit.setBounds(845, 10, 32, 32);

        Label mini = new Label(top, SWT.NONE);
        mini.setImage(Utils.getImage("mini-1.png"));
        mini.setBounds(798, 10, 32, 32);

        Combo combo = new Combo(top, SWT.NONE);
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                combo.clearSelection();
            }
        });
        combo.addModifyListener(arg0 -> {
            // List<APISearchTipsEntity> songs = Search.search(combo.getText(),"API");
            // for (APISearchTipsEntity song:songs) {
            // combo.add(song.getFilename());
            // }
            // combo.setListVisible(true);
            // combo.clearSelection();
            // for (int i = 0; i < Constant.MUSIC_PLAYER_SONGS_LIST.size(); i++) {
            // if
            // (Constant.MUSIC_PLAYER_SONGS_LIST.get(i).getName().contains(combo.getText()))
            // {
            // combo.add(Constant.MUSIC_PLAYER_SONGS_LIST.get(i).getName());
            // }
            // }
            // combo.setListVisible(true);
        });
        combo.setBounds(283, 21, 330, 25);
        combo.setVisible(false);

        Composite center = new Composite(sashForm, SWT.NONE);
        center.setBackgroundMode(SWT.INHERIT_FORCE);
        center.setLayout(new FillLayout(SWT.HORIZONTAL));

        SashForm sashForm1 = new SashForm(center, SWT.NONE);

        Composite composite1 = new Composite(sashForm1, SWT.NONE);
        composite1.setBackgroundMode(SWT.INHERIT_FORCE);
        composite1.setLayout(new FillLayout(SWT.HORIZONTAL));

        lists = new Table(composite1, SWT.FULL_SELECTION);
        lists.setHeaderVisible(true);

        TableColumn tableColumn = new TableColumn(lists, SWT.NONE);
        tableColumn.setWidth(41);
        tableColumn.setText("序号");

        TableColumn song = new TableColumn(lists, SWT.NONE);
        song.setWidth(117);
        song.setText("歌曲");

        Composite composite2 = new Composite(sashForm1, SWT.NONE);
        composite2.setBackgroundMode(SWT.INHERIT_FORCE);
        composite2.setLayout(new FillLayout(SWT.HORIZONTAL));

        lyrics = new Table(composite2, SWT.NONE);

        TableColumn lyric1 = new TableColumn(lyrics, SWT.CENTER);
        lyric1.setText("歌词");

        TableColumn lyric2 = new TableColumn(lyrics, SWT.CENTER);
        lyric2.setWidth(738);
        lyric2.setText("歌词");

        foot = new Composite(sashForm, SWT.NONE);
        foot.setBackgroundMode(SWT.INHERIT_FORCE);

        Label prev = new Label(foot, SWT.NONE);
        prev.setImage(Utils.getImage("lastsong-1.png"));
        prev.setBounds(33, 18, 32, 32);

        Label next = new Label(foot, SWT.NONE);
        next.setImage(Utils.getImage("nextsong-1.png"));
        next.setBounds(165, 18, 32, 32);

        start = new Label(foot, SWT.NONE);
        start.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (!player.playing()) {
                    return;
                }

                if (!player.pausing()) {
                    start.setImage(Utils.getImage("stop.png"));
                    player.pause();
                    stopRefresh();
                } else {
                    start.setImage(Utils.getImage("start.png"));
                    player.resume(0);
                    startRefresh(foot, timeLabel1);
                }
            }
        });
        start.setImage(Utils.getImage("stop.png"));
        start.setBounds(98, 18, 32, 32);

        progress = new ProgressBar(foot, SWT.NONE);
        progress.setEnabled(false);
        progress.setBounds(238, 25, 610, 17);
        // 设置进度条的最大长度
        progress.setMaximum(100);
        progress.setSelection(0);
        // 设置进度的条最小程度
        progress.setMinimum(0);

        timeLabel1 = new Label(foot, SWT.NONE);
        timeLabel1.setFont(Utils.getFont("Consolas", 9, SWT.NORMAL));
        timeLabel1.setEnabled(false);
        timeLabel1.setBounds(238, 4, 73, 20);

        timeLabel2 = new Label(foot, SWT.RIGHT);
        timeLabel2.setFont(Utils.getFont("Consolas", 9, SWT.NORMAL));
        timeLabel2.setEnabled(false);
        timeLabel2.setBounds(775, 4, 73, 20);

        sashForm.setWeights(1, 5, 1);
        sashForm1.setWeights(156, 728);

        // 界面移动
        top.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                click = true;
                clickX = e.x;
                clickY = e.y;
            }

            @Override
            public void mouseUp(MouseEvent e) {
                click = false;
            }
        });
        top.addMouseMoveListener(arg0 -> {
            if (click) {
                shell.setLocation(shell.getLocation().x - clickX + arg0.x, shell.getLocation().y - clickY + arg0.y);
            }
        });

        // 缩小
        mini.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                mini.setImage(Utils.getImage("mini-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                mini.setImage(Utils.getImage("mini-1.png"));
                shell.setMinimized(true);
            }
        });

        mini.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                mini.setImage(Utils.getImage("mini-1.png"));
            }

            @Override
            public void mouseHover(MouseEvent e) {
                mini.setImage(Utils.getImage("mini-2.png"));
                mini.setToolTipText("最小化");
            }
        });

        // 退出
        exit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                exit.setImage(Utils.getImage("exit-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                exit.setImage(Utils.getImage("exit-1.png"));
                exit();
            }
        });
        exit.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                exit.setImage(Utils.getImage("exit-1.png"));
            }

            @Override
            public void mouseHover(MouseEvent e) {
                exit.setImage(Utils.getImage("exit-2.png"));
                exit.setToolTipText("退出");
            }
        });

        // 双击播放
        lists.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                chose = true;
            }
        });
        lists.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (chose) {
                    TableItem[] items = lists.getSelection();
                    String id = items[0].getText(0).trim();
                    // 下一曲
                    next(id, true);
                }
            }
        });

        // 上一曲
        prev.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                prev.setImage(Utils.getImage("lastsong-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                next(null, false);// 上一曲
                prev.setImage(Utils.getImage("lastsong-1.png"));
            }
        });

        // 下一曲
        next.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                next.setImage(Utils.getImage("nextsong-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                next(null, true);// 下一曲
                next.setImage(Utils.getImage("nextsong-1.png"));
            }
        });

        foot.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(1, 5, 1);
                sashForm1.setWeights(156, 728);
            }
        });

        foot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                Constant.SPECTRUM_FOREGROUND_COLOR = Constant.COLORS
                        .get(new SecureRandom().nextInt(Constant.COLORS.size()));
            }
        });

        // 添加绘图监听器
        foot.addPaintListener(listener -> {
            if (!player.playing() || player.pausing()) {
                return;
            }

            GC gc = listener.gc;

            int width = listener.width;
            int height = listener.height;
            int length = width / 25;

            // 获取 FFT 数据快照
            var transSnapshot = player.spectrumSnapshot();
            if (transSnapshot.length < 2)
                return;

            int validDataLen = transSnapshot.length;
            if (length <= 0)
                return;

            // 对数域频率映射边界，过滤掉低频直流分量并设置最高有效频段
            double minFreqBin = 1.0;
            double maxFreqBin = validDataLen - 1;

            for (int i = 0; i < length; i++) {
                // 采用对数分布算法来将所有的频率等比例压进界面的柱形条中（偏重于低音频段的渲染使得符合人耳听觉系统）
                double ratioStart = (double) i / length;
                double ratioEnd = (double) (i + 1) / length;

                int binStart = (int) (minFreqBin * Math.pow(maxFreqBin / minFreqBin, ratioStart));
                int binEnd = (int) (minFreqBin * Math.pow(maxFreqBin / minFreqBin, ratioEnd));

                if (binEnd <= binStart)
                    binEnd = binStart + 1;
                if (binEnd > validDataLen)
                    binEnd = validDataLen;

                double sum = 0;
                int count = 0;
                for (int b = binStart; b < binEnd && b < validDataLen; b++) {
                    sum += Math.abs(transSnapshot[b]);
                    count++;
                }

                double avgMagnitude = count > 0 ? (sum / count) : 0;

                // 为了增强可视性，使用平方根放大的方式衰减高光点，并平滑整体动态幅度
                int barHeight = (int) (Math.sqrt(avgMagnitude) * 6);

                if (barHeight > height)
                    barHeight = height;

                Utils.draw(gc, i * 26 + 1, height, 22, barHeight);
            }

        });

        sashForm.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(1, 5, 1);
                sashForm1.setWeights(156, 728);
            }
        });

        composite1.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(1, 5, 1);
                sashForm1.setWeights(156, 728);
            }
        });

        composite2.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(1, 5, 1);
                sashForm1.setWeights(156, 728);
            }
        });

        sashForm1.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(1, 5, 1);
                sashForm1.setWeights(156, 728);
            }
        });

        initPlayer(shell, lists);

    }

    /**
     * 扫描初始化歌曲信息
     *
     * @param shell 窗口对象
     * @param table 表格对象
     */
    public void initPlayer(Shell shell, Table table) {
        List<SongEntity> list;
        try {
            var wrapper = new QueryWrapper<>(SongEntity.class, "song");
            list = wrapper.list();

            if (CollUtil.isEmpty(list)) {
                // 当本地没有存放数据时，自动唤起文件选择窗口添加歌曲
                var choice = new SongChoose();
                Toolkit.getDefaultToolkit().beep();
                choice.open(shell);
                list = wrapper.list();
            }
        } catch (RuntimeException exception) {
            log.error("初始化播放列表异常", exception);
            showError("无法读取或更新播放列表，请检查数据库和文件权限。");
            return;
        }

        if (CollUtil.isEmpty(list)) {
            return;
        }

        initSongTable(list, table);
    }

    private void initSongTable(List<SongEntity> list, Table table) {
        table.removeAll();
        Constant.PLAYING_LIST.clear();
        IntStream.range(0, list.size()).forEach(i -> Constant.PLAYING_LIST.put(i, list.get(i)));

        int index = 0;
        for (var entity : list) {
            var item = new TableItem(table, SWT.NONE);
            item.setText(new String[] { String.valueOf(index), entity.getName() });
            index++;
        }
    }

    private void next(String index, boolean next) {
        if (CollUtil.isEmpty(Constant.PLAYING_LIST)) {
            var msg = Utils.tips(shell, null, "未发现歌曲，现在添加歌曲？");
            if (msg.open() == SWT.YES) {
                initPlayer(shell, lists);
                if (CollUtil.isEmpty(Constant.PLAYING_LIST)) {
                    return;
                }
            } else {
                Utils.tips(shell, null, "未发现歌曲，不能播放歌曲。").open();
                return;
            }
        }

        if (StrUtil.isNotBlank(index)) {
            Constant.PLAYING_INDEX = Integer.parseInt(index);
        } else {
            if (null == Constant.PLAYING_INDEX) {
                Constant.PLAYING_INDEX = next ? 0 : Constant.PLAYING_LIST.size() - 1;
            } else {
                Constant.PLAYING_INDEX = PlaylistNavigator.move(
                        Constant.PLAYING_INDEX, Constant.PLAYING_LIST.size(), next ? 1 : -1);
            }
        }

        Constant.PLAYING_SONG = Constant.PLAYING_LIST.get(Constant.PLAYING_INDEX);
        Constant.PLAYING_SONG_LENGTH = Constant.PLAYING_SONG.getLength();
        try {
            PlaybackStarter.start(player, Constant.PLAYING_SONG.getSongPath());
        } catch (MusicPlayerError exception) {
            log.error("选择歌曲播放异常", exception);
            resetPlaybackUi();
            showError("无法播放所选歌曲，请检查文件格式和音频设备。");
            return;
        }

        try {
            Constant.MUSIC_PLAYER_PLAYING_STATE = true;
            initLyric();
            startRefresh(foot, timeLabel1);
            updateSongListsColor(lists, Constant.PLAYING_SONG);
        } catch (RuntimeException exception) {
            player.stop();
            log.error("播放界面更新异常", exception);
            resetPlaybackUi();
            showError("歌曲已停止，播放器界面更新失败。");
        }
    }

    private void updateSongListsColor(Table table, SongEntity entity) {
        start.setImage(Utils.getImage("start.png"));
        timeLabel1.setText(Utils.format(0));
        var duration = PlaybackProgress.duration(player.duration(), entity.getLength());
        timeLabel2.setText(Utils.format((int) duration));

        TableItem[] items = table.getItems();
        for (int i = 0, len = items.length; i < len; i++) {
            if (i == Constant.PLAYING_INDEX) {
                items[i].setBackground(Utils.getColor(SWT.COLOR_GRAY));
            } else {
                items[i].setBackground(Utils.getColor(SWT.COLOR_WHITE));
            }
        }

        if (entity.getIndex() <= 7) {
            table.setTopIndex(entity.getIndex());
        } else {
            table.setTopIndex(entity.getIndex() - 7);
        }

    }

    private void updateLyric(double currentPosition) {
        if (!Constant.PLAYING_LYRIC) {
            return;
        }

        TableItem[] items = lyrics.getItems();
        if (items.length == 0) {
            return;
        }

        int highlightIndex = -1;
        double maxTime = -1.0;

        // 寻找小于等于当前播放进度的最大歌词时间戳
        for (int i = 0; i < items.length; i++) {
            Object timeObj = items[i].getData("time");
            if (timeObj instanceof Double) {
                double t = (Double) timeObj;
                if (t >= 0 && t <= currentPosition) {
                    if (t > maxTime) {
                        maxTime = t;
                        highlightIndex = i;
                    }
                }
            }
        }

        // 高亮当前行，清除其它行高亮
        for (int i = 0; i < items.length; i++) {
            if (i == highlightIndex) {
                items[i].setBackground(Utils.getColor(SWT.COLOR_GRAY));
            } else {
                items[i].setBackground(Utils.getColor(SWT.COLOR_WHITE));
            }
        }

        // 自动滚动，将当前歌词行置于视口偏上
        if (highlightIndex != -1) {
            if (highlightIndex <= 7) {
                lyrics.setTopIndex(0);
            } else {
                lyrics.setTopIndex(highlightIndex - 7);
            }
        }
    }

    private void initLyric() {
        Constant.PLAYING_LYRIC = false;
        lyrics.removeAll();

        if (StrUtil.isBlank(Constant.PLAYING_SONG.getLyricPath())) {
            return;
        }

        var path = Paths.get(Constant.PLAYING_SONG.getLyricPath());
        if (!Files.exists(path)) {
            return;
        }

        try {
            var lyric = LrcParser.parse(FileUtil.readUtf8Lines(path.toFile()));
            Constant.PLAYING_LYRIC = !lyric.isEmpty();
            for (var line : lyric) {
                var item = new TableItem(lyrics, SWT.NONE);
                item.setText(new String[] { line.tag(), line.text() });
                item.setData("time", line.seconds());
            }
        } catch (RuntimeException exception) {
            log.error("歌词加载异常: {}", path, exception);
            Constant.PLAYING_LYRIC = false;
            lyrics.removeAll();
            showError("歌曲已开始播放，但歌词文件无法读取。");
        }
    }

    private void startRefresh(Composite comp, Label currentTimeLabel) {
        stopRefresh();

        refreshTask = new Runnable() {
            @Override
            public void run() {
                if (shell.isDisposed() || comp.isDisposed()) {
                    return;
                }
                if (!player.playing()) {
                    resetPlaybackUi();
                    return;
                }

                var position = player.position();
                var duration = PlaybackProgress.duration(
                        player.duration(), Constant.PLAYING_SONG.getLength());

                comp.redraw();
                updateLyric(position);
                progress.setSelection(PlaybackProgress.percentage(position, duration));
                currentTimeLabel.setText(Utils.format((int) position));
                timeLabel2.setText(Utils.format((int) duration));
                display.timerExec(100, this);
            }
        };
        display.timerExec(0, refreshTask);
    }

    private void stopRefresh() {
        if (refreshTask != null && display != null && !display.isDisposed()) {
            display.timerExec(-1, refreshTask);
            refreshTask = null;
        }
    }

    private void resetPlaybackUi() {
        Constant.MUSIC_PLAYER_PLAYING_STATE = false;
        Constant.PLAYING_LYRIC = false;
        stopRefresh();
        if (start != null && !start.isDisposed()) {
            start.setImage(Utils.getImage("stop.png"));
        }
        if (progress != null && !progress.isDisposed()) {
            progress.setSelection(0);
        }
        if (timeLabel1 != null && !timeLabel1.isDisposed()) {
            timeLabel1.setText(Utils.format(0));
        }
        if (lyrics != null && !lyrics.isDisposed()) {
            lyrics.removeAll();
        }
    }

    private void showError(String message) {
        var box = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
        box.setText("MusicPlayer");
        box.setMessage(message);
        box.open();
    }

    /**
     * 退出并释放关联的托盘与播放器进程硬件资源
     */
    private void exit() {
        stopRefresh();
        if (player != null) {
            player.close();
        }
        if (tray != null && !tray.isDisposed()) {
            tray.dispose();
        }
        if (shell != null && !shell.isDisposed()) {
            shell.dispose();
        }
        // 置于最后执行以保证前面的指令能正常被触发
        System.exit(0);
    }

}
