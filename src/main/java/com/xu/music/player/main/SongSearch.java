package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;

import java.util.List;
import java.util.Locale;

final class SongSearch {

    private SongSearch() {
    }

    static List<SongEntity> filter(List<SongEntity> songs, String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isBlank()) {
            return songs;
        }

        return songs.stream()
                .filter(song -> contains(song.getName(), normalizedKeyword)
                        || contains(song.getAuthor(), normalizedKeyword))
                .toList();
    }

    private static boolean contains(String value, String keyword) {
        return value != null && normalize(value).contains(keyword);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
