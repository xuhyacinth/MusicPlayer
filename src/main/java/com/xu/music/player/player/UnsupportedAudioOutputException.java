package com.xu.music.player.player;

/** 输出通道不支持原始格式，由界面决定是否请求兼容播放。 */
public final class UnsupportedAudioOutputException extends Exception {
    private final boolean compatibilityAvailable;

    UnsupportedAudioOutputException(String message, boolean compatibilityAvailable) {
        super(message);
        this.compatibilityAvailable = compatibilityAvailable;
    }

    public boolean compatibilityAvailable() {
        return compatibilityAvailable;
    }
}
