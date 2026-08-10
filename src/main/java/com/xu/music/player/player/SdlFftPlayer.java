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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * FFT 提取出的频域能量阵列结果（声明为基于并发的队列用于防由于截断数组引起的并发修改异常）
     */
    public static final Deque<Double> TRANS = new ConcurrentLinkedDeque<>();

    private final PcmSpectrumAnalyzer spectrumAnalyzer =
            new PcmSpectrumAnalyzer(Constant.SPECTRUM_TOTAL_NUMBER);

    /**
     * 线程池 (采用 JDK 21 虚拟线程以极致优化异步开销)
     */
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

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
        stop();
        load(AudioSystem.getAudioInputStream(url));
    }

    @Override
    public void load(File file) throws Exception {
        stop();
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
        stop();
        load(new File(path));
    }

    /**
     * 加载处理 AudioInputStream 开始构建 SourceDataLine
     * @param stream AudioInputStream 音频流
     */
    public void load(AudioInputStream stream) throws Exception {
        stop();

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
        stop();
        load(AudioSystem.getAudioInputStream(encoding, stream));
    }

    @Override
    public void load(AudioFormat format, AudioInputStream stream) throws Exception {
        stop();
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
            this.data.start();
            if (this.data.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                control = (FloatControl) this.data.getControl(FloatControl.Type.MASTER_GAIN);
            }

            this.duration = getAudioDuration(this.audio, this.audio.getFormat());
            var format = this.audio.getFormat();
            var frameSize = format.getFrameSize();
            var bufferSize = Math.max(frameSize, 4096 / frameSize * frameSize);
            var buffer = new byte[bufferSize];
            int read;
            while (playing && (read = audio.read(buffer)) != -1) {
                synchronized (this) {
                    while (this.paused) {
                        wait();
                    }
                }
                spectrumAnalyzer.accept(buffer, 0, read, format);
                this.data.write(buffer, 0, read);
            }
            data.drain();
            data.stop();
            this.playing = false; // 播放结束更新状态，使 FFT 后台任务正常退出
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

        spectrumAnalyzer.reset();

        this.playing = true;
        EXECUTOR.submit(this::start);
        startFftTask();
    }

    private void startFftTask() {
        EXECUTOR.submit(() -> {
            while (playing) {
                if (paused) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                try {
                    spectrumAnalyzer.updateSpectrum();
                    var snapshot = spectrumAnalyzer.spectrumSnapshot();
                    TRANS.clear();
                    for (var magnitude : snapshot) {
                        TRANS.add(magnitude);
                    }
                } catch (Exception e) {
                    log.error("FFT 计算异常", e);
                }

                try {
                    Thread.sleep(30); // 约 33 Hz 的刷新频率，平衡流畅度与 CPU 负载
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    @Override
    public void stop() {
        this.playing = false;
        if (this.data != null) {
            try {
                this.data.stop();
            } catch (Exception e) {
                // 忽略停止时的异常
            }
            IoUtil.close(this.data);
            this.data = null;
        }
        if (this.audio != null) {
            IoUtil.close(this.audio);
            this.audio = null;
        }
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
        return data != null ? data.getFramePosition() : 0;
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

}
