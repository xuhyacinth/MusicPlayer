package com.xu.music.player.thread;

import com.xu.music.player.fft.Complex;
import com.xu.music.player.fft.FFT;
import com.xu.music.player.player.XMusic;
import com.xu.music.player.system.Constant;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Java MusicPlayer 音频线程
 *
 * @Author: hyacinth
 * @ClassName: SpectrumThread
 * @Description: TODO
 * @Date: 2019年12月26日 下午7:58:50
 * @Copyright: hyacinth
 */
public class SpectrumThread extends Thread {

    private Composite spectrum;//频谱面板

    private int twidth;// 频谱总高读
    private int theight;// 频谱总宽度
    private int sheight;// 频谱高度

    public SpectrumThread(Composite spectrum) {
        this.spectrum = spectrum;
        twidth = Constant.SPECTRUM_TOTAL_WIDTH;
        theight = Constant.SPECTRUM_TOTAL_HEIGHT;
    }

    @Override
    public void run() {
        while (XMusic.isPlaying()) {
            Display.getDefault().asyncExec(() -> {
                twidth = Constant.SPECTRUM_TOTAL_WIDTH;
                theight = Constant.SPECTRUM_TOTAL_HEIGHT;
                draw(1, twidth, theight);
            });
            try {
                Thread.sleep(Constant.SPECTRUM_REFLASH_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void draw(int style, int width, int height) {

        InputStream inputStream = null;
        ByteArrayOutputStream stream = null;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();

        //image = graphics.getDeviceConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        //graphics.dispose();
        //graphics = image.createGraphics();
        graphics.setBackground(Constant.SPECTRUM_BACKGROUND_COLOR);
        graphics.clearRect(0, 0, width, height);
        graphics.setColor(Constant.SPECTRUM_FOREGROUND_COLOR);
        graphics.setStroke(new BasicStroke(1f));

        if (Constant.SPECTRUM_STYLE == 0) {//直接打印 PCM 或 FFT
            if (Constant.SPECTRUM_REAL_FFT) {//使用快速傅里叶变换(FFT)解码音频 PCM 默认不使用FFT(Constant.SPECTRUM_REAL_FFT = false)
                if (XMusic.deque.size() >= Constant.SPECTRUM_SAVE_INIT_SIZE) {
                    Double[] data = list2array(XMusic.deque);
                    for (int i = 0, length = data.length; i < length; i++) {
                        sheight = (int) Math.abs(data[i]);
                        graphics.fillRect(i * Constant.SPECTRUM_SPLIT_WIDTH, height - sheight, Constant.SPECTRUM_SPLIT_WIDTH, sheight);
                    }
                }
            } else {//直接打印 PCM
                if (XMusic.deque.size() >= Constant.SPECTRUM_SAVE_INIT_SIZE) {
                    for (int i = 0, len = XMusic.deque.size(); i < Constant.SPECTRUM_TOTAL_NUMBER; i++) {
                        try {
                            if (i < len) {
                                sheight = (int) Math.abs(Double.parseDouble(XMusic.deque.get(i) + ""));
                                sheight = Math.min(sheight, height);
                            }
                        } catch (Exception e) {
                            sheight = 0;
                        }
                        graphics.fillRect(i * Constant.SPECTRUM_SPLIT_WIDTH, height - sheight, Constant.SPECTRUM_SPLIT_WIDTH, sheight);
                        //graphics.fillRect(i*5, height/2-spectrum_height, 5, -spectrum_height);//双谱
                    }
                }
            }
            stream = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "png", stream);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
            inputStream = new ByteArrayInputStream(stream.toByteArray());
            spectrum.setBackgroundImage(new Image(null, new ImageData(inputStream).scaledTo(width, height)));
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else if (Constant.SPECTRUM_STYLE == 1) {//直接打印 PCM
            int indexs = 0;
            if (XMusic.deque.size() >= Constant.SPECTRUM_SAVE_INIT_SIZE) {
                for (int i = 0, len = XMusic.deque.size(); i < Constant.SPECTRUM_TOTAL_NUMBER; i++) {
                    try {
                        if (i < len) {
                            sheight = Math.abs(Integer.parseInt(XMusic.deque.get(i) + ""));
                            sheight = Math.min(sheight, height);
                        }
                    } catch (Exception e) {
                        sheight = 0;
                    }
                    int indexc = 10;
                    for (int j = 0; j < sheight; j = indexc) {
                        graphics.fillRect(indexs, height - indexc, Constant.SPECTRUM_SPLIT_WIDTH, 5);
                        indexc += 7;
                    }
                    indexs += 22;
                }
            }
            stream = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "png", stream);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
            inputStream = new ByteArrayInputStream(stream.toByteArray());
            spectrum.setBackgroundImage(new Image(null, new ImageData(inputStream).scaledTo(width, height)));
        }
        try {
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        graphics.dispose();
    }

    private Double[] list2array(List<Double> lists) {
        //Double[] c = Stream.of(lists).map(a->a.toString()).collect(Collectors.toList()).stream().map(b->Double.parseDouble(b)).toArray(Double[]::new);
        Double[] c = new Double[lists.size()];
        synchronized (lists) {
            for (int i = 0; i < lists.size(); i++) {
                try {
                    c[i] = Double.valueOf((lists.get(i) == null ? "0.0" : lists.get(i).toString()));
                } catch (Exception e) {
                    c[i] = 0.0;
                }
            }
        }
        Complex[] x = new Complex[c.length];
        for (int i = 0; i < x.length; i++) {
            try {
                x[i] = new Complex(c[i], 0);
            } catch (Exception e) {
                x[i] = new Complex(0, 0);
            }
        }
        return FFT.array(x);
    }

}
