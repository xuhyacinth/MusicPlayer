package com.xu.music.player.sql;

import java.util.LinkedList;
import java.util.List;

public class BasicWrapper<T> {

    public String last;

    public Class<T> bean;

    public String table;

    public String[] field;

    public List<String> condition = new LinkedList<>();

}
