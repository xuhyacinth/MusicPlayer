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

    @FunctionalInterface
    interface AudioSource {
        AudioInputStream open() throws Exception;
    }

    private volatile AudioInputStream audio;
    private final AudioSource source;
    private final double duration;
    private Double pendingSeek;
    private Double seekingPosition;
    private double positionOffset;
    private long lineFrameOrigin;
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
        this(audio, line, format, analyzer, null);
    }

    PlaybackSession(AudioInputStream audio, SourceDataLine line,
                    AudioFormat format, PcmSpectrumAnalyzer analyzer, AudioSource source) {
        this.audio = audio;
        this.source = source;
        this.duration = SdlFftPlayer.getAudioDuration(audio, format);
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
        synchronized (pauseMonitor) {
            if (!playing || paused) {
                return;
            }
            paused = true;
            line.stop();
        }
    }

    void resume() {
        synchronized (pauseMonitor) {
            if (!playing || !paused) {
                return;
            }
            paused = false;
            if (seekingPosition == null) {
                line.start();
            }
            pauseMonitor.notifyAll();
        }
    }

    boolean requestSeek(double seconds) {
        synchronized (pauseMonitor) {
            if (!playing || source == null || !Double.isFinite(seconds)) {
                return false;
            }
            pendingSeek = Math.max(0, duration > 0 ? Math.min(seconds, duration) : seconds);
            pauseMonitor.notifyAll();
            return true;
        }
    }

    boolean hasPendingSeek() {
        synchronized (pauseMonitor) {
            return pendingSeek != null;
        }
    }

    // 在播放线程重新解码定位，避免阻塞界面，也支持向前回跳。
    void applyPendingSeek() throws Exception {
        double target;
        synchronized (pauseMonitor) {
            if (!playing || pendingSeek == null) {
                return;
            }
            target = pendingSeek;
            pendingSeek = null;
            seekingPosition = target;
            line.stop();
            line.flush();
        }

        AudioInputStream replacement = null;
        try {
            replacement = source.open();
            long targetFrames = (long) (target * format.getFrameRate());
            int frameSize = format.getFrameSize();
            byte[] buffer = new byte[Math.max(frameSize, 16384 / frameSize * frameSize)];
            long remaining = targetFrames * frameSize;
            long consumed = 0;
            while (remaining > 0) {
                if (!playing || hasPendingSeek() || Thread.currentThread().isInterrupted()) {
                    return;
                }
                int read = replacement.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    throw new IOException("音频解码未返回有效数据");
                }
                consumed += read;
                remaining -= read;
            }
            AudioInputStream previous;
            synchronized (pauseMonitor) {
                if (!playing || pendingSeek != null) {
                    return;
                }
                previous = audio;
                audio = replacement;
                replacement = null;
                positionOffset = (double) (consumed / frameSize) / format.getFrameRate();
                lineFrameOrigin = line.getLongFramePosition();
                analyzer.reset();
                seekingPosition = null;
                if (!paused) {
                    line.start();
                }
            }
            previous.close();
        } finally {
            synchronized (pauseMonitor) {
                seekingPosition = null;
            }
            if (replacement != null) {
                replacement.close();
            }
        }
    }

    // 只写入设备当前可接收的完整帧，暂停或跳转时不阻塞在音频写入中。
    void write(byte[] buffer, int length) throws InterruptedException {
        int offset = 0;
        int frameSize = format.getFrameSize();
        while (playing && offset < length) {
            awaitIfPaused();
            synchronized (pauseMonitor) {
                if (!playing || pendingSeek != null) {
                    return;
                }
                if (!paused) {
                    int writable = Math.min(length - offset, line.available());
                    writable -= writable % frameSize;
                    if (writable > 0) {
                        int written = line.write(buffer, offset, writable);
                        analyzer.accept(buffer, offset, written, format);
                        offset += written;
                    }
                }
            }
            if (offset < length) {
                Thread.sleep(5);
            }
        }
    }

    // 等待设备播完缓冲区时仍响应暂停和跳转，不把定位操作当作自然结束。
    boolean awaitPlaybackEnd() throws InterruptedException {
        while (playing) {
            awaitIfPaused();
            synchronized (pauseMonitor) {
                if (!playing || pendingSeek != null) {
                    return false;
                }
                if (!paused && line.available() >= line.getBufferSize()) {
                    playing = false;
                    return true;
                }
            }
            Thread.sleep(5);
        }
        return false;
    }

    void awaitIfPaused() throws InterruptedException {
        synchronized (pauseMonitor) {
            while (paused && playing && pendingSeek == null) {
                pauseMonitor.wait();
            }
        }
    }

    void markStopped() {
        synchronized (pauseMonitor) {
            playing = false;
            pendingSeek = null;
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
        synchronized (pauseMonitor) {
            if (pendingSeek != null) {
                return pendingSeek;
            }
            if (seekingPosition != null) {
                return seekingPosition;
            }
            return positionOffset + (line.getLongFramePosition() - lineFrameOrigin) / format.getFrameRate();
        }
    }

    double durationSeconds() {
        return duration;
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
