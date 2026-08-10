package com.xu.music.player.hander

/**
 * 数据库异常
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
class DataBaseError : RuntimeException {

    constructor(msg: String?) : super(msg)

    constructor(cause: Throwable?) : super(cause)

    constructor(msg: String?, cause: Throwable?) : super(msg, cause)

}
