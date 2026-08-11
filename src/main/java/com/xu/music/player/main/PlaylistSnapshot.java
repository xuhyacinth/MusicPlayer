package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record PlaylistSnapshot(Map<Integer, SongEntity> songs, Integer playingIndex) {

    static PlaylistSnapshot from(List<SongEntity> source, String playingSongId) {
        Map<Integer, SongEntity> songs = new LinkedHashMap<>();
        Integer playingIndex = null;

        for (int index = 0; index < source.size(); index++) {
            SongEntity song = source.get(index);
            songs.put(index, song);
            if (playingIndex == null && playingSongId != null && playingSongId.equals(song.getId())) {
                playingIndex = index;
            }
        }

        return new PlaylistSnapshot(Collections.unmodifiableMap(songs), playingIndex);
    }
}
