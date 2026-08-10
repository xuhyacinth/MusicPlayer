package com.xu.music.player.wrapper;

import cn.hutool.core.util.StrUtil;
import com.xu.music.player.hander.DataBaseError;
import com.xu.music.player.wrapper.sql.SqlCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SQL Wrapper 的公共状态与参数收集逻辑。
 */
public class BasicWrapper<T> {

    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    protected Class<T> bean;
    protected String table;
    protected String[] field;
    protected final List<String> conditions = new ArrayList<>();
    protected final List<Object> parameters = new ArrayList<>();

    protected void addCondition(String sql, Object... values) {
        conditions.add(" and " + sql);
        Collections.addAll(parameters, values);
    }

    protected SqlCommand command(String baseSql, List<Object> leadingParameters) {
        var sql = new StringBuilder(baseSql);
        conditions.forEach(sql::append);

        var allParameters = new ArrayList<>(leadingParameters);
        allParameters.addAll(parameters);
        return new SqlCommand(sql.toString(), allParameters);
    }

    protected static String requireIdentifier(String name) {
        if (name == null || !IDENTIFIER.matcher(name).matches()) {
            throw new DataBaseError("非法 SQL 标识符: " + name);
        }
        return name;
    }

    protected String dealField(String name) {
        requireIdentifier(name);
        var matcher = UPPER_CASE.matcher(name);
        var result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, "_" + matcher.group().toLowerCase(Locale.ROOT));
        }
        matcher.appendTail(result);
        var fieldName = result.toString();
        return StrUtil.equalsAnyIgnoreCase(fieldName, "index") ? "`" + fieldName + "`" : fieldName;
    }
}
