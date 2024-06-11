package com.xu.music.player.test;

import com.xu.music.player.player.SourceDataLinePlayer;

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
        player.stop();
    }

}
