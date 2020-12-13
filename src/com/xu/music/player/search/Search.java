package com.xu.music.player.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


@SuppressWarnings(value = "all")
public class Search {

    private static String json = "";
    private static String url = "";

    public static void main(String[] args) throws MalformedURLException, IOException {
        List<APISearchTipsEntity> songs = Search.search("不醉不", "API");
        //for (APISearchTipsEntity song:songs) {
        //System.out.println(song.toString());
        //}
        songs = downloads(songs.get(0));
        download(songs.get(0));
    }

    public static void download(APISearchTipsEntity entitys) {
        String url = "http://www.kugou.com/yy/index.php?r=play/getdata&hash=" + entitys.getHash();
        String content = "";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            BufferedReader breader = new BufferedReader(reader);
            while ((content = breader.readLine()) != null) {
                json += content;
            }
            System.out.println(json);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> List<T> downloads(APISearchTipsEntity entitys) {
        List<T> songs = new ArrayList<T>();
        String url = "http://www.kugou.com/yy/index.php?r=play/getdata&hash=" + (empty(entitys.getS320hash()) ? entitys.getHash() : entitys.getS320hash());
        String content = "";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            BufferedReader breader = new BufferedReader(reader);
            while ((content = breader.readLine()) != null) {
                json += content;
            }
            json = json.substring(0, json.lastIndexOf("errcode") + 11);
            JSONObject ojsons = JSONObject.parseObject(json);
            ojsons = JSONObject.parseObject(ojsons.getString("data"));
            JSONArray array = JSONArray.parseArray(ojsons.getString("info"));
            for (int i = 0; i < array.size(); i++) {
                APISearchTipsEntity entity = JSON.toJavaObject(JSONObject.parseObject(array.get(i).toString()), APISearchTipsEntity.class);
                entity.setS320hash(JSON.parseObject(array.get(i).toString()).get("320hash").toString());
                entity.setS320filesize(JSON.parseObject(array.get(i).toString()).get("320filesize").toString());
                entity.setS320privilege(JSON.parseObject(array.get(i).toString()).get("320privilege").toString());
                songs.add((T) entity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return songs;
    }

    public static <T> List<T> search(String name, String type) {
        List<T> songs = new ArrayList<T>();
        if ("API".equalsIgnoreCase(type)) {
            url = "http://mobilecdn.kugou.com/api/v3/search/song?format=json&keyword=" + name + "&page=1&pagesize=20&showtype=1";
        } else if ("WEB".equalsIgnoreCase(type)) {
            url = "http://searchtip.kugou.com/getSearchTip?MusicTipCount=5&MVTipCount=2&albumcount=2&keyword=" + name + "&callback=jQuery180014477266089871377_1523886180659&_=1523886" + RandomCode();
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            BufferedReader breader = new BufferedReader(reader);
            json = breader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ("API".equalsIgnoreCase(type)) {
            JSONObject ojsons = JSONObject.parseObject(json);
            ojsons = JSONObject.parseObject(ojsons.getString("data"));
            JSONArray array = JSONArray.parseArray(ojsons.getString("info"));
            for (int i = 0; i < array.size(); i++) {
                APISearchTipsEntity entity = JSON.toJavaObject(JSONObject.parseObject(array.get(i).toString()), APISearchTipsEntity.class);
                entity.setS320hash(JSON.parseObject(array.get(i).toString()).get("320hash").toString());
                entity.setS320filesize(JSON.parseObject(array.get(i).toString()).get("320filesize").toString());
                entity.setS320privilege(JSON.parseObject(array.get(i).toString()).get("320privilege").toString());
                songs.add((T) entity);
            }
        } else if ("WEB".equalsIgnoreCase(type)) {
            json = json.substring(json.indexOf("(") + 1, json.lastIndexOf(")"));
            JSONObject ojsons = JSONObject.parseObject(json);
            JSONArray arrays = JSONArray.parseArray(ojsons.getString("data"));
            for (int i = 0; i < arrays.size(); i++) {
                ojsons = JSONObject.parseObject(JSONObject.parseObject(arrays.get(i).toString()).toJSONString());
                JSONArray array = JSONArray.parseArray(ojsons.getString("RecordDatas"));
                for (int j = 0; j < array.size(); j++) {
                    WEBSearchTipsEntity entity = JSON.toJavaObject(JSONObject.parseObject(array.get(j).toString()), WEBSearchTipsEntity.class);
                    songs.add((T) entity);
                }
            }
        }
        return songs;
    }

    private static String RandomCode() {
        String code = "";
        for (int i = 0; i < 6; i++) {
            code += new Random().nextInt(10) + "";
        }
        return code;
    }

    private static boolean empty(String val) {
        if (val == null || val.length() <= 0) {
            return true;
        } else {
            return false;
        }
    }

}
