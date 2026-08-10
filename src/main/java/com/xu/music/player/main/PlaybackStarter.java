package com.xu.music.player.main;

import com.xu.music.player.hander.MusicPlayerError;
import com.xu.music.player.player.Player;

/**
 * 在失败时回收半初始化资源的播放启动动作。
 */
final class PlaybackStarter {

    private PlaybackStarter() {
    }

    static void start(Player player, String path) {
        try {
            player.load(path);
            player.play();
        } catch (Exception exception) {
            try {
                player.stop();
            } catch (RuntimeException stopException) {
                exception.addSuppressed(stopException);
            }
            throw new MusicPlayerError("无法播放音频: " + path, exception);
        }
    }
}
