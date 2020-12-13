package com.xu.music.player.player;

public class Test {

    public static void main(String[] args) throws Exception {
        Player player = XMusic.player();
        player.load("C:\\Users\\Administrator\\Desktop\\梦涵 - 加减乘除.mp3");
        System.out.println(player.info());
        player.start();
        System.out.println(123);
    }

}
