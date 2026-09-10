package com.xu.music.player.taskbar;

import com.sun.jna.Platform;
import com.xu.music.player.entity.SongEntity;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.plaf.basic.BasicGraphicsUtils;

/** 主屏任务栏上的透明歌词窗口，原生 SWT 托盘仍由 SWT 管理。 */
public final class TaskbarLyrics implements AutoCloseable {
    private volatile String text = "";
    private boolean enabled;
    private boolean locked = true;
    private int offset = 180;
    private int lyricWidth = 320;
    private JWindow window;
    private WindowsTaskbar bridge;
    private Timer timer;
    private Consumer<String> reportError = message -> {};

    public static boolean supported() {
        return Platform.isWindows() && !GraphicsEnvironment.isHeadless();
    }

    public void update(SongEntity song, String lyric) {
        String next = displayText(song, lyric);
        if (text.equals(next)) return;
        text = next;
        onAwt(() -> { if (enabled) refresh(); });
    }

    public void setEnabled(boolean value, Consumer<String> onError) {
        onAwt(() -> {
            enabled = value && supported();
            reportError = onError;
            if (enabled) {
                refresh();
                if (enabled) {
                    if (timer == null) timer = new Timer(500, event -> refresh());
                    timer.start();
                }
            } else {
                if (timer != null) timer.stop();
                if (window != null) window.setVisible(false);
            }
        });
    }

    public void setLocked(boolean value) {
        onAwt(() -> { locked = value; if (enabled) refresh(); });
    }

    public void changeWidth(int delta) {
        onAwt(() -> { lyricWidth = Math.clamp(lyricWidth + delta, 120, 800); if (enabled) refresh(); });
    }

    public void resetPosition() {
        onAwt(() -> { offset = 180; lyricWidth = 320; if (enabled) refresh(); });
    }

    @Override public void close() {
        onAwt(() -> {
            enabled = false;
            if (timer != null) timer.stop();
            timer = null;
            if (window != null) window.dispose();
            window = null;
            bridge = null;
            text = "";
            reportError = message -> {};
        });
    }

    private void refresh() {
        if (!enabled) return;
        try {
            if (bridge == null) bridge = new WindowsTaskbar();
            Rectangle bar = bridge.visibleBounds();
            if (bar == null || (locked && text.isBlank())) {
                if (window != null) window.setVisible(false);
                return;
            }
            if (window == null) window = createWindow();
            double scale = window.getGraphicsConfiguration().getDefaultTransform().getScaleX();
            Rectangle bounds = placement(bar, scale, offset, lyricWidth);
            if (bounds == null) { window.setVisible(false); return; }
            if (!window.getBounds().equals(bounds)) window.setBounds(bounds);
            offset = bounds.x - (int) Math.round(bar.x / scale);
            bridge.configure(window, locked);
            window.setVisible(true);
            window.repaint();
        } catch (Exception | LinkageError error) {
            enabled = false;
            if (timer != null) timer.stop();
            if (window != null) window.setVisible(false);
            LoggerFactory.getLogger(TaskbarLyrics.class).error("[TASKBAR-LYRICS] 任务栏歌词不可用", error);
            reportError.accept(error.getMessage() == null ? "无法创建任务栏歌词窗口" : error.getMessage());
        }
    }

    private JWindow createWindow() {
        JWindow view = new JWindow();
        view.setName("MusicPlayerSwtTaskbarLyrics");
        view.setType(Window.Type.UTILITY);
        view.setFocusableWindowState(false);
        view.setAutoRequestFocus(false);
        view.setBackground(new Color(0, 0, 0, 0));
        view.setAlwaysOnTop(true);
        view.getContentPane().add(new LyricText());
        // 首次显示前创建句柄并设置不激活，避免打断其他应用输入。
        view.addNotify();
        return view;
    }

    private final class LyricText extends JComponent {
        LyricText() {
            setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
            MouseAdapter mouse = new MouseAdapter() {
                private int startX;
                private int startOffset;
                @Override public void mousePressed(MouseEvent event) {
                    startX = event.getXOnScreen(); startOffset = offset;
                }
                @Override public void mouseDragged(MouseEvent event) {
                    if (locked) return;
                    offset = Math.max(0, startOffset + event.getXOnScreen() - startX);
                    refresh();
                }
                @Override public void mouseWheelMoved(MouseWheelEvent event) {
                    if (!locked) changeWidth(-event.getWheelRotation() * 24);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(getFont());
                if (!locked) {
                    g.setColor(new Color(0, 100, 180, 140));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g.setColor(new Color(120, 190, 255));
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }
                String value = text.isBlank() ? "拖动调整位置，滚轮调整宽度" : text;
                FontMetrics metrics = g.getFontMetrics();
                String shown = BasicGraphicsUtils.getClippedString(this, metrics, value, getWidth() - 16);
                int x = (getWidth() - metrics.stringWidth(shown)) / 2;
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g.setColor(new Color(0, 0, 0, 210));
                g.drawString(shown, x - 1, y); g.drawString(shown, x + 1, y);
                g.drawString(shown, x, y - 1); g.drawString(shown, x, y + 1);
                g.setColor(new Color(235, 235, 235));
                g.drawString(shown, x, y);
            } finally { g.dispose(); }
        }
    }

    private static void onAwt(Runnable action) {
        if (EventQueue.isDispatchThread()) action.run(); else EventQueue.invokeLater(action);
    }

    static String displayText(SongEntity song, String lyric) {
        if (song == null) return "";
        if (lyric != null && !lyric.isBlank()) return lyric.trim().replace('\n', ' ').replace('\r', ' ');
        String name = song.getName() == null ? "" : song.getName().trim();
        String author = song.getAuthor() == null ? "" : song.getAuthor().trim();
        if (name.isEmpty()) return author;
        if (author.isEmpty() || Arrays.asList(name.split(" - ")).contains(author)) return name;
        return name + " - " + author;
    }

    /** Win32 物理坐标转换成主屏 AWT 逻辑坐标，右侧为托盘预留空间。 */
    static Rectangle placement(Rectangle bar, double scale, int offset, int width) {
        int available = (int) Math.round(bar.width / scale) - 240;
        if (available < 120) return null;
        int actualWidth = Math.clamp(width, 120, Math.min(800, available));
        int left = Math.clamp(offset, 0, available - actualWidth);
        return new Rectangle((int) Math.round(bar.x / scale) + left, (int) Math.round(bar.y / scale),
                actualWidth, (int) Math.round(bar.height / scale));
    }
}
