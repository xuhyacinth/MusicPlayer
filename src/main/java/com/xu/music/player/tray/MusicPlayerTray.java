package com.xu.music.player.tray;

import com.xu.music.player.utils.Utils;
import com.xu.music.player.taskbar.TaskbarLyrics;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import java.util.Objects;

/**
 * 通用托盘
 *
 * @since 2024年6月4日19点07分
 * @version swt-java/v1.0.0
 */
public class MusicPlayerTray {

    private final Tray tray;
    private final Shell shell;
    private final Runnable closeAction;
    private Menu menu;
    private final TaskbarLyrics lyrics;

    public MusicPlayerTray(Shell shell, Tray tray, Runnable closeAction) {
        this(shell, tray, closeAction, null);
    }

    public MusicPlayerTray(Shell shell, Tray tray, Runnable closeAction, TaskbarLyrics lyrics) {
        this.lyrics = lyrics;
        this.shell = shell;
        this.tray = tray;
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
    }

    public void tray() {
        if (tray == null) {
            MessageDialog.openError(shell, "错误提示", "您的系统不支持托盘图标");
        } else {
            TrayItem item = new TrayItem(tray, SWT.NONE);
            item.setToolTipText("音乐播放器");
            item.setImage(Utils.getImage("main.png"));

            // 双击或单击托盘图标还原窗口
            item.addListener(SWT.Selection, arg0 -> {
                shell.setVisible(true);
                shell.setMinimized(false);
                shell.setActive();
            });

            menu = new Menu(shell, SWT.POP_UP);
            item.addListener(SWT.MenuDetect, event -> {
                // 使用同一 SWT 显示设备的逻辑坐标，不混入 AWT 或 Win32 物理像素。
                menu.setLocation(shell.getDisplay().getCursorLocation());
                menu.setVisible(true);
            });
            shell.addDisposeListener(event -> { if (!item.isDisposed()) item.dispose(); });

            // 显示主窗口
            MenuItem show = new MenuItem(menu, SWT.PUSH);
            show.setText("显示主窗口");
            show.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    shell.setVisible(true);
                    shell.setMinimized(false);
                    shell.setActive();
                }
            });

            // 最小化
            MenuItem mini = new MenuItem(menu, SWT.PUSH);
            mini.setText("最小化");
            mini.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    shell.setMinimized(true);
                }
            });

            if (lyrics != null && TaskbarLyrics.supported()) addLyricsMenu();

            // 横线
            new MenuItem(menu, SWT.SEPARATOR);
            MenuItem close = new MenuItem(menu, SWT.PUSH);
            close.setText("关闭");
            close.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    close();
                }
            });
        }
    }

    private void addLyricsMenu() {
        new MenuItem(menu, SWT.SEPARATOR);
        MenuItem enabled = new MenuItem(menu, SWT.CHECK);
        enabled.setText("任务栏歌词");
        var display = shell.getDisplay();
        enabled.addListener(SWT.Selection, event -> lyrics.setEnabled(enabled.getSelection(), message -> {
            if (display.isDisposed()) return;
            try {
                display.asyncExec(() -> {
                    if (shell.isDisposed() || enabled.isDisposed()) return;
                    enabled.setSelection(false);
                    MessageDialog.openError(shell, "任务栏歌词", message);
                });
            } catch (org.eclipse.swt.SWTException error) {
                if (error.code != SWT.ERROR_DEVICE_DISPOSED) throw error;
            }
        }));
        MenuItem locked = new MenuItem(menu, SWT.CHECK);
        locked.setText("锁定歌词位置（鼠标穿透）");
        locked.setSelection(true);
        locked.addListener(SWT.Selection, event -> lyrics.setLocked(locked.getSelection()));
        action("歌词变窄", () -> lyrics.changeWidth(-40));
        action("歌词变宽", () -> lyrics.changeWidth(40));
        action("重置歌词位置和宽度", lyrics::resetPosition);
    }

    private void action(String text, Runnable action) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(text);
        item.addListener(SWT.Selection, event -> action.run());
    }

    void close() {
        closeAction.run();
    }

}
