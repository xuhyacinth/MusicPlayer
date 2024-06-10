package com.xu.music.player.sql

import cn.hutool.core.collection.CollUtil
import cn.hutool.core.io.IoUtil
import cn.hutool.core.map.MapUtil
import cn.hutool.core.util.ArrayUtil
import cn.hutool.core.util.StrUtil
import com.xu.music.player.hander.DataBaseError
import com.xu.music.player.utils.CommUtils
import com.xu.music.player.utils.SysType
import java.io.BufferedInputStream
import java.io.File
import java.lang.reflect.Field
import java.sql.Blob
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*
import java.util.regex.Pattern

/**
 * 数据库操作
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
class SQLiteHelper : Helper {

    override val conn: Connection
        get() {
            try {
                return DriverManager.getConnection("jdbc:sqlite:" + DATABASE)
            } catch (e: SQLException) {
                throw DataBaseError(e.message)
            }
        }

    override fun insert(sql: String?, vararg params: Any?): Int {
        return update(sql, *params)
    }

    override fun update(sql: String?, vararg params: Any?): Int {
        try {
            val conn = this.conn
            if (ArrayUtil.isEmpty(params)) {
                try {
                    conn.createStatement().use { state ->
                        return state.executeUpdate(sql)
                    }
                } catch (e: SQLException) {
                    conn.rollback()
                } finally {
                    IoUtil.close(conn)
                }
            }
            try {
                conn.prepareStatement(sql).use { state ->
                    setValues(state, *params)
                    return state.executeUpdate(sql)
                }
            } catch (e: SQLException) {
                conn.rollback()
            } finally {
                IoUtil.close(conn)
            }
        } catch (e: Exception) {
            throw DataBaseError(e.message)
        }
        return 0
    }

    override fun delete(sql: String?, vararg para: Any?): Int {
        return insert(sql, *para)
    }

    override fun <T> select(sql: String?, cls: Class<T>?, vararg params: Any?): List<T> {
        val data = select(sql, *params)
        if (data.isEmpty()) {
            return emptyList()
        }
        return data.mapNotNull { convert(it, cls) }
    }

    override fun select(sql: String?, vararg params: Any?): List<Map<String?, Any?>?> {
        val list: MutableList<Map<String?, Any?>?> = ArrayList()
        val conn = this.conn
        try {
            conn.prepareStatement(sql).use { state ->
                this.setValues(state, *params)
                val result = state.executeQuery()
                val data = result.metaData

                val len = data.columnCount
                val col = arrayOfNulls<String>(len)
                for (i in 0 until len) {
                    col[i] = data.getColumnName(i + 1)
                }

                while (result.next()) {
                    list.add(setValue(result, col, len))
                }
                IoUtil.close(result)
            }
        } catch (e: Exception) {
            throw DataBaseError(e.message)
        } finally {
            IoUtil.close(conn)
        }
        return list
    }

    /**
     * 设置预编译数据
     *
     * @param state 预编译对象
     * @param obj   预编译数据
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    private fun setValues(state: PreparedStatement?, vararg obj: Any?) {
        if (null == state || ArrayUtil.isEmpty(obj)) {
            return
        }
        setValues(state, listOf(*obj))
    }

    /**
     * 设置预编译数据
     *
     * @param state  预编译对象
     * @param params 预编译数据
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    private fun setValues(state: PreparedStatement?, params: List<Any?>) {
        if (null == state || CollUtil.isEmpty(params)) {
            return
        }
        try {
            for (i in params.indices) {
                val `object` = params[i]
                if (null == `object`) {
                    state.setString(i + 1, "")
                    continue
                }
                val type = `object`.javaClass.simpleName
                when (type) {
                    "SerialBlob" -> state.setBlob(i + 1, params[i] as Blob?)
                    "Integer" -> state.setInt(i + 1, `object`.toString().toInt())
                    "Double" -> state.setDouble(i + 1, `object`.toString().toDouble())
                    "Float" -> state.setFloat(i + 1, `object`.toString().toFloat())
                    "Long" -> state.setLong(i + 1, `object`.toString().toLong())
                    "Short" -> state.setShort(i + 1, `object`.toString().toShort())
                    "Timestamp" -> state.setTimestamp(i + 1, params[i] as Timestamp?)
                    else -> state.setString(i + 1, `object`.toString())
                }
            }
        } catch (e: SQLException) {
            throw DataBaseError(e.message)
        }
    }

    /**
     * 获取值
     *
     * @param map   值
     * @param field 字段
     * @return 结果
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    private fun getValue(map: Map<String?, Any?>?, field: Field): Any? {
        if (MapUtil.isEmpty(map)) {
            return null
        }
        for ((key, value) in map!!) {
            if (StrUtil.equalsAnyIgnoreCase(key, field.name, underline(field.name))) {
                return value
            }
        }
        return null
    }

    /**
     * 转换
     *
     * @param map 值
     * @param cls 类
     * @param <T> 泛型
     * @return 结果
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    private fun <T> convert(map: Map<String?, Any?>?, cls: Class<T>?): T? {
        if (MapUtil.isEmpty(map) || null == cls) {
            return null
        }
        try {
            val fields = cls.declaredFields
            val t = cls.getDeclaredConstructor().newInstance()
            for (field in fields) {
                val `object` = getValue(map, field) ?: continue
                field.isAccessible = true
                val name = field.type.simpleName
                if (StrUtil.equalsAny(name, "Date", "LocalDate", "LocalTime", "LocalDateTime")) {
                    field[t] = CommUtils.formatDateTime(`object`)
                } else {
                    field[t] = `object`
                }
            }
            return t
        } catch (e: Exception) {
            throw DataBaseError(e.message)
        }
    }

    /**
     * Blob 转字节数组
     *
     * @param blob Blob
     * @return 字节数组
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    private fun blob2byte(blob: Blob?): ByteArray {
        try {
            if (blob == null || blob.length() == 0L) {
                return ByteArray(0)
            }
            val bt = ByteArray(blob.length().toInt())
            val stream = BufferedInputStream(blob.binaryStream)
            stream.read(bt)
            IoUtil.close(stream)
            return bt
        } catch (e: Exception) {
            throw DataBaseError(e.message)
        }
    }

    /**
     * 设置值
     *
     * @param result 结果
     * @param col    列
     * @param len    长度
     * @return 结果
     * @throws Exception 异常
     * @date 2024年6月10日15点30分
     * @since V1.0.0.0
     */
    @Throws(Exception::class)
    private fun setValue(result: ResultSet, col: Array<String?>, len: Int): Map<String?, Any?> {
        val map: MutableMap<String?, Any?> = HashMap()
        for (i in 0 until len) {
            val obj = result.getObject(col[i]) ?: continue
            val typeName = obj.javaClass.simpleName
            if ("BLOB" == typeName) {
                val blob = result.getBlob(col[i])
                map[col[i]] = blob2byte(blob)
            } else {
                map[col[i]] = result.getObject(col[i])
            }
        }
        return map
    }

    companion object {
        private const val DATABASE = "sqlite/db/MusicPlayer.db"
        private const val MAC_OS = "sqlite/sqlite-tools-osx-x64-3460000/sqlite3"
        private const val LINUX = "sqlite/sqlite-tools-linux-x64-3460000/sqlite3"
        private const val WINDOWS = "sqlite/sqlite-tools-win-x64-3460000/sqlite3.exe"

        init {
            try {
                val path = when (SysType.getMainType().type) {
                    2, 3 -> File(MAC_OS).canonicalPath
                    4 -> File(LINUX).canonicalPath
                    else -> File(WINDOWS).canonicalPath
                }
                System.setProperty("java.library.path", path + ";" + System.getProperty("java.library.path"))
                Class.forName("org.sqlite.JDBC")
            } catch (e: Exception) {
                throw DataBaseError(e.message)
            }
        }

        /**
         * 转下划线
         *
         * @param str 字符串
         * @return 结果
         * @date 2024年6月4日19点07分
         * @since idea
         */
        private fun underline(str: String): String {
            var str = str
            val reg = "[A-Z]+"
            val matcher = Pattern.compile(reg).matcher(str)
            while (matcher.find()) {
                val group = matcher.group()
                str = str.replace(group, "_" + group.lowercase())
            }
            return str
        }
    }
}
