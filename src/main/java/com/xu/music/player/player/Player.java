package com.xu.music.player.player;

import java.io.File;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * 音频播放
 *
 * @author hyacinth
 * @since 2024年6月4日19点07分
 * @version swt-java/v1.0.0
 */
public interface Player extends AutoCloseable {

    /** 源格式和当前 Java Sound 输出格式，仅描述应用输出，不保证系统位精确输出。 */
    default String audioFormatDescription() {
        return "";
    }
    /**
     * 加载音频
     *
     * @param url 音频文件url
     * @throws Exception 异常
     * @since 2019年10月31日19:06:39
     */
    void load(URL url) throws Exception;

    /**
     * 加载音频
     *
     * @param file 音频文件
     * @throws Exception 异常
     * @since 2019年10月31日19:06:39
     */
    void load(File file) throws Exception;

    /**
     * 加载音频
     *
     * @param path 文件路径
     * @throws Exception 异常
     * @since 2019年10月31日19:06:39
     */
    void load(String path) throws Exception;

    /**
     * 加载音频
     *
     * @param stream 音频文件输入流
     * @throws Exception 异常
     * @since 2019年10月31日19:06:39
     */
    void load(AudioInputStream stream) throws Exception;

    /**
     * 加载音频
     *
     * @param encoding Encoding
     * @param stream   AudioInputStream
     * @throws Exception 异常
     * @since 2019年10月31日19:06:39
     */
    void load(AudioFormat.Encoding encoding, AudioInputStream stream) throws Exception;

    /**
     * 加载音频
     *
     * @param format AudioFormat
     * @param stream AudioInputStream
     * @throws Exception 异常
     * @since 2019年10月31日19:06:39
     */
    void load(AudioFormat format, AudioInputStream stream) throws Exception;

    /**
     * 暂停播放
     *
     * @since 2019年10月31日19:06:39
     */
    void pause();

    /**
     * 请求跳转至指定秒数，保持当前播放或暂停状态。
     *
     * @return 是否接受跳转请求；不可重新打开的音频流不支持跳转
     */
    default boolean seek(double seconds) {
        return false;
    }

    /**
     * 继续播放
     *
     * @param duration 音频位置
     * @since 2019年10月31日19:06:39
     */
    void resume(long duration);

    /**
     * 开始播放
     *
     * @since 2019年10月31日19:06:39
     */
    void play();

    /**
     * 结束播放
     *
     * @since 2019年10月31日19:06:39
     */
    void stop();

    /**
     * 设置音频设备增益（分贝）
     *
     * @param volume 音量
     * @since 2019年10月31日19:06:39
     */
    void volume(float volume);

    /**
     * 设置播放音量，范围为 0～100，0 表示静音；切歌后保持当前设置。
     */
    void setVolume(int percentage);

    /**
     * 获取音频播放位置
     *
     * @return 播放位置（秒）
     * @since 2019年10月31日19:06:39
     */
    double position();

    /**
     * 获取音频总时长
     *
     * @return 音频总时长（秒）
     * @since 2019年10月31日19:06:39
     */
    double duration();

    /**
     * 是否正在播放
     *
     * @return 是否正在播放
     * @since 2019年10月31日19:06:39
     */
    boolean playing();

    /**
     * 是否正在
     *
     * @return 是否正在播放
     * @since 2019年10月31日19:06:39
     */
    boolean pausing();

    /**
     * 设置后续启动的播放会话自然播放结束时的回调。
     *
     * @param listener 回调；传入 {@code null} 时清除回调
     */
    default void onNaturalCompletion(Runnable listener) {
    }

    /**
     * 获取不可变语义的频谱快照。
     */
    default double[] spectrumSnapshot() {
        return new double[0];
    }

    /**
     * 释放播放器资源。
     */
    @Override
    default void close() {
        stop();
    }

}
