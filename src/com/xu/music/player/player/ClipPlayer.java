package com.xu.music.player.player;

import cn.hutool.core.text.CharSequenceUtil;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.net.URL;

/**
 * 音频播放
 *
 * @date 2019年10月31日19:06:39
 * @since idea
 */
public class ClipPlayer implements NewPlayer {


    /**
     * Clip
     */
    private Clip clip;

    /**
     * FloatControl
     */
    private FloatControl control = null;

    /**
     * 播放位置
     */
    private volatile long position = 0;

    /**
     * 暂停
     */
    private volatile boolean paused = false;


    private ClipPlayer() {

    }

    public static ClipPlayer createPlayer() {
        return ClipPlayer.SingletonHolder.player;
    }

    private static class SingletonHolder {
        private static final ClipPlayer player = new ClipPlayer();
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
        DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), AudioSystem.NOT_SPECIFIED);

        this.clip = (Clip) AudioSystem.getLine(info);
        this.clip.addLineListener(event -> {
            System.out.println(event.getType() + "\t" + event.getFramePosition());
        });

        this.clip.open(stream);
        if (this.clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            control = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
        }
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
        if (this.clip != null && this.clip.isRunning()) {
            paused = true;
            position = this.clip.getMicrosecondPosition();
            this.clip.stop();
        }
    }

    @Override
    public void resume(long duration) {
        if (this.clip != null && paused) {
            paused = false;
            this.position = (0 == duration) ? position : duration;
            this.clip.setMicrosecondPosition(position);
            this.clip.start();
        }
    }

    @Override
    public void play() {
        if (this.clip != null) {
            this.clip.start();
            this.clip.drain();
        }
    }

    @Override
    public void stop() {
        if (this.clip != null) {
            this.clip.stop();
            this.clip.close();
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

}
