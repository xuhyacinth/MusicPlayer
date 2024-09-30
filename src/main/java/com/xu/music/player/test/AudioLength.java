package com.xu.music.player.test;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioLength {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
        // 加载音频文件
        File audioFile = new File("song/Beyond - 长城（粤语）.flac");
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioStream.getFormat();

        // 获取音频总帧数
        long frameLength = audioStream.getFrameLength();
        System.out.println("Frame Length: " + frameLength);

        // 获取每秒的帧数（帧率）
        float frameRate = format.getFrameRate();
        System.out.println("Frame Rate: " + frameRate);

        // 检查是否返回无效值
        if (frameLength == AudioSystem.NOT_SPECIFIED || frameRate <= 0) {
            System.out.println("Unable to calculate duration due to invalid frame length or frame rate.");
        } else {
            // 计算总时长（秒）
            double durationInSeconds = frameLength / frameRate;
            System.out.println("Audio Duration: " + durationInSeconds + " seconds");
        }
    }

}
