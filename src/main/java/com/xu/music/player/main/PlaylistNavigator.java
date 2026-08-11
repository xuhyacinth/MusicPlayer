package com.xu.music.player.main;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntPredicate;

/**
 * 播放列表索引计算。
 */
public final class PlaylistNavigator {

    private PlaylistNavigator() {
    }

    public static int move(int current, int size, int delta) {
        if (size <= 0) {
            throw new IllegalArgumentException("播放列表不能为空");
        }
        return Math.floorMod(current + delta, size);
    }

    public static OptionalInt findPlayable(Integer current, int size, int delta, IntPredicate playable) {
        if (size <= 0) {
            return OptionalInt.empty();
        }
        if (delta != -1 && delta != 1) {
            throw new IllegalArgumentException("播放方向必须是 -1 或 1");
        }
        Objects.requireNonNull(playable);

        int candidate = current == null ? (delta == 1 ? 0 : size - 1) : move(current, size, delta);
        for (int checked = 0; checked < size; checked++) {
            if (playable.test(candidate)) {
                return OptionalInt.of(candidate);
            }
            candidate = move(candidate, size, delta);
        }
        return OptionalInt.empty();
    }
}
