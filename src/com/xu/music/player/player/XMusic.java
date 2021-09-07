package com.xu.music.player.player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import com.xu.music.player.modle.Controller;
import com.xu.music.player.modle.ControllerServer;
import com.xu.music.player.system.Constant;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

public class XMusic implements Player {

    public static volatile LinkedList<Double> deque = new LinkedList<>();
    private static Thread thread = null;
    private static DataLine.Info info = null;
    private static AudioFormat format = null;
    private static SourceDataLine data = null;
    private static AudioInputStream stream = null;
    private static volatile boolean playing = false;

    private XMusic() {
    }

    public static XMusic createPlayer() {
        return XMusic.SingletonHolder.player;
    }

    public static boolean isPlaying() {
        return playing;
    }

    @Override
    public void load(URL url) throws Exception {
        end();
        load(AudioSystem.getAudioInputStream(url));
    }

    @Override
    public void load(File file) throws Exception {
        end();
        loadFile(AudioSystem.getAudioInputStream(file));
    }

    @Override
    public void load(String path) throws Exception {
        end();
        load(new File(path));
    }

    @Override
    public void load(InputStream stream) throws Exception {
        end();
        loadFile(AudioSystem.getAudioInputStream(stream));
    }

    @Override
    public void load(AudioFormat.Encoding encoding, AudioInputStream stream) throws Exception {
        end();
        loadFile(AudioSystem.getAudioInputStream(encoding, stream));
    }

    @Override
    public void load(AudioFormat format, AudioInputStream stream) throws Exception {
        end();
        loadFile(AudioSystem.getAudioInputStream(format, stream));
    }

    @Override
    public void end() throws IOException {
        if (data != null) {
            data.stop();
            data.drain();
            stream.close();
            if (thread != null) {
                thread.stop();
                thread = null;
            }
            deque.clear();
        }
        playing = false;
    }

    @Override
    public void stop() {
        if (data != null && data.isOpen()) {
            data.stop();
            synchronized (thread) {
                try {
                    thread.wait(0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            playing = false;
        }
    }

    @Override
    public void start() throws Exception {
        if (thread != null) {
            synchronized (thread) {
                thread.notify();
                data.start();
                playing = true;
            }
        } else {
            playing = true;
            thread.setUncaughtExceptionHandler(new ExceptionHandler());
            thread = new Thread(() -> {
                try {
                    if (info != null) {
                        data.open(format);
                        data.start();
                        byte[] buf = new byte[4];
                        int channels = stream.getFormat().getChannels();
                        float rate = stream.getFormat().getSampleRate();
                        while (stream.read(buf) != -1 && playing) {
                            if (channels == 2) {//立体声
                                if (rate == 16) {
                                    put((double) ((buf[1] << 8) | buf[0]));//左声道
                                    //put((double) ((buf[3] << 8) | buf[2]));//右声道
                                } else {
                                    put((double) buf[1]);//左声道
                                    put((double) buf[3]);//左声道
                                    //put((double) buf[2]);//右声道
                                    //put((double) buf[4]);//右声道
                                }
                            } else {//单声道
                                if (rate == 16) {
                                    put((double) ((buf[1] << 8) | buf[0]));
                                    put((double) ((buf[3] << 8) | buf[2]));
                                } else {
                                    put((double) buf[0]);
                                    put((double) buf[1]);
                                    put((double) buf[2]);
                                    put((double) buf[3]);
                                }
                            }
                            data.write(buf, 0, 4);
                        }
                        new ControllerServer().endLyricPlayer(new Controller());// 结束歌词和频谱
                        System.out.println("解码器 结束歌词和频谱");
                        end();// 结束播放流
                        System.out.println("解码器 结束播放流");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public boolean isOpen() {
        return null != data && data.isOpen();
    }

    @Override
    public boolean isActive() {
        return null != data && data.isActive();
    }

    @Override
    public boolean isRuning() {
        return null != data && data.isRunning();
    }

    @Override
    public String info() {
        return null == data ? "" : data.getFormat().toString();
    }

    @Override
    public double length() {
        return Integer.parseInt(Constant.PLAYING_SONG_NAME.split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[4]);
    }

    public void put(Double v) {
        synchronized (deque) {
            deque.add(v);
            if (deque.size() > Constant.SPECTRUM_TOTAL_NUMBER) {
                deque.removeFirst();
            }
        }
    }

    private void loadFile(AudioInputStream stream) {
        try {
            format = stream.getFormat();
            if (format.getEncoding().toString().toLowerCase().contains("mpeg")) {//mp3
                MpegAudioFileReader mp = new MpegAudioFileReader();
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                XMusic.stream = AudioSystem.getAudioInputStream(format, stream);
            } else if (format.getEncoding().toString().toLowerCase().contains("flac")) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                XMusic.stream = AudioSystem.getAudioInputStream(format, stream);
            }
            info = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
            data = (SourceDataLine) AudioSystem.getLine(info);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static class SingletonHolder {
        private static final XMusic player = new XMusic();
    }

}

