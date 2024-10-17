package com.xu.music.player.hander

/**
 * 数据库异常
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class DataBaseError : RuntimeException {

    constructor(msg: String?) : super(msg)

    constructor(cause: Throwable?) : super(cause)

    constructor(msg: String?, cause: Throwable?) : super(msg, cause)

}
