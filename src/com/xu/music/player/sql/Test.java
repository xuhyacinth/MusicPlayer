package com.xu.music.player.sql;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
public class Test {

    public static void main(String[] args) throws Exception {
        SongEntity value = new SongEntity();
        System.out.println(value.getClass().getSimpleName());
        value.setId("111111");
        value.setName("vvvvvvvvv");
        value.setCreateTime(LocalDateTime.now());

        UpdateWrapper wrapper = new UpdateWrapper<>(value,"player");
        wrapper.insert();
        value.setName("eeee");
        wrapper.last(" where id = '111111'").update();

    }


}
