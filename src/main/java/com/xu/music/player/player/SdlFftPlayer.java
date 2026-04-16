package com.xu.music.player.player;

import java.io.File;
import java.net.URL;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;

import com.xu.music.player.constant.Constant;
import com.xu.music.player.hander.DataBaseError;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * SourceDataLine 音频播放
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class SdlFftPlayer implements Player {

    private static final Logger log = LoggerFactory.getLogger(SdlFftPlayer.class);

    /**
     * 缓存的原始音频采样源数据
     */
    protected static final Deque<Double> SRC = new LinkedList<>();

    /**
     * FFT 提取出的频域能量阵列结果（声明为基于并发的队列用于防由于截断数组引起的并发修改异常）
     */
    public static final Deque<Double> TRANS = new ConcurrentLinkedDeque<>();

    /**
     * 线程池 (采用 JDK 21 虚拟线程以极致优化异步开销)
     */
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * FFT
     */
    private final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

    /**
     * SourceDataLine
     */
    private SourceDataLine data = null;

    /**
     * AudioInputStream
     */
    private AudioInputStream audio = null;

    /**
     * 音频时长
     */
    private double duration = 0.0D;

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

    private SdlFftPlayer() {
    }

    public static SdlFftPlayer create() {
        return SingletonHolder.PLAYER;
    }

    private static class SingletonHolder {
        private static final SdlFftPlayer PLAYER = new SdlFftPlayer();
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

    /**
     * 加载处理 AudioInputStream 开始构建 SourceDataLine
     * @param stream AudioInputStream 音频流
     */
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
            log.error("SdlFftPlayer 播放异常！", e);
        }
    }

    @Override
    public void play() {
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

    @Override
    public boolean playing() {
        return this.playing;
    }

    @Override
    public boolean pausing() {
        return this.paused;
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

        // Stereo
        if (channels == 2) {
            if (sample == 16) {
                short left = (short) ((buff[1] << 8) | (buff[0] & 0xFF));
                short right = (short) ((buff[3] << 8) | (buff[2] & 0xFF));
                putSrc((left + right) / 2.0 / 32768.0);
                return;
            }

            // Assuming 8-bit samples
            double left = (buff[0] & 0xFF) / 128.0 - 1.0;
            double right = (buff[1] & 0xFF) / 128.0 - 1.0;
            putSrc((left + right) / 2.0);
            return;
        }

        // Mono
        if (sample == 16) {
            putSrc((short) ((buff[1] << 8) | (buff[0] & 0xFF)) / 32768.0);
            return;
        }

        // Assuming 8-bit samples
        putSrc((buff[0] & 0xFF) / 128.0 - 1.0);
    }

    /**
     * FFT采样拦截计数器，用于控制进行傅里叶计算的频率，避免高负载
     */
    private int sampleCount = 0;

    /**
     * 追加源数据样本。
     * 当收到足够的样本之后将触发一次计算密集型的 FFT 计算。采用 50% 步进滑动窗口算法大大减消资源开销。
     *
     * @param value PCM 数据双精度采样值
     */
    private void putSrc(double value) {
        synchronized (SRC) {
            SRC.add(value);
            // 维持固定大小的采样窗口
            if (SRC.size() > Constant.SPECTRUM_TOTAL_NUMBER) {
                SRC.removeFirst();
            }
            sampleCount++;

            // 满编，且更新的数据点积累达到容量的一半（交叠步进距离为 1/2）才执行一次 FFT。
            // 例如大小 128 时每 64 个样本点才做 1 遍计算，可削减大量的 CPU 无意义自耗
            if (SRC.size() == Constant.SPECTRUM_TOTAL_NUMBER && sampleCount >= Constant.SPECTRUM_TOTAL_NUMBER / 2) {
                sampleCount = 0;
                // 执行短时快速傅里叶变换，提取其频域上各点幅度特征
                Complex[] complex = fft.transform(SRC.stream().mapToDouble(Double::doubleValue).toArray(), TransformType.FORWARD);

                // 生成一帧全新的渲染列，并原子替换
                Deque<Double> tempTrans = new LinkedList<>();
                for (int i = 0; i < Constant.SPECTRUM_TOTAL_NUMBER; i++) {
                    tempTrans.add(complex[i].abs());
                }
                TRANS.clear();
                TRANS.addAll(tempTrans);
            }
        }
    }

}