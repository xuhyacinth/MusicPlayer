package com.xu.music.player.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface Player {

    /**
     * Java Music 加载音频
     *
     * @param url 音频文件url地址
     * @return boolean 是否加载成功
     * @Title: load
     * @Description: Java Music 加载音频
     * @date 2019年10月31日19:06:39
     */
    void load(URL url);

    /**
     * Java Music 加载音频
     *
     * @param file 音频文件
     * @return boolean 是否加载成功
     * @Title: load
     * @Description: Java Music 加载音频
     * @date 2019年10月31日19:06:39
     */
    void load(File file);

    /**
     * Java Music 加载音频
     *
     * @return boolean 是否加载成功
     * @Title: load
     * @Description: Java Music 加载音频
     * @date 2019年10月31日19:06:39
     */
    void load(String path);

    /**
     * Java Music 加载音频
     *
     * @param stream 音频文件输入流
     * @return boolean 是否加载成功
     * @Title: load
     * @Description: Java Music 加载音频
     * @date 2019年10月31日19:06:39
     */
    void load(InputStream stream);

    /**
     * Java Music 加载音频
     *
     * @param encoding Encoding
     * @param stream   AudioInputStream
     * @return boolean 是否加载成功
     * @Title: load
     * @Description: Java Music 加载音频
     * @date 2019年10月31日19:06:39
     */
    void load(Encoding encoding, AudioInputStream stream);

    /**
     * Java Music 加载音频
     *
     * @param stream AudioFormat
     * @param stream AudioInputStream
     * @return boolean 是否加载成功
     * @Title: load
     * @Description: Java Music 加载音频
     * @date 2019年10月31日19:06:39
     */
    void load(AudioFormat format, AudioInputStream stream);

    /**
     * Java Music 结束播放
     *
     * @throws IOException
     * @Title: end
     * @Description: Java Music 结束播放
     * @date 2019年10月31日19:06:39
     */
    void end() throws IOException;

    /**
     * Java Music 暂停播放
     *
     * @Title: stop
     * @Description: Java Music 结束播放
     * @date 2019年10月31日19:06:39
     */
    void stop();

    /**
     * Java Music 开始播放
     *
     * @throws Exception
     * @Title: start
     * @Description: Java Music 结束播放
     * @date 2019年10月31日19:06:39
     */
    void start() throws Exception;

    /**
     * Java Music 是否打开
     *
     * @return 是否打开
     * @Title: isOpen
     * @Description: Java Music 是否打开
     * @date 2019年10月31日19:06:39
     */
    boolean isOpen();

    /**
     * Java Music 是否运行
     *
     * @return 是否运行
     * @Title: isAlive
     * @Description: Java Music 是否运行
     * @date 2019年10月31日19:06:39
     */
    boolean isAlive();

    /**
     * Java Music 是否正在播放
     *
     * @return 是否正在播放
     * @Title: isRuning
     * @Description: Java Music 是否正在播放
     * @date 2019年10月31日19:06:39
     */
    boolean isRuning();

    /**
     * Java Music 音频信息
     *
     * @return 音频信息
     * @Title: info
     * @Description: Java Music 音频信息
     * @date 2019年10月31日19:06:39
     */
    String info();

    /**
     * Java Music 音频播放时长
     *
     * @return 音频播放时长
     * @Title: length
     * @Description: Java Music 音频播放时长
     * @date 2019年10月31日19:06:39
     */
    double length();

}
