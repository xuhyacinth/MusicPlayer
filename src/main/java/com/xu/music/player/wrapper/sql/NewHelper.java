package com.xu.music.player.wrapper.sql;

import java.io.BufferedInputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

import com.xu.music.player.hander.DataBaseError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import java.sql.Timestamp;

/**
 * 数据库操作
 *
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class NewHelper implements Helper {

    private static final Path DEFAULT_DATABASE = Path.of("lib", "sqlite", "db", "MusicPlayer.db");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path database;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DataBaseError(e.getMessage(), e);
        }
    }

    public NewHelper() {
        this(DEFAULT_DATABASE);
    }

    public NewHelper(Path database) {
        this.database = database.toAbsolutePath().normalize();
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
            return DriverManager.getConnection("jdbc:sqlite:" + database);
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage(), e);
        }
    }

    @Override
    public int insert(String sql, Object... params) {
        return update(sql, params);
    }

    @Override
    public int update(String sql, Object... params) {
        try (Connection conn = this.getConn();
             PreparedStatement state = conn.prepareStatement(sql)) {
            setValues(state, params);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage(), e); // 不再吞掉异常，方便捕获上层错误
        }
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
        // 使用 try-with-resources 自动管理 Connection、PreparedStatement 和 ResultSet，杜绝连接泄露
        try (Connection conn = this.getConn();
             PreparedStatement state = conn.prepareStatement(sql)) {

            this.setValues(state, params);
            try (ResultSet result = state.executeQuery()) {
                ResultSetMetaData data = result.getMetaData();

                int len = data.getColumnCount();
                String[] col = new String[len];
                for (int i = 0; i < len; i++) {
                    col[i] = data.getColumnName(i + 1);
                }

                while (result.next()) {
                    list.add(setValue(result, col, len));
                }
            }
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage(), e);
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
                    state.setObject(i + 1, null);
                    continue;
                }
                switch (object) {
                    case Blob blob -> state.setBlob(i + 1, blob);
                    case byte[] bytes -> state.setBytes(i + 1, bytes);
                    case Timestamp timestamp -> state.setTimestamp(i + 1, timestamp);
                    case Date date -> state.setString(i + 1, DATE_TIME.format(
                            date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
                    case LocalDateTime dateTime -> state.setString(i + 1, DATE_TIME.format(dateTime));
                    default -> state.setObject(i + 1, object);
                }
            }
        } catch (SQLException e) {
            throw new DataBaseError(e.getMessage(), e);
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
                field.set(t, convertValue(field.getType(), object));
            }
            return t;
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage(), e);
        }
    }

    private Object convertValue(Class<?> targetType, Object value) {
        if (targetType.isInstance(value)) {
            return value;
        }
        if (!(value instanceof CharSequence text)) {
            return value;
        }

        var raw = text.toString();
        if (targetType == Date.class) {
            var local = LocalDateTime.parse(raw, DATE_TIME);
            return Date.from(local.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(raw, DATE_TIME);
        }
        if (targetType == LocalDate.class) {
            return LocalDate.parse(raw);
        }
        if (targetType == LocalTime.class) {
            return LocalTime.parse(raw);
        }
        return value;
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
            stream.read(bt, 0, bt.length);
            IoUtil.close(stream);
            return bt;
        } catch (Exception e) {
            throw new DataBaseError(e.getMessage(), e);
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
