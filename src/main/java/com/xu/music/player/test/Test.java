package com.xu.music.player.test;

import cn.hutool.json.JSONUtil;
import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.wrapper.QueryWrapper;

import java.util.List;

public class Test {

    public static void main(String[] args) {
        SongEntity value = new SongEntity();
//        System.out.println(value.getClass().getSimpleName());
//        value.setId("111111");
//        value.setName("vvvvvvvvv");
//        value.setCreateTime(new Date());
//
//        InsertWrapper<SongEntity> add = new InsertWrapper<>(value, "song");
//        add.insert();

        QueryWrapper<SongEntity> select = new QueryWrapper<>(SongEntity.class, "song");
        List<SongEntity> v = select.last("limit 1").list();
        System.out.println(JSONUtil.toJsonPrettyStr(v));
    }

}
