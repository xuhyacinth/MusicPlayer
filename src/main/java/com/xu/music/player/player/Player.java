package com.xu.music.player.player;

import java.io.File;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * 音频播放
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public interface Player {

    /**
     * 加载音频
     *
     * @param url 音频文件url
     * @throws Exception 异常
     * @date 2019年10月31日19:06:39
     */
    void load(URL url) throws Exception;

    /**
     * 加载音频
     *
     * @param file 音频文件
     * @throws Exception 异常
     * @date 2019年10月31日19:06:39
     */
    void load(File file) throws Exception;

    /**
     * 加载音频
     *
     * @param path 文件路径
     * @throws Exception 异常
     * @date 2019年10月31日19:06:39
     */
    void load(String path) throws Exception;

    /**
     * 加载音频
     *
     * @param stream 音频文件输入流
     * @throws Exception 异常
     * @date 2019年10月31日19:06:39
     */
    void load(AudioInputStream stream) throws Exception;

    /**
     * 加载音频
     *
     * @param encoding Encoding
     * @param stream   AudioInputStream
     * @throws Exception 异常
     * @date 2019年10月31日19:06:39
     */
    void load(AudioFormat.Encoding encoding, AudioInputStream stream) throws Exception;

    /**
     * 加载音频
     *
     * @param format AudioFormat
     * @param stream AudioInputStream
     * @throws Exception 异常
     * @date 2019年10月31日19:06:39
     */
    void load(AudioFormat format, AudioInputStream stream) throws Exception;

    /**
     * 暂停播放
     *
     * @date 2019年10月31日19:06:39
     */
    void pause();

    /**
     * 继续播放
     *
     * @param duration 音频位置
     * @date 2019年10月31日19:06:39
     */
    void resume(long duration);

    /**
     * 开始播放
     *
     * @throws Exception 异常
     * @date 2019年10月31日19:06:39
     */
    void play() throws Exception;

    /**
     * 结束播放
     *
     * @date 2019年10月31日19:06:39
     */
    void stop();

    /**
     * 设置音量
     *
     * @param volume 音量
     * @date 2019年10月31日19:06:39
     */
    void volume(float volume);

    /**
     * 获取音频播放位置
     *
     * @return 播放位置
     * @date 2019年10月31日19:06:39
     */
    double position();

    /**
     * 获取音频总时长
     *
     * @return 音频总时长
     * @date 2019年10月31日19:06:39
     */
    double duration();

}
