package com.xu.music.player.player;

/**
 * @author Administrator
 */
public class Test {

    public static void main(String[] args) throws Exception {
        Player player = VPlayer.createPlayer();
        player.load("E:\\KuGou\\左宏元 - 情与法 (纯音乐).mp3");
        player.start();
    }

}
