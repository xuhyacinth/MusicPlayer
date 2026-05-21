package com.xu.music.player.tray;

import com.xu.music.player.utils.Utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;


/**
 * 通用托盘
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class MusicPlayerTray {

    private final Tray tray;
    private final Shell shell;
    private Menu menu;

    public MusicPlayerTray(Shell shell, Tray tray) {
        super();
        this.shell = shell;
        this.tray = tray;
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
            item.addListener(SWT.MenuDetect, arg0 -> menu.setVisible(true));

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

            // 横线
            new MenuItem(menu, SWT.SEPARATOR);
            MenuItem close = new MenuItem(menu, SWT.PUSH);
            close.setText("关闭");
            close.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    tray.dispose();
                    shell.dispose();
                    System.exit(0);
                }
            });
        }
    }

}
