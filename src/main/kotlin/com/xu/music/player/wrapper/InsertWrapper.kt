package com.xu.music.player.wrapper

import com.xu.music.player.hander.DataBaseError
import com.xu.music.player.sql.SQLiteHelper

/** 歌曲等实体的插入封装。 */
class InsertWrapper<T : Any>(private val data: T, table: String) : BasicWrapper<T>() {
    init {
        if (table.isBlank()) throw DataBaseError("参数错误！")
        this.table = table
    }

    fun insert(): Int = try {
        SQLiteHelper().insert(insertSql(data))
    } catch (e: Exception) {
        throw DataBaseError(e.message, e)
    }
}
