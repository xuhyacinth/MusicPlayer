package com.xu.music.player.player;

import com.xu.music.player.modle.Controller;
import com.xu.music.player.modle.ControllerServer;
import com.xu.music.player.system.Constant;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;

public class XMusic implements Player {

    public static volatile LinkedList<Double> deque = new LinkedList<Double>();
    private static DataLine.Info info = null;
    private static AudioFormat format = null;
    private static SourceDataLine data = null;
    private static AudioInputStream stream = null;
    private static volatile boolean playing = false;
    private static Thread thread = null;

    private XMusic() {

    }

    /**
     * 静态内部内模式 创建播放器实例
     *
     * @return 播放器实例
     */
    public static XMusic player() {
        return Music.modle;
    }

    public static boolean isPlaying() {
        return playing;
    }

    @Override
    public void load(URL url) {
        try {
            end();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            stream = AudioSystem.getAudioInputStream(url);
            format = stream.getFormat();
            if (format.getEncoding().toString().toLowerCase().contains("mpeg")) {//mp3
                MpegAudioFileReader mp = new MpegAudioFileReader();
                stream = mp.getAudioInputStream(url);
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(format, stream);
            } else if (format.getEncoding().toString().toLowerCase().contains("flac")) {//flac
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            info = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
            data = (SourceDataLine) AudioSystem.getLine(info);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void load(File file) {
        try {
            end();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            stream = AudioSystem.getAudioInputStream(file);
            format = stream.getFormat();
            if (format.getEncoding().toString().toLowerCase().contains("mpeg")) {//mp3
                MpegAudioFileReader mp = new MpegAudioFileReader();
                stream = mp.getAudioInputStream(file);
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(format, stream);
            } else if (format.getEncoding().toString().toLowerCase().contains("flac")) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            info = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
            data = (SourceDataLine) AudioSystem.getLine(info);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void load(InputStream streams) {
        try {
            end();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        try {
            stream = AudioSystem.getAudioInputStream(streams);
            format = stream.getFormat();
            if (format.getEncoding().toString().toLowerCase().contains("mpeg")) {//mp3
                MpegAudioFileReader mp = new MpegAudioFileReader();
                stream = mp.getAudioInputStream(streams);
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(format, stream);
            } else if (format.getEncoding().toString().toLowerCase().contains("flac")) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(), format.getChannels() * 2, format.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            info = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
            data = (SourceDataLine) AudioSystem.getLine(info);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void load(String path) {
        try {
            end();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = new File(path);
        load(file);
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
    public void load(Encoding encoding, AudioInputStream stream) {
        // TODO Auto-generated method stub
    }

    @Override
    public void load(AudioFormat format, AudioInputStream stream) {

    }

    @SuppressWarnings("deprecation")
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
    public void start() {
        if (thread != null) {
            synchronized (thread) {
                thread.notify();
                data.start();
                playing = true;
            }
        } else {
            playing = true;
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
    public double length() {
        return Integer.parseInt(Constant.PLAYING_SONG_NAME.split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[3]);
    }

    @Override
    public boolean isOpen() {
        return data.isOpen();
    }

    @Override
    public boolean isAlive() {
        if (data != null && data.isOpen()) {
            return data.isActive();
        } else {
            return false;
        }
    }

    @Override
    public boolean isRuning() {
        if (data != null && data.isOpen()) {
            return data.isRunning();
        } else {
            return false;
        }
    }

    @Override
    public String info() {
        if (stream != null) {
            return stream.getFormat().toString();
        } else {
            return "";
        }
    }

    public void put(Double v) {
        synchronized (deque) {
            deque.add(v);
            if (deque.size() > Constant.SPECTRUM_TOTAL_NUMBER) {
                deque.removeFirst();
            }
        }
    }

    private static class Music {
        private static XMusic modle = new XMusic();
    }

}

