package com.xu.music.player.config;

import com.xu.music.player.system.Constant;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Writing {

    public void write(List<String> lists) {
        File file = new File(Constant.MUSIC_PLAYER_SONG_LISTS_FULL_PATH);
        HashSet<String> songs = new HashSet<>();
        if (file.exists()) {
            songs = new Reading().read();
            int count = songs.size();
            String content;
            String[] splits;
            for (String list : lists) {
                splits = list.split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT);
                content = count + "";
                try {
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + splits[0];
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + getSongName(splits[0]);
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + getSongName(splits[0]);
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + getSongLength(splits[0]);
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + splits[1];
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                count++;
                songs.add(content);
            }
        } else {
            try {
                new File(Constant.MUSIC_PLAYER_SONG_LISTS_PATH).mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String content;
            String[] splits;
            int count = 0;
            for (String list : lists) {
                splits = list.split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT);
                content = count + "";
                try {
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + splits[0];
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + getSongName(splits[0]);
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + getSongName(splits[0]);
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + getSongLength(splits[0]);
                    content += Constant.MUSIC_PLAYER_SYSTEM_SPLIT + splits[1];
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                count++;
                songs.add(content);
            }
        }
        FileWriter fWriter = null;
        BufferedWriter bWriter;
        try {
            fWriter = new FileWriter(Constant.MUSIC_PLAYER_SONG_LISTS_FULL_PATH);
            bWriter = new BufferedWriter(fWriter);
            Constant.MUSIC_PLAYER_SONGS_LIST.clear();
            for (String song : songs) {
                bWriter.write(song);
                Constant.MUSIC_PLAYER_SONGS_LIST.add(song);
                bWriter.newLine();
            }
            bWriter.flush();
            bWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fWriter != null) {
                    fWriter.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getSongName(String song) {
        song = song.replace("/", "\\");
        return song.substring(song.lastIndexOf("\\") + 1, song.lastIndexOf("."));
    }

    private int getSongLength(String path) {
        File file = new File(path);
        AudioFile mp3 = null;
        try {
            mp3 = AudioFileIO.read(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(mp3).getAudioHeader().getTrackLength();
    }

}
