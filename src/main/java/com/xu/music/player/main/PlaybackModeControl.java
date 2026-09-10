package com.xu.music.player.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/** 绘制播放模式图标，不依赖特殊字体；点击循环切换模式。 */
final class PlaybackModeControl {
    private final Canvas button;
    private final Color normal;
    private final Color highlighted;
    private PlaybackMode mode = PlaybackMode.SEQUENTIAL;
    private boolean hovering;
    private boolean pressed;

    PlaybackModeControl(Composite parent) {
        button = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        normal = new Color(parent.getDisplay(), 18, 150, 219);
        highlighted = new Color(parent.getDisplay(), 26, 250, 41);
        button.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        button.addPaintListener(event -> paintIcon(event.gc));
        button.addListener(SWT.MouseEnter, event -> {
            hovering = true;
            button.redraw();
        });
        button.addListener(SWT.MouseExit, event -> {
            hovering = false;
            pressed = false;
            button.redraw();
        });
        button.addListener(SWT.MouseDown, event -> {
            if (event.button == 1) {
                pressed = true;
                button.setFocus();
                button.redraw();
            }
        });
        button.addListener(SWT.MouseDoubleClick, event -> {
            if (event.button == 1) pressed = true;
        });
        button.addListener(SWT.MouseUp, event -> {
            if (event.button != 1) return;
            boolean activate = pressed && button.getClientArea().contains(event.x, event.y);
            pressed = false;
            if (activate) cycle();
            button.redraw();
        });
        button.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.CR || event.character == ' ') {
                cycle();
                event.doit = false;
            }
        });
        button.addListener(SWT.FocusIn, event -> button.redraw());
        button.addListener(SWT.FocusOut, event -> button.redraw());
        button.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override public void getName(AccessibleEvent event) {
                event.result = "播放模式：" + mode.label() + "，点击切换";
            }
        });
        button.addListener(SWT.Dispose, event -> {
            normal.dispose();
            highlighted.dispose();
        });
        updateTooltip();
    }

    PlaybackMode mode() {
        return mode;
    }

    void setBounds(int x, int y, int width, int height) {
        button.setBounds(x, y, width, height);
    }

    private void cycle() {
        mode = switch (mode) {
            case SEQUENTIAL -> PlaybackMode.RANDOM;
            case RANDOM -> PlaybackMode.REPEAT_ONE;
            case REPEAT_ONE -> PlaybackMode.SEQUENTIAL;
        };
        updateTooltip();
        button.redraw();
    }

    private void updateTooltip() {
        button.setToolTipText("播放模式：" + mode.label() + "，点击切换");
    }

    private void paintIcon(GC gc) {
        gc.setAntialias(SWT.ON);
        gc.setLineWidth(2);
        gc.setLineCap(SWT.CAP_ROUND);
        gc.setLineJoin(SWT.JOIN_ROUND);
        gc.setForeground(hovering || pressed ? highlighted : normal);
        switch (mode) {
            case SEQUENTIAL -> {
                gc.drawLine(6, 9, 19, 9);
                gc.drawLine(6, 16, 16, 16);
                gc.drawLine(6, 23, 19, 23);
                gc.drawLine(25, 8, 25, 24);
                gc.drawPolyline(new int[]{21, 20, 25, 24, 29, 20});
            }
            case RANDOM -> {
                gc.drawPolyline(new int[]{5, 9, 10, 9, 22, 23, 27, 23});
                gc.drawPolyline(new int[]{5, 23, 10, 23, 22, 9, 27, 9});
                gc.drawPolyline(new int[]{24, 6, 27, 9, 24, 12});
                gc.drawPolyline(new int[]{24, 20, 27, 23, 24, 26});
            }
            case REPEAT_ONE -> {
                gc.drawPolyline(new int[]{6, 14, 6, 9, 25, 9});
                gc.drawPolyline(new int[]{22, 6, 25, 9, 22, 12});
                gc.drawPolyline(new int[]{26, 18, 26, 23, 7, 23});
                gc.drawPolyline(new int[]{10, 20, 7, 23, 10, 26});
                // 用线段绘制数字 1，避免不同平台的字体外观差异。
                gc.drawPolyline(new int[]{14, 15, 17, 13, 17, 20});
                gc.drawLine(14, 20, 20, 20);
            }
        }
        if (button.isFocusControl()) {
            gc.drawFocus(1, 1, button.getSize().x - 2, button.getSize().y - 2);
        }
    }
}
