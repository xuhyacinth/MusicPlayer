package com.xu.music.player.wrapper.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 一条参数化 SQL 命令。
 */
public record SqlCommand(String sql, List<Object> parameters) {

    public SqlCommand {
        Objects.requireNonNull(sql, "sql");
        parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    public Object[] parameterArray() {
        return parameters.toArray();
    }
}
