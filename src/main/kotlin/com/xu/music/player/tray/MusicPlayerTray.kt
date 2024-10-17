package com.xu.music.player.tray

import com.xu.music.player.utils.Utils.getImage
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Tray
import org.eclipse.swt.widgets.TrayItem

/**
 * 通用托盘
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class MusicPlayerTray(private val shell: Shell, private val tray: Tray?) {

    private var menu: Menu? = null

    fun tray() {
        if (tray == null) {
            MessageDialog.openError(shell, "错误提示", "您的系统不支持托盘图标")
        } else {
            val item = TrayItem(tray, SWT.NONE)
            item.toolTipText = "登录"
            item.image = getImage("main.png")
            menu = Menu(shell, SWT.POP_UP)
            item.addListener(SWT.MenuDetect) { arg0: Event? -> menu!!.isVisible = true }
            // 放大
//            MenuItem max = new MenuItem(menu, SWT.PUSH);
//            max.setText("放大");
//            max.addSelectionListener(new SelectionAdapter() {
//                public void widgetSelected(SelectionEvent arg0) {
//                    shell.setVisible(true);
//                    shell.setMaximized(true);
//                }
//            });
            // 缩小
            val mini = MenuItem(menu, SWT.PUSH)
            mini.text = "缩小"
            mini.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(arg0: SelectionEvent) {
                    shell.maximized = true
                }
            })
            // 关闭
            //横线
            MenuItem(menu, SWT.SEPARATOR)
            val close = MenuItem(menu, SWT.PUSH)
            close.text = "关闭"
            close.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(arg0: SelectionEvent) {
                    tray.dispose()
                    shell.dispose()
                    System.exit(0)
                }
            })
        }
    }

}
