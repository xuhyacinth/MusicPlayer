package com.xu.music.player.hander;

/**
 * 数据库异常
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class DataBaseError extends RuntimeException {

    public DataBaseError(String msg) {
        super(msg);
    }

    public DataBaseError(Throwable cause) {
        super(cause);
    }

    public DataBaseError(String msg, Throwable cause) {
        super(msg, cause);
    }

}
