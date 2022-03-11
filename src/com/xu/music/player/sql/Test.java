package com.xu.music.player.sql;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Administrator
 */
public class Test {

    static Pattern pattern = Pattern.compile("[A-Z]${1}");

    public static void main(String[] args) throws IllegalAccessException {
        SongEntity value = new SongEntity();
        System.out.println(value.getClass().getSimpleName());
        value.setId("111111");
        value.setCreateTime(LocalDateTime.now());
        save(value, "info");


    }

    public static <T> void save(T t, String tableName) throws IllegalAccessException {
        Field[] fields = t.getClass().getDeclaredFields();
        String sql = "insert into " + tableName + "";
        List<String> fieldsList = new LinkedList<>();
        List<Object> valuesList = new LinkedList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(t);
            if (null != value) {
                fieldsList.add(dealField(field.getName()));
                valuesList.add(field.get(t));
            }
        }
        sql = sql + "(" + String.join(", ", fieldsList) + ") values()";
        System.out.println(sql);
    }

    private static String dealValue(List<Object> valuesList) {
        List<String> values = new LinkedList<>();
        for (Object value : valuesList) {
            if (value instanceof Double){

            } else if ()
        }
        return String.join(", ", values);
    }

    private static String dealField(String fieldName) {
        Matcher matcher = Pattern.compile("[A-Z]").matcher(fieldName);
        while (matcher.find()) {
            fieldName = fieldName.replace(matcher.group(), "_" + matcher.group().toLowerCase(Locale.ROOT));
        }
        return fieldName;
    }

}
