package com.xu.music.player.test;

import com.xu.music.player.player.SdlPlayer;

public class Test {

    public static void main(String[] args) throws Exception {
        SdlPlayer player = SdlPlayer.createPlayer();
        player.load("D:\\Kugou\\KugouMusic\\梦涵 - 加减乘除.mp3");
        player.play();
        Thread.sleep(1000);
        player.stop();
        player.load("D:\\Kugou\\KugouMusic\\张敬轩 - 酷爱.mp3");
        player.play();
        System.out.println("---------");
//        Thread.sleep(15000);
//        player.pause();
//        Thread.sleep(2000);
//        player.resume(0);
//        player.stop();
    }

}
