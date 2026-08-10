package com.xu.music.player.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一次音频加载所拥有的全部资源和状态。
 */
final class PlaybackSession implements AutoCloseable {

    private final AudioInputStream audio;
    private final SourceDataLine line;
    private final AudioFormat format;
    private final PcmSpectrumAnalyzer analyzer;
    private final Object pauseMonitor = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean playing;
    private volatile boolean paused;
    private volatile Thread playbackThread;
    private volatile Thread spectrumThread;

    PlaybackSession(AudioInputStream audio, SourceDataLine line,
                    AudioFormat format, PcmSpectrumAnalyzer analyzer) {
        this.audio = audio;
        this.line = line;
        this.format = format;
        this.analyzer = analyzer;
    }

    void attachTasks(Thread playbackThread, Thread spectrumThread) {
        this.playbackThread = playbackThread;
        this.spectrumThread = spectrumThread;
    }

    void start() {
        if (closed.get()) {
            return;
        }
        analyzer.reset();
        playing = true;
        paused = false;
        line.start();
    }

    void pause() {
        if (!playing || paused) {
            return;
        }
        paused = true;
        line.stop();
    }

    void resume() {
        if (!playing || !paused) {
            return;
        }
        synchronized (pauseMonitor) {
            paused = false;
            line.start();
            pauseMonitor.notifyAll();
        }
    }

    void awaitIfPaused() throws InterruptedException {
        synchronized (pauseMonitor) {
            while (paused && playing) {
                pauseMonitor.wait();
            }
        }
    }

    void markStopped() {
        playing = false;
        synchronized (pauseMonitor) {
            paused = false;
            pauseMonitor.notifyAll();
        }
    }

    boolean playing() {
        return playing;
    }

    boolean paused() {
        return paused;
    }

    double positionSeconds() {
        return line.getLongFramePosition() / format.getFrameRate();
    }

    double durationSeconds() {
        var frames = audio.getFrameLength();
        return frames < 0 || format.getFrameRate() <= 0 ? 0 : frames / format.getFrameRate();
    }

    AudioInputStream audio() {
        return audio;
    }

    SourceDataLine line() {
        return line;
    }

    AudioFormat format() {
        return format;
    }

    PcmSpectrumAnalyzer analyzer() {
        return analyzer;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        markStopped();
        interrupt(playbackThread);
        interrupt(spectrumThread);
        try {
            line.stop();
        } catch (Exception ignored) {
            // 音频行可能已被系统关闭。
        }
        line.close();
        try {
            audio.close();
        } catch (IOException ignored) {
            // 关闭阶段没有可恢复操作。
        }
    }

    private static void interrupt(Thread thread) {
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }
}
