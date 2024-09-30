package com.xu.music.player.test;

import javax.sound.sampled.*;

public class AudioPlaybackProgress {
    public static void main(String[] args) throws Exception {
        // 音频参数配置
        float sampleRate = 44100; // 采样率
        int numChannels = 1; // 单声道
        int sampleSizeInBits = 16; // 每个样本16位
        boolean signed = true;
        boolean bigEndian = false;

        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, numChannels, signed, bigEndian);
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();

        // 生成 440 Hz 的正弦波（2 秒钟）
        double frequency = 440;
        int durationInSeconds = 2;
        int numSamples = (int) (durationInSeconds * sampleRate);
        byte[] audioBuffer = new byte[numSamples * 2]; // 每个样本2字节（16位）

        for (int i = 0; i < numSamples; i++) {
            double time = i / sampleRate;
            double angle = 2.0 * Math.PI * frequency * time;
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE);
            audioBuffer[i * 2] = (byte) (sample & 0xff); // 低位
            audioBuffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff); // 高位
        }

        // 获取音频总时长
        double totalDuration = getAudioDuration(audioBuffer.length, sampleRate, numChannels, sampleSizeInBits);
        System.out.println("Audio Duration: " + totalDuration + " seconds");

        // 播放音频并监控进度
        new Thread(() -> {
            line.write(audioBuffer, 0, audioBuffer.length);
            line.drain();
            line.close();
        }).start();

        // 实时监控进度
        while (line.isOpen()) {
            long currentFramePosition = line.getLongFramePosition();
            double playedTime = currentFramePosition / sampleRate;
            System.out.println("Played: " + playedTime + " / " + totalDuration + " seconds");
            // 每100毫秒更新一次
            Thread.sleep(100);
        }
    }

    // 计算音频时长
    public static double getAudioDuration(int totalBytes, float sampleRate, int channels, int bitsPerSample) {
        return totalBytes / (sampleRate * channels * (bitsPerSample / 8.0));
    }
}

