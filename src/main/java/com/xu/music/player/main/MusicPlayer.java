package com.xu.music.player.main;

import java.awt.*;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import com.xu.music.player.constant.Constant;
import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.player.Player;
import com.xu.music.player.player.SdlFftPlayer;
import com.xu.music.player.tray.MusicPlayerTray;
import com.xu.music.player.utils.ResourceManager;
import com.xu.music.player.utils.Utils;
import com.xu.music.player.window.SongChoose;
import com.xu.music.player.wrapper.QueryWrapper;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
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
import org.eclipse.swt.graphics.Point;
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
@Slf4j
public class MusicPlayer {

    private final List<Integer> spectrum = new LinkedList<>();

    private static Timer TIMER = new Timer(true);

    private final SecureRandom random = new SecureRandom();

    private double position;

    // 播放按钮
    public boolean playing = true;
    protected Shell shell;
    // 播放器
    private Player player = null;
    private Display display;
    // 播放器托盘
    private Tray tray;
    private Table lists;
    private Table lyrics;
    private Composite top;
    // 频谱面板
    private Composite foot;
    // 进度条
    private ProgressBar progress;
    private Label timeLabel1;
    // 界面移动
    private int clickX, clickY;
    private Label timeLabel2;
    // 双击播放
    private boolean chose = true;
    private Label start;
    // 界面移动
    private boolean click = false;

    public static void main(String[] args) {
        try {
            MusicPlayer window = new MusicPlayer();
            window.open();
        } catch (Exception e) {
            log.error("播放异常！", e);
        }
    }

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
     */
    protected void createContents() {
        shell = new Shell(SWT.NONE);
        shell.setImage(Utils.getImage("main.png"));
        shell.setSize(new Point(1000, 645));
        shell.setSize(900, 486);
        shell.setText("MusicPlayer");
        shell.setLocation((display.getClientArea().width - shell.getSize().x) / 2,
                (display.getClientArea().height - shell.getSize().y) / 2);
        shell.setLayout(new FillLayout(SWT.HORIZONTAL));
        shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

        // 初始化播放器
        player = SdlFftPlayer.create();

        // 托盘引入
        tray = display.getSystemTray();
        MusicPlayerTray trayutil = new MusicPlayerTray(shell, tray);
        trayutil.tray();

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setBackgroundMode(SWT.INHERIT_FORCE);
        composite.setLayout(new FillLayout(SWT.HORIZONTAL));

        SashForm sashForm = new SashForm(composite, SWT.VERTICAL);

        top = new Composite(sashForm, SWT.NONE);
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
            // 	combo.add(song.getFilename());
            // }
            // combo.setListVisible(true);
//            combo.clearSelection();
//            for (int i = 0; i < Constant.MUSIC_PLAYER_SONGS_LIST.size(); i++) {
//                if (Constant.MUSIC_PLAYER_SONGS_LIST.get(i).getName().contains(combo.getText())) {
//                    combo.add(Constant.MUSIC_PLAYER_SONGS_LIST.get(i).getName());
//                }
//            }
//            combo.setListVisible(true);
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

        TableColumn tableColumn_1 = new TableColumn(lists, SWT.NONE);
        tableColumn_1.setWidth(117);
        tableColumn_1.setText("歌曲");

        Composite composite2 = new Composite(sashForm1, SWT.NONE);
        composite2.setBackgroundMode(SWT.INHERIT_FORCE);
        composite2.setLayout(new FillLayout(SWT.HORIZONTAL));

        lyrics = new Table(composite2, SWT.NONE);

        TableColumn tableColumn_2 = new TableColumn(lyrics, SWT.CENTER);
        tableColumn_2.setText("歌词");

        TableColumn tableColumn_3 = new TableColumn(lyrics, SWT.CENTER);
        tableColumn_3.setWidth(738);
        tableColumn_3.setText("歌词");

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
                playing = Constant.MUSIC_PLAYER_PLAYING_STATE;
                if (playing) {
                    // TODO:
                    start.setImage(Utils.getImage("start.png"));
                    playing = false;
                    player.pause();
                } else {
                    // TODO:
                    playing = true;
                    start.setImage(Utils.getImage("stop.png"));
                    player.resume(0);
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
        timeLabel1.setFont(ResourceManager.getFont("Consolas", 9, SWT.NORMAL));
        timeLabel1.setEnabled(false);
        timeLabel1.setBounds(238, 4, 73, 20);

        timeLabel2 = new Label(foot, SWT.RIGHT);
        timeLabel2.setFont(ResourceManager.getFont("Consolas", 9, SWT.NORMAL));
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
                    choose(id, true);// 下一曲
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
                choose(null, false);// 上一曲
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
                choose(null, true);// 下一曲
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
                org.eclipse.swt.graphics.Color color = Constant.COLORS.get(new Random().nextInt(Constant.COLORS.size()));
                if (color != Constant.SPECTRUM_BACKGROUND_COLOR) {
                    Constant.SPECTRUM_FOREGROUND_COLOR = color;
                }
            }
        });

        // 添加绘图监听器
        foot.addPaintListener(listener -> {
            GC gc = listener.gc;

            int width = listener.width;
            int height = listener.height;
            int length = width / 25;

            if (spectrum.size() >= length) {
                for (int i = 0; i < length; i++) {
                    draw(gc, i * 26, height, 26, spectrum.get(i));
                }
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

    public void initPlayer(Shell shell, Table table) {
        QueryWrapper<SongEntity> wrapper = new QueryWrapper<>(SongEntity.class, "song");
        List<SongEntity> list = wrapper.list();

        if (CollUtil.isEmpty(list)) {
            SongChoose choice = new SongChoose();
            Toolkit.getDefaultToolkit().beep();
            choice.open(shell);
            list = wrapper.list();
        }

        if (CollUtil.isEmpty(list)) {
            return;
        }

        initTable(list, table);
    }

    private void initTable(List<SongEntity> list, Table table) {
        table.removeAll();
        TableItem item;
        IntStream.range(0, list.size()).forEach(i -> {
            Constant.PLAYING_LIST.put(i, list.get(i));
        });

        int index = 0;
        for (SongEntity entity : list) {
            item = new TableItem(table, SWT.NONE);
            item.setText(new String[]{String.valueOf(index), entity.getName()});
            index++;
        }
    }

    private void choose(String index, boolean next) {
        if (CollUtil.isEmpty(Constant.PLAYING_LIST)) {
            MessageBox msg = Utils.tips(shell, null, "未发现歌曲，现在添加歌曲？");
            if (msg.open() == SWT.YES) {
                initPlayer(shell, lists);
            } else {
                Utils.tips(shell, null, "未发现歌曲，不能播放歌曲。").open();
                return;
            }
        }

        if (StrUtil.isNotBlank(index)) {
            Constant.PLAYING_INDEX = Integer.parseInt(index);
        } else {
            Constant.PLAYING_INDEX += next ? 1 : -1;
            if (Constant.PLAYING_INDEX > Constant.PLAYING_LIST.size() - 1) {
                Constant.PLAYING_INDEX = 0;
            }
            if (Constant.PLAYING_INDEX < 0) {
                Constant.PLAYING_INDEX = Constant.PLAYING_LIST.size() + 1;
            }
        }

        Constant.PLAYING_SONG = Constant.PLAYING_LIST.get(Constant.PLAYING_INDEX);
        try {
            player.load(Constant.PLAYING_SONG.getSongPath());
            player.play();
            Constant.MUSIC_PLAYER_PLAYING_STATE = true;
        } catch (Exception e) {
            log.error("选择歌曲播放异常！", e);
        }

        spectrum(foot, timeLabel2);
        updateListsColor(lists, Constant.PLAYING_SONG);
    }

    private void spectrum(Composite comp, Label label) {
        TIMER.cancel();
        position = 0;
        TIMER = new Timer(true);
        TIMER.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                display.asyncExec(() -> {
                    // 频谱面板
                    if (!comp.isDisposed()) {
                        update();
                        comp.redraw();
                    }
                    // 进度条
                    progress.setSelection((int) ((int) position / (Constant.PLAYING_SONG.getLength() / 100)));
                    // 实时播放时间
                    label.setText(Utils.format((int) position));
                });
            }
        }, 0, 100);
    }

    /**
     * Composite 绘画
     *
     * @param gc     GC
     * @param x      x坐标
     * @param y      y坐标
     * @param width  宽度
     * @param height 高度
     * @date 2024年2月2日19点27分
     * @since V1.0.0.0
     */
    private void draw(GC gc, int x, int y, int width, int height) {
        // 设置条形的颜色
        org.eclipse.swt.graphics.Color color = new org.eclipse.swt.graphics.Color(display, random.nextInt(255), random.nextInt(255), random.nextInt(255));
        gc.setBackground(color);
        // 绘制条形
        org.eclipse.swt.graphics.Rectangle draw = new org.eclipse.swt.graphics.Rectangle(x, y, width, -height);
        gc.fillRectangle(draw);
        // 释放颜色资源
        color.dispose();
    }

    /**
     * 模拟 更新绘画的数据
     *
     * @date 2024年2月2日19点27分
     * @since V1.0.0.0
     */
    public void update() {
        if (CollUtil.isEmpty(SdlFftPlayer.TRANS) || SdlFftPlayer.TRANS.isEmpty()) {
            return;
        }
        position += 0.1;
        spectrum.clear();
        for (int i = 0, len = SdlFftPlayer.TRANS.size(); i < len; i++) {
            Double v = SdlFftPlayer.TRANS.peek();
            if (null == v) {
                continue;
            }
            spectrum.add(Math.abs(v.intValue()));
        }
    }

    private void updateListsColor(Table table, SongEntity entity) {
        start.setImage(Utils.getImage("start.png"));
        timeLabel1.setText(Utils.format(entity.getLength().intValue()));

        TableItem[] items = table.getItems();
        for (TableItem item : items) {
            if (StrUtil.equals(entity.getId(), item.getText(0))) {
                item.setBackground(ResourceManager.getColor(SWT.COLOR_GRAY));
            } else {
                item.setBackground(ResourceManager.getColor(SWT.COLOR_WHITE));
            }
        }

        if (entity.getIndex() <= 7) {
            table.setTopIndex(entity.getIndex());
        } else {
            table.setTopIndex(entity.getIndex() - 7);
        }

    }

    private void exit() {
        tray.dispose();
        System.exit(0);
        player.stop();
        shell.dispose();
    }

}
