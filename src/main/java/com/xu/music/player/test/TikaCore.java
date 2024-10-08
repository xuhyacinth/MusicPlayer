package com.xu.music.player.test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import java.util.Map;

public class TikaCore {

    public static void main(String[] args) throws Exception {

        // 获取音频文件格式
        Path path = Paths.get("song/梦涵 - 加减乘除.mp3");
        AudioFileFormat format = AudioSystem.getAudioFileFormat(path.toFile());

        // 获取音频文件元数据
        Map<String, Object> properties = format.properties();
        for (String key : properties.keySet()) {
            System.out.println(key + ": " + properties.get(key));
        }

    }

}
