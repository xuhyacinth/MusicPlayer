package com.xu.music.player.main;

/** 自动续播模式；每次启动默认顺序播放。 */
enum PlaybackMode {
    SEQUENTIAL("顺序播放"),
    RANDOM("随机播放"),
    REPEAT_ONE("单曲循环");

    private final String label;

    PlaybackMode(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
