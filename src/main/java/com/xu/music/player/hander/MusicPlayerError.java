package com.xu.music.player.hander;

/**
 * 播放异常
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class MusicPlayerError extends RuntimeException {

    public MusicPlayerError(String msg) {
        super(msg);
    }

    public MusicPlayerError(Throwable cause) {
        super(cause);
    }

    public MusicPlayerError(String msg, Throwable cause) {
        super(msg, cause);
    }

}
