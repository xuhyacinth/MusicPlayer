package com.xu.music.player.test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SineWaveSynth {

    // 生成并播放一个正弦波音调
    public static void main(String[] args) throws LineUnavailableException {
        // 音频参数配置
        // 采样率，通常为44100 Hz
        float sampleRate = 44100;
        // 单声道
        int numChannels = 1;
        // 每个样本的位数，通常为16位
        int sampleSizeInBits = 16;
        // 是否有符号
        boolean signed = true;
        // 数据的字节顺序
        boolean bigEndian = false;

        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, numChannels, signed, bigEndian);

        // 获取并打开音频行
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();

        // 生成一个频率为440 Hz的正弦波，持续2秒
        // 频率，440Hz是A4音调
        double frequency = 440;
        // 音频持续时间
        int durationInSeconds = 2;
        int numSamples = (int) (durationInSeconds * sampleRate);
        // 每个样本2字节(16位)
        byte[] audioBuffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            // 正弦波公式：y(t) = A * sin(2 * pi * f * t)
            double time = i / sampleRate;
            double angle = 2.0 * Math.PI * frequency * time;
            // 振幅为最大16位短整型
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE);

            // 将样本值转换为字节，并存储到缓冲区中
            // 低字节
            audioBuffer[i * 2] = (byte) (sample & 0xff);
            // 高字节
            audioBuffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }

        // 播放生成的音频数据
        line.write(audioBuffer, 0, audioBuffer.length);

        // 等待音频播放结束
        line.drain();
        line.close();
    }
}
