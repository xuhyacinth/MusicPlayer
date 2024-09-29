package com.xu.music.player.test;

import cn.hutool.core.collection.CollUtil;

import com.xu.music.player.player.SdlFftPlayer;
import com.xu.music.player.player.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author hyacinth
 */
public class SwtDraw {

    private Shell shell = null;

    private Display display = null;

    private Composite composite = null;

    private final Random random = new Random();

    private List<Integer> spectrum = new LinkedList<>();

    public static void main(String[] args) {
        SwtDraw test = new SwtDraw();
        test.open();
    }

    /**
     * 测试播放
     */
    public void play() {
        try {
            Player player = SdlFftPlayer.create();
            player.load("C:\\Users\\xuyq\\Music\\Beyond - 长城（粤语）.flac");
            player.play();
        } catch (Exception e) {

        }
    }

    /**
     * 打开 SWT 界面
     *
     * @date 2024年2月2日19点27分
     * @since V1.0.0.0
     */
    public void open() {
        display = Display.getDefault();
        createContents();
        shell.open();
        shell.layout();
        play();
        task();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * 设置 SWT Shell内容
     *
     * @date 2024年2月2日19点27分
     * @since V1.0.0.0
     */
    protected void createContents() {
        shell = new Shell(display);
        shell.setSize(900, 500);
        shell.setLayout(new FillLayout(SWT.HORIZONTAL));

        // 创建一个Composite
        composite = new Composite(shell, SWT.NONE);

        // 添加绘图监听器
        composite.addPaintListener(listener -> {
            GC gc = listener.gc;

            int width = listener.width;
            int height = listener.height;
            int length = width / 25;

            if (spectrum.size() >= length) {
                for (int i = 0; i < length; i++) {
                    draw(gc, i * 25, height, 25, spectrum.get(i));
                }
            }

        });

    }

    /**
     * 模拟 需要绘画的数据 任务
     *
     * @date 2024年2月2日19点27分
     * @since V1.0.0.0
     */
    public void task() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                display.asyncExec(() -> {
                    if (!composite.isDisposed()) {
                        // 在这里调用你更新数据的方法
                        updateData();
                        // 重绘
                        composite.redraw();
                    }
                });
            }
        }, 0, 100);
    }

    /**
     * 模拟 更新绘画的数据
     *
     * @date 2024年2月2日19点27分
     * @since V1.0.0.0
     */
    public void updateData() {
        if (CollUtil.isEmpty(SdlFftPlayer.TRANS) || SdlFftPlayer.TRANS.isEmpty()) {
            return;
        }

        spectrum.clear();
        for (int i = 0, len = SdlFftPlayer.TRANS.size(); i < len; i++) {
            Double v = SdlFftPlayer.TRANS.peek();
            if (null == v) {
                continue;
            }
            spectrum.add(Math.abs(v.intValue()));
        }
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
        Color color = new Color(display, random.nextInt(255), random.nextInt(255), random.nextInt(255));
        gc.setBackground(color);
        // 绘制条形
        Rectangle draw = new Rectangle(x, y, width, -height);
        gc.fillRectangle(draw);
        // 释放颜色资源
        color.dispose();
    }

}
