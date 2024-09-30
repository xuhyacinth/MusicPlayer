package com.xu.music.player.wrapper.sql;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.utils.Utils;
import java.io.BufferedInputStream;
import java.io.File;
import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * 数据库操作
 *
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class NewHelper implements Helper {

    private static final String DATABASE = "sqlite/db/MusicPlayer.db";
    private static final String MAC_OS = "sqlite/sqlite-tools-osx-x64-3460000/sqlite3";
    private static final String LINUX = "sqlite/sqlite-tools-linux-x64-3460000/sqlite3";
    private static final String WINDOWS = "sqlite/sqlite-tools-win-x64-3460000/sqlite3.exe";

    static {
        try {
            String path;
            switch (SysType.getSystemMainType().type) {
                case 2:
                case 3:
                    path = new File(MAC_OS).getCanonicalPath();
                    break;
                case 4:
                    path = new File(LINUX).getCanonicalPath();
                    break;
                default:
                    path = new File(WINDOWS).getCanonicalPath();
            }
            System.setProperty("java.library.path", path + ";" + System.getProperty("java.library.path"));
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage());
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
    private static String underline(String str) {
        String reg = "[A-Z]+";
        Matcher matcher = Pattern.compile(reg).matcher(str);
        while (matcher.find()) {
            String group = matcher.group();
            str = str.replace(group, "_" + group.toLowerCase(Locale.ROOT));
        }
        return str;
    }

    @Override
    public Connection getConn() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + DATABASE);
        } catch (SQLException e) {
            throw new DataBaseError(e.getMessage());
        }
    }

    @Override
    public int insert(String sql, Object... params) {
        return update(sql, params);
    }

    @Override
    public int update(String sql, Object... params) {
        try (Connection conn = this.getConn()) {
            if (ArrayUtil.isEmpty(params)) {
                try (Statement state = conn.createStatement()) {
                    return state.executeUpdate(sql);
                } catch (SQLException e) {
                    conn.rollback();
                }
            }

            try (PreparedStatement state = conn.prepareStatement(sql)) {
                setValues(state, params);
                return state.executeUpdate(sql);
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage());
        }
        return 0;
    }

    @Override
    public int delete(String sql, Object... para) {
        return insert(sql, para);
    }

    @Override
    public <T> List<T> select(String sql, Class<T> cls, Object... params) {
        List<Map<String, Object>> data = select(sql, params);
        if (CollUtil.isEmpty(data)) {
            return new ArrayList<>();
        }
        return data.stream().filter(Objects::nonNull)
                .map(item -> convert(item, cls))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> select(String sql, Object... params) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection conn = this.getConn();
        try (PreparedStatement state = conn.prepareStatement(sql)) {

            this.setValues(state, params);
            ResultSet result = state.executeQuery();
            ResultSetMetaData data = result.getMetaData();

            int len = data.getColumnCount();
            String[] col = new String[len];
            for (int i = 0; i < len; i++) {
                col[i] = data.getColumnName(i + 1);
            }

            while (result.next()) {
                list.add(setValue(result, col, len));
            }

            IoUtil.close(result);
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage());
        } finally {
            IoUtil.close(conn);
        }
        return list;
    }

    /**
     * 设置预编译数据
     *
     * @param state 预编译对象
     * @param obj   预编译数据
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public void setValues(PreparedStatement state, Object... obj) {
        if (null == state || ArrayUtil.isEmpty(obj)) {
            return;
        }
        setValues(state, Arrays.asList(obj));
    }

    /**
     * 设置预编译数据
     *
     * @param state  预编译对象
     * @param params 预编译数据
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public void setValues(PreparedStatement state, List<Object> params) {
        if (null == state || CollUtil.isEmpty(params)) {
            return;
        }
        try {
            for (int i = 0; i < params.size(); i++) {
                Object object = params.get(i);
                if (null == object) {
                    state.setString(i + 1, "");
                    continue;
                }
                String type = object.getClass().getSimpleName();
                switch (type) {
                    case "SerialBlob":
                        state.setBlob(i + 1, (Blob) params.get(i));
                        break;
                    case "Integer":
                        state.setInt(i + 1, Integer.parseInt(String.valueOf(object)));
                        break;
                    case "Double":
                        state.setDouble(i + 1, Double.parseDouble(String.valueOf(object)));
                        break;
                    case "Float":
                        state.setFloat(i + 1, Float.parseFloat(String.valueOf(object)));
                        break;
                    case "Long":
                        state.setLong(i + 1, Long.parseLong(String.valueOf(object)));
                        break;
                    case "Short":
                        state.setShort(i + 1, Short.parseShort(String.valueOf(object)));
                        break;
                    case "Timestamp":
                        state.setTimestamp(i + 1, (Timestamp) params.get(i));
                        break;
                    default:
                        state.setString(i + 1, String.valueOf(object));
                }
            }
        } catch (SQLException e) {
            throw new DataBaseError(e.getMessage());
        }
    }

    /**
     * 获取值
     *
     * @param map   值
     * @param field 字段
     * @return 结果
     * @date 2024年6月4日19点07分
     * @since idea
     */
    private Object getValue(Map<String, Object> map, Field field) {
        if (MapUtil.isEmpty(map)) {
            return null;
        }
        for (Map.Entry<String, Object> result : map.entrySet()) {
            if (StrUtil.equalsAnyIgnoreCase(result.getKey(), field.getName(), underline(field.getName()))) {
                return result.getValue();
            }
        }
        return null;
    }

    /**
     * 转换
     *
     * @param map 值
     * @param cls 类
     * @param <T> 泛型
     * @return 结果
     * @date 2024年6月4日19点07分
     * @since idea
     */
    private <T> T convert(Map<String, Object> map, Class<T> cls) {
        if (MapUtil.isEmpty(map) || null == cls) {
            return null;
        }
        try {
            Field[] fields = cls.getDeclaredFields();
            T t = cls.getDeclaredConstructor().newInstance();
            for (Field field : fields) {
                Object object = getValue(map, field);
                if (null == object) {
                    continue;
                }
                field.setAccessible(true);
                String name = field.getType().getSimpleName();
                if (StrUtil.equalsAny(name, "Date", "LocalDate", "LocalTime", "LocalDateTime")) {
                    field.set(t, Utils.formatDateTime(object));
                } else {
                    field.set(t, object);
                }
            }
            return t;
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage());
        }
    }

    /**
     * Blob 转字节数组
     *
     * @param blob Blob
     * @return 字节数组
     * @date 2024年6月4日19点07分
     * @since idea
     */
    private byte[] blob2byte(Blob blob) {
        try {
            if (blob == null || blob.length() == 0) {
                return new byte[0];
            }
            byte[] bt = new byte[(int) blob.length()];
            BufferedInputStream stream = new BufferedInputStream(blob.getBinaryStream());
            stream.read(bt);
            IoUtil.close(stream);
            return bt;
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage());
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
     * @date 2024年6月4日19点07分
     * @since idea
     */
    private Map<String, Object> setValue(ResultSet result, String[] col, int len) throws Exception {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < len; i++) {
            Object obj = result.getObject(col[i]);
            if (obj == null) {
                continue;
            }
            String typeName = obj.getClass().getSimpleName();
            if ("BLOB".equals(typeName)) {
                Blob blob = result.getBlob(col[i]);
                map.put(col[i], blob2byte(blob));
            } else {
                map.put(col[i], result.getObject(col[i]));
            }
        }
        return map;
    }

}
