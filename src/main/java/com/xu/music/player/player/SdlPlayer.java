package com.xu.music.player.player;

import java.io.File;
import java.net.URL;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;

import com.xu.music.player.constant.Constant;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.hander.MusicPlayerError;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math3.transform.FastFourierTransformer;

/**
 * SourceDataLine 音频播放
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class SdlPlayer implements Player {

    /**
     * 频谱
     */
    public static final Deque<Double> SRC = new LinkedList<>();

    /**
     * 线程池
     */
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

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
     * 音频时长
     */
    private double duration = 0.0D;

    /**
     * 暂停
     */
    private volatile boolean paused = false;

    /**
     * 播放
     */
    private volatile boolean playing = false;

    private SdlPlayer() {
    }

    public static SdlPlayer create() {
        return SingletonHolder.PLAYER;
    }

    private static class SingletonHolder {
        private static final SdlPlayer PLAYER = new SdlPlayer();
    }

    @Override
    public void load(URL url) throws Exception {
        if (this.playing) {
            stop();
        }

        load(AudioSystem.getAudioInputStream(url));
    }

    @Override
    public void load(File file) throws Exception {
        if (this.playing) {
            stop();
        }

        if (!file.exists()) {
            throw new DataBaseError("File does not exist");
        }

        String name = file.getName();
        if (CharSequenceUtil.endWithIgnoreCase(name, ".mp3")) {
            AudioInputStream stream = new MpegAudioFileReader().getAudioInputStream(file);
            load(stream);
            return;
        }

        if (CharSequenceUtil.endWithIgnoreCase(name, ".flac")) {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            load(stream);
            return;
        }

        load(AudioSystem.getAudioInputStream(file));
    }

    @Override
    public void load(String path) throws Exception {
        if (this.playing) {
            stop();
        }

        load(new File(path));
    }

    @Override
    public void load(AudioInputStream stream) throws Exception {
        if (this.playing) {
            stop();
        }

        AudioFormat format = stream.getFormat();
        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(),
                format.getChannels() * 2, format.getSampleRate(), false);
        stream = AudioSystem.getAudioInputStream(format, stream);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, stream.getFormat(), AudioSystem.NOT_SPECIFIED);
        data = (SourceDataLine) AudioSystem.getLine(info);
        data.open(stream.getFormat());
        this.audio = stream;
    }

    @Override
    public void load(AudioFormat.Encoding encoding, AudioInputStream stream) throws Exception {
        if (this.playing) {
            stop();
        }

        load(AudioSystem.getAudioInputStream(encoding, stream));
    }

    @Override
    public void load(AudioFormat format, AudioInputStream stream) throws Exception {
        if (this.playing) {
            stop();
        }

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

    private void start() {
        try {
            this.playing = true;
            this.data.start();
            if (this.data.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                control = (FloatControl) this.data.getControl(FloatControl.Type.MASTER_GAIN);
            }

            this.duration = getAudioDuration(this.audio, this.audio.getFormat());
            byte[] buff = new byte[4];
            int channels = this.audio.getFormat().getChannels();
            float rate = this.audio.getFormat().getSampleRate();
            while (audio.read(buff) != -1 && playing) {
                synchronized (this) {
                    while (this.paused) {
                        wait();
                    }
                }
                setSpectrum(buff, channels, (int) rate);
                this.data.write(buff, 0, 4);
            }
            data.drain();
            data.stop();
        } catch (Exception e) {
            throw new MusicPlayerError(e.getMessage(), e);
        }
    }

    @Override
    public void play() throws Exception {
        if (this.playing) {
            return;
        }

        if (null == this.audio || null == this.data) {
            return;
        }

        EXECUTOR.submit(this::start);
    }

    @Override
    public void stop() {
        this.playing = false;
        if (null == this.audio || null == this.data) {
            return;
        }
        this.data.stop();
        IoUtil.close(this.audio);
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

    @Override
    public double position() {
        return data.getFramePosition();
    }

    @Override
    public double duration() {
        return this.duration;
    }

    /**
     * 计算音频时长
     *
     * @param audio  音频流
     * @param format 音频格式
     * @return 音频时长
     * @date 2019年10月31日19:06:39
     */
    public static double getAudioDuration(AudioInputStream audio, AudioFormat format) {
        return audio.getFrameLength() * format.getFrameSize() / (format.getSampleRate() * format.getChannels() * (format.getSampleSizeInBits() / 8.0));
    }

    private void setSpectrum(byte[] buff, int channels, int sample) {
        if (buff.length != 4) {
            return;
        }

        if (channels == 2) {
            // Stereo
            processStereoBuff(buff, sample);
            return;
        }

        // Mono
        processMonoBuff(buff, sample);
    }

    private void processStereoBuff(byte[] buff, int sample) {
        if (sample == 16) {
            // Left channel
            put(convert16(buff, 0));
            // Right channel
            put(convert16(buff, 2));
            return;
        }

        // Left channel
        put(convert8(buff[0]));
        // Right channel
        put(convert8(buff[1]));
    }

    private void processMonoBuff(byte[] buff, int sample) {
        if (sample == 16) {
            put(convert16(buff, 0));
            put(convert16(buff, 2));
            return;
        }

        for (byte b : buff) {
            put(b & 0xFF);
        }
    }

    private double convert16(byte[] buff, int offset) {
        return (short) ((buff[offset + 1] << 8) | (buff[offset] & 0xFF));
    }

    private double convert8(byte sample) {
        return (sample & 0xFF) / 128.0 - 1.0;
    }

    private void put(double value) {
        synchronized (SRC) {
            SRC.add(value);
            if (SRC.size() > Constant.SPECTRUM_TOTAL_NUMBER) {
                SRC.removeFirst();
            }
        }
    }

}