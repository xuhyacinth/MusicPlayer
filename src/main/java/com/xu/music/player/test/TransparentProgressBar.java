package com.xu.music.player.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class TransparentProgressBar {

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());

        // 创建一个 ProgressBar
        ProgressBar progressBar = new ProgressBar(shell, SWT.SMOOTH);
        progressBar.setMaximum(100);

        // 使用 Canvas 覆盖绘制
        Canvas canvas = new Canvas(shell, SWT.NONE);
        canvas.addListener(SWT.Paint, e -> {
            GC gc = e.gc;
            Color bgColor = display.getSystemColor(SWT.COLOR_WHITE);
            gc.setBackground(bgColor);
            gc.fillRectangle(canvas.getClientArea());

            // 绘制进度条部分
            gc.setAlpha(128);  // 设置透明度，0 完全透明，255 不透明
            gc.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
            int width = (int) ((canvas.getClientArea().width * progressBar.getSelection()) / 100.0);
            gc.fillRectangle(0, 0, width, canvas.getClientArea().height);
        });

        // 模拟进度
        display.timerExec(100, new Runnable() {
            @Override
            public void run() {
                if (progressBar.getSelection() < progressBar.getMaximum()) {
                    progressBar.setSelection(progressBar.getSelection() + 1);
                    canvas.redraw();
                    display.timerExec(100, this);
                }
            }
        });

        shell.setSize(300, 100);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
