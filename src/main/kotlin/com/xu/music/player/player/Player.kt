package com.xu.music.player.player

import java.io.File
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding
import javax.sound.sampled.AudioInputStream

/**
 * 音频播放
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
interface Player {

    /**
     * 加载音频
     *
     * @param url 音频文件url
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    fun load(url: URL?)

    /**
     * 加载音频
     *
     * @param file 音频文件
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    fun load(file: File?)

    /**
     * 加载音频
     *
     * @param path 文件路径
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    fun load(path: String?)

    /**
     * 加载音频
     *
     * @param stream 音频文件输入流
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    fun load(stream: AudioInputStream?)

    /**
     * 加载音频
     *
     * @param encoding Encoding
     * @param stream   AudioInputStream
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    fun load(encoding: Encoding?, stream: AudioInputStream?)

    /**
     * 加载音频
     *
     * @param format AudioFormat
     * @param stream AudioInputStream
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    fun load(format: AudioFormat?, stream: AudioInputStream?)

    /**
     * 暂停播放
     *
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun pause()

    /**
     * 继续播放
     *
     * @param duration 音频位置
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun resume(duration: Long)

    /**
     * 开始播放
     *
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    fun play()

    /**
     * 结束播放
     *
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun stop()

    /**
     * 设置音量
     *
     * @param volume 音量
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    fun volume(volume: Float)
}
