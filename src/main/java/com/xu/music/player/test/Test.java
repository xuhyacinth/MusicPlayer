package com.xu.music.player.test;

import cn.hutool.json.JSONUtil;
import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.player.SourceDataLinePlayer;
import com.xu.music.player.wrapper.QueryWrapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) throws Exception {
        SourceDataLinePlayer player = SourceDataLinePlayer.createPlayer();
        player.load("D:\\Kugou\\梦涵 - 加减乘除.mp3");
        player.play();
        System.out.println("---------");
        Thread.sleep(15000);
        player.pause();
        Thread.sleep(2000);
        player.resume(0);
    }

}
