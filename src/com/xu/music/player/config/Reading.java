package com.xu.music.player.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.xu.music.player.system.Constant;

public class Reading {

    public HashSet<String> read() {
        File file = new File(Constant.MUSIC_PLAYER_SONG_LISTS_FULL_PATH);
        if (file.exists() && file.isFile()) {
            HashSet<String> songs = new HashSet<>();
            Constant.MUSIC_PLAYER_SONGS_LIST.clear();
            InputStreamReader fReader = null;
            BufferedReader bReader = null;
            String song;
            try (FileInputStream stream = new FileInputStream(file)) {
                fReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                bReader = new BufferedReader(fReader);
                while ((song = bReader.readLine()) != null) {
                    songs.add(song);
                    Constant.MUSIC_PLAYER_SONGS_LIST.add(song);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fReader != null) {
                        fReader.close();
                    }
                    if (bReader != null) {
                        bReader.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return (HashSet<String>) songs.stream().sorted().collect(Collectors.toSet());
        } else {
            return null;
        }

    }

    public boolean empty(String k) {
        return k == null || k.length() <= 0;
    }

}
