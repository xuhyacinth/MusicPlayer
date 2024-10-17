package com.xu.music.player.test;

import com.xu.music.player.player.SdlPlayer;

public class Test {

    public static void main(String[] args) throws Exception {
        SdlPlayer player = SdlPlayer.create();
        player.load("song/Beyond - 长城（粤语）.flac");
        player.play();
        System.out.println("============");
//        Thread.sleep(15000);
//        player.pause();
//        Thread.sleep(2000);
//        player.resume(0);
//        player.stop();
    }

}
