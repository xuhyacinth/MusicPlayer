package com.xu.music.player.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import java.util.function.IntConsumer;

/**
 * 点击音量图标后展开的竖向音量控件。
 */
final class VolumeControl {

    private static final int TRACK_PADDING = 10;
    private final Display display;
    private final Shell owner;
    private final Canvas button;
    private final Shell popup;
    private final Canvas slider;
    private final Label percentageLabel;
    private final IntConsumer onChange;
    private int percentage = 100;
    private boolean dragging;

    private final Listener outsideClick = this::hideIfOutside;
    private final Listener keyPress = event -> {
        switch (event.keyCode) {
            case SWT.ESC -> hide();
            case SWT.ARROW_UP, SWT.ARROW_RIGHT -> changeVolume(percentage + 1);
            case SWT.ARROW_DOWN, SWT.ARROW_LEFT -> changeVolume(percentage - 1);
            case SWT.HOME -> changeVolume(0);
            case SWT.END -> changeVolume(100);
            default -> { return; }
        }
        event.doit = false;
    };
    private final Listener ownerChanged = event -> hide();

    VolumeControl(Composite parent, IntConsumer onChange) {
        this.onChange = onChange;
        display = parent.getDisplay();
        owner = parent.getShell();
        button = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        button.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
        button.addPaintListener(event -> paintIcon(event.gc));
        button.addListener(SWT.MouseDown, event -> {
            if (event.button == 1) {
                toggle();
            }
        });
        button.addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.CR || event.character == ' ') {
                toggle();
                event.doit = false;
            }
        });
        button.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent event) {
                event.result = "音量 " + percentage + "%，点击展开音量条";
            }
        });

        // 不抢走主窗口焦点，避免再次点击图标时先收起又重新展开。
        popup = new Shell(owner, SWT.NO_TRIM | SWT.BORDER | SWT.TOOL | SWT.ON_TOP | SWT.NO_FOCUS);
        popup.setSize(72, 184);
        popup.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        percentageLabel = new Label(popup, SWT.CENTER);
        percentageLabel.setBounds(6, 10, 58, 22);
        slider = new Canvas(popup, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);
        slider.setBounds(15, 37, 40, 128);
        slider.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
        slider.addPaintListener(event -> paintSlider(event.gc));
        slider.addListener(SWT.MouseDown, event -> {
            if (event.button == 1) {
                dragging = true;
                slider.setCapture(true);
                changeVolume(percentageAt(event.y, slider.getClientArea().height));
            }
        });
        slider.addListener(SWT.MouseMove, event -> {
            if (dragging) {
                changeVolume(percentageAt(event.y, slider.getClientArea().height));
            }
        });
        slider.addListener(SWT.MouseUp, event -> {
            if (event.button == 1 && dragging) {
                changeVolume(percentageAt(event.y, slider.getClientArea().height));
                dragging = false;
                slider.setCapture(false);
            }
        });
        slider.addListener(SWT.MouseWheel, event -> changeVolume(percentage + event.count * 5));
        for (int type : new int[]{SWT.Move, SWT.Resize, SWT.Hide, SWT.Iconify, SWT.Deactivate}) {
            owner.addListener(type, ownerChanged);
        }
        button.addDisposeListener(event -> {
            hide();
            if (!popup.isDisposed()) {
                popup.dispose();
            }
            if (!owner.isDisposed()) {
                for (int type : new int[]{SWT.Move, SWT.Resize, SWT.Hide, SWT.Iconify, SWT.Deactivate}) {
                    owner.removeListener(type, ownerChanged);
                }
            }
        });
        updateLabels();
    }

    void setBounds(int x, int y, int width, int height) {
        button.setBounds(x, y, width, height);
    }

    private void toggle() {
        if (popup.isVisible()) {
            hide();
            return;
        }
        var anchor = button.toDisplay(0, 0);
        var size = popup.getSize();
        var screen = button.getMonitor().getClientArea();
        int x = anchor.x + (button.getSize().x - size.x) / 2;
        int y = anchor.y - size.y - 6;
        popup.setLocation(Math.clamp(x, screen.x, Math.max(screen.x, screen.x + screen.width - size.x)),
                Math.clamp(y, screen.y, Math.max(screen.y, screen.y + screen.height - size.y)));
        display.addFilter(SWT.MouseDown, outsideClick);
        display.addFilter(SWT.KeyDown, keyPress);
        popup.setVisible(true);
    }

    private void hideIfOutside(Event event) {
        if (event.widget != button
                && (!(event.widget instanceof Control control) || control.getShell() != popup)) {
            hide();
        }
    }

    private void hide() {
        dragging = false;
        display.removeFilter(SWT.MouseDown, outsideClick);
        display.removeFilter(SWT.KeyDown, keyPress);
        if (!slider.isDisposed()) {
            slider.setCapture(false);
        }
        if (!popup.isDisposed()) {
            popup.setVisible(false);
        }
    }

    private void changeVolume(int value) {
        percentage = Math.clamp(value, 0, 100);
        onChange.accept(percentage);
        updateLabels();
        slider.redraw();
        button.redraw();
    }

    private void updateLabels() {
        percentageLabel.setText(percentage + "%");
        button.setToolTipText(percentage == 0 ? "已静音，点击调节音量" : "音量 " + percentage + "%，点击调节");
        slider.setToolTipText("音量 " + percentage + "%");
    }

    static int percentageAt(int y, int height) {
        int travel = height - 1 - 2 * TRACK_PADDING;
        if (travel <= 0) {
            return 0;
        }
        return Math.clamp((int) Math.round((height - 1 - TRACK_PADDING - (double) y) * 100 / travel), 0, 100);
    }

    private void paintSlider(GC gc) {
        var area = slider.getClientArea();
        int bottom = area.height - 1 - TRACK_PADDING;
        int y = bottom - (bottom - TRACK_PADDING) * percentage / 100;
        int center = area.width / 2;
        gc.setAntialias(SWT.ON);
        gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        gc.fillRoundRectangle(center - 2, TRACK_PADDING, 4, bottom - TRACK_PADDING, 4, 4);
        gc.setBackground(display.getSystemColor(SWT.COLOR_LIST_SELECTION));
        gc.fillRoundRectangle(center - 2, y, 4, bottom - y, 4, 4);
        gc.fillOval(center - 6, y - 6, 12, 12);
    }

    private void paintIcon(GC gc) {
        gc.setAntialias(SWT.ON);
        gc.setLineWidth(2);
        gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
        gc.drawPolygon(new int[]{6, 13, 11, 13, 17, 8, 17, 24, 11, 19, 6, 19});
        if (percentage == 0) {
            gc.drawLine(21, 13, 27, 19);
            gc.drawLine(21, 19, 27, 13);
        } else {
            gc.drawArc(16, 11, 8, 10, -60, 120);
            if (percentage > 50) {
                gc.drawArc(16, 7, 14, 18, -60, 120);
            }
        }
        if (button.isFocusControl()) {
            gc.drawFocus(1, 1, button.getSize().x - 2, button.getSize().y - 2);
        }
    }
}
