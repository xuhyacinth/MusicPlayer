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

/**
 * @author hyacinth
 */
public class VPlayer implements Player {

    private Thread thread = null;
    private DataLine.Info info = null;
    private AudioFormat format = null;
    private SourceDataLine line = null;
    private AudioInputStream audio = null;
    private volatile boolean playing = false;
    public volatile LinkedList<Double> deque = new LinkedList<>();

    private VPlayer() {
    }

    public static VPlayer createPlayer() {
        return SingletonHolder.player;
    }

    @Override
    public void load(URL url) throws Exception {
        end();
        load(AudioSystem.getAudioInputStream(url));
    }

    @Override
    public void load(File file) throws Exception {
        end();
        String name = file.getName();
        if (Audio.isSupport(name)) {
            if (Audio.getIndex(name) == Audio.MP3.getIndex()) {
                MpegAudioFileReader reader = new MpegAudioFileReader();
                loadAudio(reader.getAudioInputStream(file));
            } else {
                loadAudio(AudioSystem.getAudioInputStream(file));
            }
        }
    }

    @Override
    public void load(String path) throws Exception {
        end();
        load(new File(path));
    }

    @Override
    public void load(InputStream stream) throws Exception {
        end();
        loadAudio(AudioSystem.getAudioInputStream(stream));
    }

    @Override
    public void load(AudioFormat.Encoding encoding, AudioInputStream stream) throws Exception {
        end();
        loadAudio(AudioSystem.getAudioInputStream(encoding, stream));
    }

    @Override
    public void load(AudioFormat format, AudioInputStream stream) throws Exception {
        end();
        loadAudio(AudioSystem.getAudioInputStream(format, stream));
    }

    private void loadAudio(AudioInputStream stream) {
        try {
            format = stream.getFormat();
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(),
                    format.getChannels() * 2, format.getSampleRate(), false);
            audio = AudioSystem.getAudioInputStream(format, stream);
            info = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(info);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void end() throws IOException {
        if (line != null) {
            line.stop();
            line.drain();
            audio.close();
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
        if (line != null && line.isOpen()) {
            line.stop();
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
                line.start();
                playing = true;
            }
        } else {
            playing = true;
            thread = new Thread(() -> {
                try {
                    if (info != null) {
                        line.open(format);
                        line.start();
                        byte[] buf = new byte[4];
                        int channels = audio.getFormat().getChannels();
                        float rate = audio.getFormat().getSampleRate();
                        while (audio.read(buf) != -1 && playing) {
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
                            line.write(buf, 0, 4);
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
        return null != line && line.isOpen();
    }

    @Override
    public boolean isActive() {
        return null != line && line.isActive();
    }

    @Override
    public boolean isRuning() {
        return null != line && line.isRunning();
    }

    @Override
    public String info() {
        return null == line ? "" : line.getFormat().toString();
    }

    @Override
    public double length() {
        return 0;
    }

    public void put(Double v) {
        synchronized (deque) {
            deque.add(v);
            if (deque.size() > Constant.SPECTRUM_TOTAL_NUMBER) {
                deque.removeFirst();
            }
        }
    }

    private static class SingletonHolder {
        private static final VPlayer player = new VPlayer();
    }

    public static void main(String[] args) throws Exception {
        Player player = VPlayer.createPlayer();
        player.load("E:\\KuGou\\不才 - 化身孤岛的鲸(1).flac");
        player.start();
    }

}
