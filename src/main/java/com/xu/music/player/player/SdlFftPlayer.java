package com.xu.music.player.player;

import cn.hutool.core.text.CharSequenceUtil;
import com.xu.music.player.constant.Constant;
import com.xu.music.player.hander.MusicPlayerError;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 SourceDataLine 和独立播放会话的音频播放器。
 */
public final class SdlFftPlayer implements Player {

    private static final Logger log = LoggerFactory.getLogger(SdlFftPlayer.class);

    private final PlaybackSessionSlot sessions = new PlaybackSessionSlot();
    private final PlaybackCompletionNotifier completionNotifier = new PlaybackCompletionNotifier();
    private final AtomicLong taskSequence = new AtomicLong();

    private SdlFftPlayer() {
    }

    public static SdlFftPlayer create() {
        return SingletonHolder.PLAYER;
    }

    private static class SingletonHolder {
        private static final SdlFftPlayer PLAYER = new SdlFftPlayer();
    }

    @Override
    public synchronized void load(URL url) throws Exception {
        stop();
        open(AudioSystem.getAudioInputStream(url));
    }

    @Override
    public synchronized void load(File file) throws Exception {
        stop();
        if (!file.isFile()) {
            throw new MusicPlayerError("音频文件不存在: " + file);
        }

        AudioInputStream stream;
        if (CharSequenceUtil.endWithIgnoreCase(file.getName(), ".mp3")) {
            stream = new MpegAudioFileReader().getAudioInputStream(file);
        } else {
            stream = AudioSystem.getAudioInputStream(file);
        }
        open(stream);
    }

    @Override
    public synchronized void load(String path) throws Exception {
        load(new File(path));
    }

    @Override
    public synchronized void load(AudioInputStream stream) throws Exception {
        stop();
        open(stream);
    }

    @Override
    public synchronized void load(AudioFormat.Encoding encoding, AudioInputStream stream) throws Exception {
        stop();
        open(AudioSystem.getAudioInputStream(encoding, stream));
    }

    @Override
    public synchronized void load(AudioFormat format, AudioInputStream stream) throws Exception {
        stop();
        open(AudioSystem.getAudioInputStream(format, stream));
    }

    private void open(AudioInputStream source) throws Exception {
        AudioInputStream pcm = null;
        SourceDataLine line = null;
        try {
            var sourceFormat = source.getFormat();
            var pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false);
            pcm = AudioSystem.getAudioInputStream(pcmFormat, source);
            var info = new DataLine.Info(SourceDataLine.class, pcmFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(pcmFormat);

            var analyzer = new PcmSpectrumAnalyzer(Constant.SPECTRUM_TOTAL_NUMBER);
            var session = new PlaybackSession(pcm, line, pcmFormat, analyzer);
            var previous = sessions.replace(session);
            if (previous != null) {
                previous.close();
            }
        } catch (Exception exception) {
            if (line != null) {
                line.close();
            }
            if (pcm != null) {
                pcm.close();
            } else {
                source.close();
            }
            throw exception;
        }
    }

    @Override
    public synchronized void play() {
        var session = sessions.current();
        if (session == null || session.playing()) {
            return;
        }

        var completionListener = completionNotifier.snapshot();
        var sequence = taskSequence.incrementAndGet();
        var playbackThread = Thread.ofVirtual()
                .name("music-playback-" + sequence)
                .unstarted(() -> runPlayback(session, completionListener));
        var spectrumThread = Thread.ofVirtual()
                .name("music-spectrum-" + sequence)
                .unstarted(() -> runSpectrum(session));
        session.attachTasks(playbackThread, spectrumThread);
        session.start();
        playbackThread.start();
        spectrumThread.start();
    }

    private void runPlayback(PlaybackSession session, Runnable completionListener) {
        var reachedEof = false;
        try {
            var format = session.format();
            var frameSize = format.getFrameSize();
            var bufferSize = Math.max(frameSize, 4096 / frameSize * frameSize);
            var buffer = new byte[bufferSize];

            while (session.playing() && !Thread.currentThread().isInterrupted()) {
                session.awaitIfPaused();
                if (!session.playing()) {
                    break;
                }

                var read = session.audio().read(buffer);
                if (read == -1) {
                    reachedEof = true;
                    session.line().drain();
                    break;
                }
                var alignedLength = read - read % frameSize;
                if (alignedLength == 0) {
                    continue;
                }
                session.analyzer().accept(buffer, 0, alignedLength, format);
                session.line().write(buffer, 0, alignedLength);
            }
        } catch (InterruptedException exception) {
            reachedEof = false;
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            reachedEof = false;
            if (session.playing()) {
                log.error("音频播放任务异常", exception);
            }
        } finally {
            session.markStopped();
            var completedCurrentSession = sessions.complete(session);
            session.close();
            try {
                completionNotifier.notifyIfNatural(completionListener, reachedEof, completedCurrentSession);
            } catch (RuntimeException exception) {
                log.error("Natural playback completion callback failed", exception);
            }
        }
    }

    private void runSpectrum(PlaybackSession session) {
        try {
            while (session.playing() && !Thread.currentThread().isInterrupted()) {
                if (!session.paused()) {
                    session.analyzer().updateSpectrum();
                }
                Thread.sleep(30);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            if (session.playing()) {
                log.error("频谱计算任务异常", exception);
            }
        }
    }

    @Override
    public void pause() {
        var session = sessions.current();
        if (session != null) {
            session.pause();
        }
    }

    @Override
    public void resume(long duration) {
        var session = sessions.current();
        if (session != null) {
            session.resume();
        }
    }

    @Override
    public synchronized void stop() {
        var session = sessions.detach();
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void volume(float volume) {
        var session = sessions.current();
        if (session == null || !session.line().isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        var control = (FloatControl) session.line().getControl(FloatControl.Type.MASTER_GAIN);
        if (volume >= control.getMinimum() && volume <= control.getMaximum()) {
            control.setValue(volume);
        }
    }

    @Override
    public double position() {
        var session = sessions.current();
        return session == null ? 0 : session.positionSeconds();
    }

    @Override
    public double duration() {
        var session = sessions.current();
        return session == null ? 0 : session.durationSeconds();
    }

    @Override
    public boolean playing() {
        var session = sessions.current();
        return session != null && session.playing();
    }

    @Override
    public boolean pausing() {
        var session = sessions.current();
        return session != null && session.paused();
    }

    @Override
    public void onNaturalCompletion(Runnable listener) {
        completionNotifier.setListener(listener);
    }

    @Override
    public double[] spectrumSnapshot() {
        var session = sessions.current();
        return session == null ? new double[0] : session.analyzer().spectrumSnapshot();
    }

    public static double getAudioDuration(AudioInputStream audio, AudioFormat format) {
        var frames = audio.getFrameLength();
        return frames < 0 || format.getFrameRate() <= 0 ? 0 : frames / format.getFrameRate();
    }
}
