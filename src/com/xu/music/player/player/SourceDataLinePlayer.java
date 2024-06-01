package com.xu.music.player.player;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.xu.music.player.system.Constant;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.net.URL;
import java.util.Deque;
import java.util.LinkedList;

/**
 * 音频播放
 *
 * @date 2019年10月31日19:06:39
 * @since idea
 */
public class SourceDataLinePlayer implements NewPlayer {

    /**
     * SourceDataLine
     */
    private SourceDataLine data = null;


    /**
     * AudioInputStream
     */
    private AudioInputStream audio = null;

    /**
     * FloatControl
     */
    private FloatControl control = null;

    /**
     * 暂停
     */
    private volatile boolean paused = false;

    /**
     * 播放
     */
    private volatile boolean playing = false;

    /**
     * 频谱
     */
    public static final Deque<Double> deque = new LinkedList<>();

    public void put(Double v) {
        synchronized (deque) {
            deque.add(v);
            if (deque.size() > Constant.SPECTRUM_TOTAL_NUMBER) {
                deque.removeFirst();
            }
        }
    }

    private SourceDataLinePlayer() {
    }

    public static SourceDataLinePlayer createPlayer() {
        return SourceDataLinePlayer.SingletonHolder.player;
    }

    private static class SingletonHolder {
        private static final SourceDataLinePlayer player = new SourceDataLinePlayer();
    }

    @Override
    public void load(URL url) throws Exception {
        load(AudioSystem.getAudioInputStream(url));
    }

    @Override
    public void load(File file) throws Exception {
        String name = file.getName();
        if (CharSequenceUtil.endWithIgnoreCase(name, ".mp3")) {
            AudioInputStream stream = new MpegAudioFileReader().getAudioInputStream(file);

            AudioFormat format = stream.getFormat();
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(),
                    format.getChannels() * 2, format.getSampleRate(), false);

            stream = AudioSystem.getAudioInputStream(format, stream);
            load(stream);
            return;
        }
        if (CharSequenceUtil.endWithIgnoreCase(name, ".flac")) {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);

            AudioFormat format = stream.getFormat();
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(),
                    format.getChannels() * 2, format.getSampleRate(), false);

            stream = AudioSystem.getAudioInputStream(format, stream);

            load(stream);
            return;
        }
        load(AudioSystem.getAudioInputStream(file));
    }

    @Override
    public void load(String path) throws Exception {
        load(new File(path));
    }

    @Override
    public void load(AudioInputStream stream) throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat(), AudioSystem.NOT_SPECIFIED);
        data = (SourceDataLine) AudioSystem.getLine(info);
        data.open(stream.getFormat());
        this.audio = stream;
    }

    @Override
    public void load(AudioFormat.Encoding encoding, AudioInputStream stream) throws Exception {
        load(AudioSystem.getAudioInputStream(encoding, stream));
    }

    @Override
    public void load(AudioFormat format, AudioInputStream stream) throws Exception {
        load(AudioSystem.getAudioInputStream(format, stream));
    }

    @Override
    public void pause() {
        this.paused = true;
    }

    @Override
    public void resume(long duration) {
        this.paused = false;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void play() throws Exception {
        if (null == this.audio || null == this.data) {
            return;
        }
        this.playing = true;
        this.data.start();
        if (this.data.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            control = (FloatControl) this.data.getControl(FloatControl.Type.MASTER_GAIN);
        }

        byte[] buff = new byte[4];
        int channels = this.audio.getFormat().getChannels();
        float rate = this.audio.getFormat().getSampleRate();

        while (audio.read(buff) != -1 && playing) {
            synchronized (this) {
                while (this.paused) {
                    this.data.flush();
                    wait();
                }
            }
            if (channels == 2) { // 立体声
                if (rate == 16) {
                    put((double) ((buff[1] << 8) | buff[0] & 0xFF)); // 左声道
                    put((double) ((buff[3] << 8) | buff[2] & 0xFF)); // 右声道
                } else {
                    put((double) buff[0]); // 左声道
                    put((double) buff[2]); // 左声道
                    put((double) buff[1]); // 右声道
                    put((double) buff[3]); // 右声道
                }
            } else { // 单声道
                if (rate == 16) {
                    put((double) ((buff[1] << 8) | buff[0] & 0xFF));
                    put((double) ((buff[3] << 8) | buff[2] & 0xFF));
                } else {
                    put((double) buff[0]);
                    put((double) buff[1]);
                    put((double) buff[2]);
                    put((double) buff[3]);
                }
            }
            this.data.write(buff, 0, 4);
        }
        data.drain();
        data.stop();
    }

    @Override
    public void stop() {
        if (null == this.audio || null == this.data) {
            return;
        }
        this.playing = false;
        IoUtil.close(this.audio);
        this.data.stop();
        IoUtil.close(this.data);
    }

    @Override
    public void volume(float volume) {
        if (null == control) {
            return;
        }
        if (volume < control.getMinimum() || volume > control.getMaximum()) {
            return;
        }
        control.setValue(volume);
    }

}
