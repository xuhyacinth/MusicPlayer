package com.xu.music.player.main;

import java.awt.Color;
import java.awt.Toolkit;
import java.util.LinkedList;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.wb.swt.SWTResourceManager;

import com.xu.music.player.config.Reading;
import com.xu.music.player.config.SongChoiceWindow;
import com.xu.music.player.entity.PlayerEntity;
import com.xu.music.player.lyric.LoadLocalLyric;
import com.xu.music.player.modle.Controller;
import com.xu.music.player.modle.ControllerServer;
import com.xu.music.player.player.Player;
import com.xu.music.player.player.XMusic;
import com.xu.music.player.system.Constant;
import com.xu.music.player.tray.MusicPlayerTray;

/**
 * Java MusicPlayer 观察者
 *
 * @Author: hyacinth
 * @ClassName: LyricPlayer
 * @Description: TODO
 * @Date: 2020年5月18日22:21:13
 * @Copyright: hyacinth
 */
public class MusicPlayer {

    public boolean playing = true; // 播放按钮
    private Player player = null; // 播放器
    private static int merchant = 0;
    private static int remainder = 0;
    private static String format = "";
    protected Shell shell;
    private Display display;
    private Tray tray; // 播放器托盘
    private Table lists;
    private Table lyrics;
    private Composite top;
    private Composite foot; // 频谱面板
    private ProgressBar progress; // 进度条
    private Label timeLabel1;

    private int clickX, clickY; // 界面移动
    private Label timeLabel2;

    private boolean chose = true; // 双击播放
    private Label start;
    private ControllerServer server = new ControllerServer(); // 歌词及频谱
    private boolean click = false; // 界面移动

    /**
     * Launch the application.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            MusicPlayer window = new MusicPlayer();
            window.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String format(int time) {
        merchant = time / 60;
        remainder = time % 60;
        if (time < 10) {
            format = "00:0" + time;
        } else if (time < 60) {
            format = "00:" + time;
        } else {
            if (merchant < 10 && remainder < 10) {
                format = "0" + merchant + ":0" + remainder;
            } else if (merchant < 10 && remainder < 60) {
                format = "0" + merchant + ":" + remainder;
            } else if (merchant >= 10 && remainder < 10) {
                format = merchant + ":0" + remainder;
            } else if (merchant >= 10 && remainder < 60) {
                format = merchant + ":0" + remainder;
            }
        }
        return format;
    }

    /**
     * Open the window.
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
     */
    protected void createContents() {
        shell = new Shell(SWT.NONE);
        shell.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/main.png"));
        shell.setSize(new Point(1000, 645));
        shell.setSize(900, 486);
        shell.setText("MusicPlayer");
        shell.setLocation((display.getClientArea().width - shell.getSize().x) / 2,
                (display.getClientArea().height - shell.getSize().y) / 2);
        shell.setLayout(new FillLayout(SWT.HORIZONTAL));
        shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

        // 初始化播放器
        player = XMusic.createPlayer();

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
        exit.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/exit-1.png"));
        exit.setBounds(845, 10, 32, 32);

        Label mini = new Label(top, SWT.NONE);
        mini.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/mini-1.png"));
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
            combo.clearSelection();
            for (int i = 0; i < Constant.MUSIC_PLAYER_SONGS_LIST.size(); i++) {
                if (Constant.MUSIC_PLAYER_SONGS_LIST.get(i).contains(combo.getText())) {
                    combo.add(Constant.MUSIC_PLAYER_SONGS_LIST.get(i).split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[1]);
                }
            }
            combo.setListVisible(true);
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
        prev.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/lastsong-1.png"));
        prev.setBounds(33, 18, 32, 32);

        Label next = new Label(foot, SWT.NONE);
        next.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/nextsong-1.png"));
        next.setBounds(165, 18, 32, 32);

        start = new Label(foot, SWT.NONE);
        start.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                playing = Constant.MUSIC_PLAYER_PLAYING_STATE;
                if (playing && !XMusic.isPlaying()) {
                    // TODO:
                    start.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/start.png"));
                    playing = false;
                } else {
                    // TODO:
                    playing = true;
                    start.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/stop.png"));
                }
            }
        });
        start.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/stop.png"));
        start.setBounds(98, 18, 32, 32);

        progress = new ProgressBar(foot, SWT.NONE);
        progress.setEnabled(false);
        progress.setBounds(238, 25, 610, 17);
        progress.setMaximum(100);// 设置进度条的最大长度
        progress.setSelection(0);
        progress.setMinimum(0);// 设置进度的条最小程度

        timeLabel1 = new Label(foot, SWT.NONE);
        timeLabel1.setFont(SWTResourceManager.getFont("Consolas", 9, SWT.NORMAL));
        timeLabel1.setEnabled(false);
        timeLabel1.setBounds(238, 4, 73, 20);

        timeLabel2 = new Label(foot, SWT.RIGHT);
        timeLabel2.setFont(SWTResourceManager.getFont("Consolas", 9, SWT.NORMAL));
        timeLabel2.setEnabled(false);
        timeLabel2.setBounds(775, 4, 73, 20);

        sashForm.setWeights(new int[]{1, 5, 1});
        sashForm1.setWeights(new int[]{156, 728});

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
        top.addMouseMoveListener(arg0 -> {// 当鼠标按下的时候执行这条语句
            if (click) {
                shell.setLocation(shell.getLocation().x - clickX + arg0.x, shell.getLocation().y - clickY + arg0.y);
            }
        });

        // 缩小
        mini.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                mini.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/mini-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                mini.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/mini-1.png"));
                shell.setMinimized(true);
            }
        });

        mini.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                mini.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/mini-1.png"));
            }

            @Override
            public void mouseHover(MouseEvent e) {
                mini.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/mini-2.png"));
                mini.setToolTipText("最小化");
            }
        });

        // 退出
        exit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                exit.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/exit-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                exit.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/exit-1.png"));
                exitMusicPlayer();
            }
        });
        exit.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                exit.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/exit-1.png"));
            }

            @Override
            public void mouseHover(MouseEvent e) {
                exit.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/exit-2.png"));
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
                    int index = Integer.parseInt(items[0].getText(0).trim());
                    nextSong(index - 1, true);// 下一曲
                }
            }
        });

        // 上一曲
        prev.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                prev.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/lastsong-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                nextSong(-1, false);// 上一曲
                prev.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/lastsong-1.png"));
            }
        });

        // 下一曲
        next.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                next.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/nextsong-2.png"));
            }

            @Override
            public void mouseUp(MouseEvent e) {
                nextSong(-1, true);// 下一曲
                next.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/nextsong-1.png"));
            }
        });

        foot.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(new int[]{1, 5, 1});
                sashForm1.setWeights(new int[]{156, 728});
            }
        });

        foot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                Color color = Constant.MUSIC_PLAYER_COLORS.get(new Random().nextInt(Constant.MUSIC_PLAYER_COLORS.size()));
                if (color != Constant.SPECTRUM_BACKGROUND_COLOR) {
                    Constant.SPECTRUM_FOREGROUND_COLOR = color;
                }
            }
        });

        sashForm.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(new int[]{1, 5, 1});
                sashForm1.setWeights(new int[]{156, 728});
            }
        });

        composite1.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(new int[]{1, 5, 1});
                sashForm1.setWeights(new int[]{156, 728});
            }
        });

        composite2.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(new int[]{1, 5, 1});
                sashForm1.setWeights(new int[]{156, 728});
            }
        });

        sashForm1.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                sashForm.setWeights(new int[]{1, 5, 1});
                sashForm1.setWeights(new int[]{156, 728});
            }
        });

        initMusicPlayer(shell, lists);
        System.gc();

    }

    /**
     * Java MusicPlayer 初始化音乐播放器
     *
     * @param shell
     * @param table
     * @return void
     * @Author: hyacinth
     * @Title: initMusicPlayer
     * @Description: TODO
     * @date: 2019年12月26日 下午7:20:00
     */
    public void initMusicPlayer(Shell shell, Table table) {
        SongChoiceWindow choice = new SongChoiceWindow();
        new Reading().read();
        if (Constant.MUSIC_PLAYER_SONGS_LIST == null || Constant.MUSIC_PLAYER_SONGS_LIST.size() <= 0) {
            Toolkit.getDefaultToolkit().beep();
            choice.openChoiseWindows(shell);
        }
        updatePlayerSongLists(Constant.MUSIC_PLAYER_SONGS_LIST, table);
        readMusicPlayerPlayingSong();
    }

    /**
     * Java MusicPlayer 更新播放歌曲列表
     *
     * @param lists
     * @param table
     * @return void
     * @Author: hyacinth
     * @Title: updatePlayerSongLists
     * @Description: TODO
     * @date: 2019年12月26日 下午7:20:33
     */
    private void updatePlayerSongLists(LinkedList<String> lists, Table table) {
        table.removeAll();
        TableItem item;
        for (int i = 0; i < lists.size(); i++) {
            item = new TableItem(table, SWT.NONE);
            item.setText(new String[]{lists.get(i).split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[0], lists.get(i).split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[2]});
        }
    }

    /**
     * Java MusicPlayer 改变播放歌曲
     * <table border="1" cellpadding="10">
     * <tr><td colspan="2" align="center">changePlayingSong</td></tr>
     * <tr><th align="center">Mode 输入参数</th><th align="center">参数解释</th></tr>
     * <tr><td align="left">false</td><td align="left">上一曲</td></tr>
     * <tr><td align="left">true</td><td align="left">下一曲</td></tr>
     *
     * @param index 歌曲索引
     * @param next  切歌模式(上一曲/下一曲)
     * @return void
     * @Author: hyacinth
     * @Title: nextSong
     * @date: 2021年9月6日17点11分
     */
    private void nextSong(int index, boolean next) {
        if (index >= Constant.MUSIC_PLAYER_SONGS_LIST.size()) {
            index = 0;
        }
        if (Constant.MUSIC_PLAYER_SONGS_LIST.size() <= 0) {
            Toolkit.getDefaultToolkit().beep();
            MessageBox message = new MessageBox(shell, SWT.YES | SWT.ICON_WARNING | SWT.NO);
            message.setText("提示");
            message.setMessage("未发现歌曲，现在添加歌曲？");
            if (message.open() == SWT.YES) {
                initMusicPlayer(shell, lists);
            } else {
                Toolkit.getDefaultToolkit().beep();
                message = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
                message.setText("提示");
                message.setMessage("你将不能播放歌曲。");
                message.open();
            }
        }
        Constant.PLAYING_SONG_INDEX = index == -1 ? Constant.PLAYING_SONG_INDEX : index;
        Constant.PLAYING_SONG_NAME = Constant.MUSIC_PLAYER_SONGS_LIST.get(Constant.PLAYING_SONG_INDEX);
        try {
            player.load(Constant.PLAYING_SONG_NAME.split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[1]);
            player.start();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        Constant.MUSIC_PLAYER_PLAYING_STATE = true;
        updatePlayerSongListsColor(lists, Constant.PLAYING_SONG_INDEX);
        if (next) {// 下一曲
            if (Constant.PLAYING_SONG_INDEX >= Constant.MUSIC_PLAYER_SONGS_LIST.size()) {
                Constant.PLAYING_SONG_INDEX = 0;
            } else {
                Constant.PLAYING_SONG_INDEX++;
            }
        } else {// 上一曲
            if (Constant.PLAYING_SONG_INDEX <= 0) {
                Constant.PLAYING_SONG_INDEX = Constant.MUSIC_PLAYER_SONGS_LIST.size();
            } else {
                Constant.PLAYING_SONG_INDEX--;
            }
        }
        updatePlayerSongListsColor(lists, Constant.PLAYING_SONG_INDEX);
    }

    /**
     * Java MusicPlayer 改变选中歌曲的颜色
     *
     * @param table
     * @param index
     * @return void
     * @Author: hyacinth
     * @Title: updatePlayerSongListsColor
     * @Description: TODO
     * @date: 2019年12月26日 下午7:40:10
     */
    private void updatePlayerSongListsColor(Table table, int index) {
        start.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/start.png"));

        Constant.PLAYING_SONG_HAVE_LYRIC = false;
        Constant.PLAYING_SONG_LENGTH = Integer.parseInt(Constant.PLAYING_SONG_NAME.split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[4]);
        timeLabel1.setText(format(Constant.PLAYING_SONG_LENGTH));

        TableItem[] items = table.getItems();
        for (int i = 0; i < items.length; i++) {
            if (index == i) {
                items[i].setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
            } else {
                items[i].setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
            }
        }
        if (index <= 7) {
            table.setTopIndex(index);
        } else {
            table.setTopIndex(index - 7);
        }
        if ("Y".equalsIgnoreCase(Constant.MUSIC_PLAYER_SONGS_LIST.get(Constant.PLAYING_SONG_INDEX).split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[4])) {
            Constant.PLAYING_SONG_HAVE_LYRIC = true;
            LoadLocalLyric lyric = new LoadLocalLyric();
            String path = Constant.MUSIC_PLAYER_SONGS_LIST.get(Constant.PLAYING_SONG_INDEX).split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[0];
            path = path.substring(0, path.lastIndexOf(".")) + ".lrc";
            lyric.lyric(path);
            lyrics.removeAll();
            if (Constant.PLAYING_SONG_LYRIC != null && Constant.PLAYING_SONG_LYRIC.size() > 0) {
                TableItem item;
                for (int i = 0, len = Constant.PLAYING_SONG_LYRIC.size() + 8; i < len; i++) {
                    item = new TableItem(lyrics, SWT.NONE);
                    if (i < len - 8) {
                        item.setText(new String[]{"", Constant.PLAYING_SONG_LYRIC.get(i).split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[1]});
                    }
                }
                PlayerEntity.setBar(progress);
                PlayerEntity.setText(timeLabel2);
                PlayerEntity.setSong(Constant.PLAYING_SONG_NAME);
                PlayerEntity.setTable(lyrics);
            }
            PlayerEntity.setSpectrum(foot);
            server.endLyricPlayer(new Controller());
            server.startLyricPlayer(new Controller());
        }
        setMusicPlayerPlayingSong(index + "");
    }

    /**
     * Java MusicPlayer 退出音乐播放器
     *
     * @return void
     * @Author: hyacinth
     * @Title: exitMusicPlayer
     * @Description: TODO
     * @date: 2019年12月26日 下午7:57:12
     */
    private void exitMusicPlayer() {
        tray.dispose();
        System.exit(0);
        player.stop();
        shell.dispose();
    }

    /**
     * Java MusicPlayer 将正在播放的歌曲存在注册表中
     *
     * @param index
     * @return void
     * @Author: hyacinth
     * @Title: setMusicPlayerPlayingSong
     * @Description: TODO
     * @date: 2019年12月29日 下午2:57:30
     */
    private void setMusicPlayerPlayingSong(String index) {
        Preferences preferences = Preferences.userNodeForPackage(MusicPlayer.class);
        if (preferences.get("MusicPlayer", null) == null) {
            preferences.put("MusicPlayer", index);
        } else {
            preferences.put("MusicPlayer", index);
        }
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Java MusicPlayer 读取上次播放器退出前播放的歌曲
     *
     * @return void
     * @Author: hyacinth
     * @Title: readMusicPlayerPlayingSong
     * @Description: TODO
     * @date: 2019年12月29日 下午2:58:24
     */
    private void readMusicPlayerPlayingSong() {
//        Preferences preferences = Preferences.userNodeForPackage(MusicPlayer.class);
//        String index = preferences.get("MusicPlayer", null);
//        nextSong(Integer.parseInt(index), true);
    }

    /**
     * Java MusicPlayer 暂停播放(未实现)
     *
     * @return void
     * @Author: hyacinth
     * @Title: stopMusicPlayer
     * @Description: TODO
     * @date: 2019年12月26日 下午7:47:41
     */
    public void stopMusicPlayer() {
        try {
            player.end();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Constant.MUSIC_PLAYER_PLAYING_STATE = false;
        start.setImage(SWTResourceManager.getImage(MusicPlayer.class, "/com/xu/music/player/image/stop.png"));
        updatePlayerSongListsColor(lists, Constant.PLAYING_SONG_INDEX);
    }

}
