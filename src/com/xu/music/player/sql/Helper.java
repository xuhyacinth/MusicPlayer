package com.xu.music.player.sql;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;


/**
 * @author Administrator
 */
public class Helper {

    private ResultSet result = null;
    private Connection connection = null;
    private PreparedStatement statement = null;

    private static final String DATABASE = "sqlite/player/player.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * 关闭的方法
     */
    public void closeAll(Connection connection, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置预编译数据
     *
     * @param pstmt  预编译对象
     * @param params 预编译数据
     */
    public void setValues(PreparedStatement pstmt, List<Object> params) {
        if (pstmt != null && params != null && params.size() > 0) {
            String type = null;
            for (int i = 0; i < params.size(); i++) {
                Object object = params.get(i);
                try {
                    if (object != null) {
                        type = object.getClass().getName();
                        if ("javax.sql.rowset.serial.SerialBlob".equals(type)) {//javax.sql.rowset.serial.SerialBlob
                            pstmt.setBlob(i + 1, (Blob) params.get(i));
                        } else if ("java.lang.Integer".equals(type)) {//java.lang.Integer
                            pstmt.setInt(i + 1, Integer.parseInt(String.valueOf(object)));
                        } else if ("java.lang.Double".equals(type)) {//java.lang.Double
                            pstmt.setDouble(i + 1, Double.parseDouble(String.valueOf(object)));
                        } else if ("java.lang.Float".equals(type)) {//java.lang.Float
                            pstmt.setDouble(i + 1, Float.parseFloat(String.valueOf(object)));
                        } else if ("java.lang.Long".equals(type)) {//java.lang.Long
                            pstmt.setLong(i + 1, Long.parseLong(String.valueOf(object)));
                        } else if ("java.lang.Short".equals(type)) {//java.lang.Short
                            pstmt.setShort(i + 1, Short.parseShort(String.valueOf(object)));
                        } else if ("java.lang.String".equals(type)) {//java.lang.String
                            pstmt.setString(i + 1, String.valueOf(object));
                        } else if ("java.lang.Timestamp".equals(type)) {//java.sql.Timestamp
                            pstmt.setTimestamp(i + 1, (Timestamp) params.get(i));
                        } else {
                            pstmt.setString(i + 1, String.valueOf(object));
                        }
                    } else {
                        pstmt.setString(i + 1, "");
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 设置预编译数据
     *
     * @param pstmt 预编译对象
     * @param objs  预编译数据
     */
    public void setValues(PreparedStatement pstmt, Object... objs) {
        if (pstmt != null && objs != null && objs.length > 0) {
            String type = null;
            for (int i = 0, len = objs.length; i < len; i++) {
                Object object = objs[i];
                try {
                    if (object != null) {
                        type = object.getClass().getName();
                        if ("javax.sql.rowset.serial.SerialBlob".equals(type)) {//javax.sql.rowset.serial.SerialBlob
                            pstmt.setBlob(i + 1, (Blob) objs[i]);
                        } else if ("java.lang.Integer".equals(type)) {//java.lang.Integer
                            pstmt.setInt(i + 1, Integer.parseInt(String.valueOf(object)));
                        } else if ("java.lang.Double".equals(type)) {//java.lang.Double
                            pstmt.setDouble(i + 1, Double.parseDouble(String.valueOf(object)));
                        } else if ("java.lang.Float".equals(type)) {//java.lang.Float
                            pstmt.setDouble(i + 1, Float.parseFloat(String.valueOf(object)));
                        } else if ("java.lang.Long".equals(type)) {//java.lang.Long
                            pstmt.setLong(i + 1, Long.parseLong(String.valueOf(object)));
                        } else if ("java.lang.Short".equals(type)) {//java.lang.Short
                            pstmt.setShort(i + 1, Short.parseShort(String.valueOf(object)));
                        } else if ("java.lang.String".equals(type)) {//java.lang.String
                            pstmt.setString(i + 1, String.valueOf(object));
                        } else if ("java.lang.Timestamp".equals(type)) {//java.sql.Timestamp
                            pstmt.setTimestamp(i + 1, (Timestamp) objs[i]);
                        } else {
                            pstmt.setString(i + 1, String.valueOf(object));
                        }
                    } else {
                        pstmt.setString(i + 1, "");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 多SQL增删改(不建议使用)
     *
     * @param sql    sql语句(使用?作为占位符)
     * @param params sql语句参数
     * @return 结果(- 1代表失败, 大于0表示成功)
     */
    public int update(List<String> sql, List<List<Object>> params) {
        connection = this.getConnection();
        int result = 0;
        try {
            connection.setAutoCommit(false);  //事务处理
            for (int i = 0; i < sql.size(); i++) {
                List<Object> param = params.get(i);
                statement = connection.prepareStatement(sql.get(i));  //预编译对象
                setValues(statement, param);    //设置参数
                result = statement.executeUpdate();
            }
            connection.commit(); //没有错处执行
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();  //出错回滚
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            closeAll(connection, statement, null);
        }
        return result;
    }

    /**
     * 单表增删改
     *
     * @param sql    sql语句
     * @param params 参数
     * @return 结果(- 1代表失败, 大于0表示成功)
     */
    public int update(String sql, List<Object> params) {
        connection = this.getConnection();
        int result = 0;
        try {
            statement = connection.prepareStatement(sql);//预编译对象
            setValues(statement, params);//设置参数
            result = statement.executeUpdate();
        } catch (SQLException e) {
            try {
                connection.rollback();  //出错回滚
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            closeAll(connection, statement, null);
        }
        return result;
    }

    /**
     * 单表增删改
     *
     * @param sql    sql语句
     * @param params 参数
     * @return 结果(- 1代表失败, 大于0表示成功)
     */
    public int update(String sql, Object... params) {
        connection = this.getConnection();
        int result = 0;
        try {
            statement = connection.prepareStatement(sql);  //预编译对象
            setValues(statement, params);    //设置参数
            result = statement.executeUpdate();
        } catch (SQLException e) {
            try {
                connection.rollback();  //出错回滚
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            closeAll(connection, statement, null);
        }

        return result;
    }


    /**
     * 多SQL查询(不建议使用)
     *
     * @param sql    sql语句(使用?作为占位符)
     * @param params sql语句参数
     * @return 结果
     */
    public List<String> find(String sql, List<Object> params) {
        List<String> list = new ArrayList<String>();
        connection = this.getConnection();
        try {
            statement = connection.prepareStatement(sql);  //预编译对象
            setValues(statement, params);   //设置参数
            result = statement.executeQuery();  //执行查询

            ResultSetMetaData md = result.getMetaData();  //结果集的元数据，它反映了结果集的信息
            int count = md.getColumnCount();    //取出结果集中列的数量

            if (result.next()) {
                for (int i = 1; i <= count; i++) {
                    list.add(result.getString(i));
                }
            }
        } catch (SQLException e) {
            //TODO:
        } finally {
            closeAll(connection, statement, result);
        }
        return list;
    }

    /**
     * 单表查询
     *
     * @param sql    sql语句(使用?作为占位符)
     * @param params sql语句参数
     * @return 结果
     */
    public List<String> find(String sql, Object... params) {
        List<String> list = new ArrayList<String>();
        connection = this.getConnection();
        try {
            statement = connection.prepareStatement(sql);  //预编译对象
            setValues(statement, params);   //设置参数
            result = statement.executeQuery();  //执行查询

            ResultSetMetaData md = result.getMetaData();  //结果集的元数据，它反映了结果集的信息
            int count = md.getColumnCount();    //取出结果集中列的数量

            if (result.next()) {
                for (int i = 1; i <= count; i++) {
                    list.add(result.getString(i));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeAll(connection, statement, result);
        }
        return list;
    }

    /**
     * 单表查询
     *
     * @param sql    sql语句
     * @param c      JavaBean
     * @param params sql语句参数
     * @return 结果
     * @throws Exception
     */
    public <T> List<T> find(String sql, Class<T> c, List<Object> params) throws Exception {
        if (params == null) {
            return find(sql, c);
        } else {
            return find(sql, c, params.toArray());
        }

    }

    /**
     * 单表查询
     *
     * @param sql    sql语句
     * @param c      JavaBean
     * @param params sql语句参数
     * @return 结果
     */
    public <T> List<T> find(String sql, Class<T> c, Object... params) {
        return Optional.ofNullable(finds(sql, params)).orElse(new ArrayList<>()).stream().filter(Objects::nonNull).map(item -> {
            Field[] fields = c.getDeclaredFields();
            T t = null;
            try {
                t = c.getDeclaredConstructor().newInstance();
                for (Field field : fields) {
                    for (Map.Entry<String, Object> result : item.entrySet()) {
                        if (StringUtils.equalsIgnoreCase(field.getName(), result.getKey()) ||
                                StringUtils.equalsIgnoreCase(to_db_case(field.getName()), result.getKey())) {
                            field.setAccessible(true);
                            switch (field.getType().getName()) {
                                case "java.time.LocalDateTime":
                                    String time = (String) result.getValue();
                                    if (StringUtils.isNotBlank(time)) {
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                        field.set(t, LocalDateTime.parse(time, formatter));
                                    }
                                    break;
                                default:
                                    field.set(t, result.getValue());
                                    break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return t;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static String to_db_case(String str) {
        String reg = "[A-Z]+";
        Matcher matcher = Pattern.compile(reg).matcher(str);
        while (matcher.find()) {
            String group = matcher.group();
            str = str.replace(group, "_" + group.toLowerCase(Locale.ROOT));
        }
        return str;
    }

    /**
     * 单表查询
     *
     * @param sql    sql语句
     * @param params sql语句参数
     * @return 结果
     */
    public List<Map<String, Object>> finds(String sql, List<Object> params) {
        if (params == null) {
            return finds(sql);
        } else {
            return finds(sql, params.toArray());
        }
    }

    /**
     * 单表查询
     *
     * @param sql  sql语句
     * @param objs sql语句参数
     * @return 结果
     */
    public List<Map<String, Object>> finds(String sql, Object... objs) {
        List<Map<String, Object>> list = new ArrayList<>();
        connection = this.getConnection();
        try {
            statement = connection.prepareStatement(sql);
            this.setValues(statement, objs);//给占位符赋值
            result = statement.executeQuery();//获取结果集
            //获取返回结果中的列的列名
            ResultSetMetaData rsmd = result.getMetaData();
            int colLen = rsmd.getColumnCount(); //获取结果集中列的数量
            String[] colNames = new String[colLen];
            for (int i = 0; i < colLen; i++) {//取出每个列的列名存放到数组中
                colNames[i] = rsmd.getColumnName(i + 1);
            }
            Map<String, Object> map = null;
            String typeName;
            Object obj;
            while (result.next()) {// 循环取值，每循环一次就是一条记录，存放到一个map中
                map = new HashMap<>();
                for (int i = 0; i < colLen; i++) { // 循环取出每个列的值
                    obj = result.getObject(colNames[i]);
                    if (obj != null) {
                        typeName = obj.getClass().getSimpleName();
                        if ("BLOB".equals(typeName)) {
                            Blob blob = result.getBlob(colNames[i]);
                            byte[] bt = null;
                            BufferedInputStream bis = null;
                            try {
                                bis = new BufferedInputStream(blob.getBinaryStream());
                                bt = new byte[(int) blob.length()];
                                bis.read(bt);
                                map.put(colNames[i], bt);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (bis != null) {
                                    try {
                                        bis.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            map.put(colNames[i], result.getObject(colNames[i]));
                        }
                    }
                }
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.closeAll(connection, statement, result);
        }
        return list;
    }

}

