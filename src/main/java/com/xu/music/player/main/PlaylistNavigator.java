package com.xu.music.player.main;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
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

    /** 自动顺序播放不回绕；手动切歌保留上一曲、下一曲的循环边界。 */
    static OptionalInt findPlayable(Integer current, int size, int delta, PlaybackMode mode,
                                    boolean naturalCompletion, IntPredicate playable, IntUnaryOperator random) {
        if (size <= 0) return OptionalInt.empty();
        if (delta != -1 && delta != 1) throw new IllegalArgumentException("播放方向必须是 -1 或 1");
        boolean hasCurrent = current != null && current >= 0 && current < size;
        if (mode == PlaybackMode.RANDOM) {
            var candidates = new ArrayList<Integer>();
            for (int index = 0; index < size; index++) {
                if ((!hasCurrent || index != current) && playable.test(index)) candidates.add(index);
            }
            if (!candidates.isEmpty()) {
                return OptionalInt.of(candidates.get(random.applyAsInt(candidates.size())));
            }
            return hasCurrent && playable.test(current) ? OptionalInt.of(current) : OptionalInt.empty();
        }
        if (naturalCompletion) {
            if (mode == PlaybackMode.REPEAT_ONE) {
                return hasCurrent && playable.test(current) ? OptionalInt.of(current) : OptionalInt.empty();
            }
            for (int index = hasCurrent ? current + 1 : 0; index < size; index++) {
                if (playable.test(index)) return OptionalInt.of(index);
            }
            return OptionalInt.empty();
        }
        return findPlayable(hasCurrent ? current : null, size, delta, playable);
    }
}
