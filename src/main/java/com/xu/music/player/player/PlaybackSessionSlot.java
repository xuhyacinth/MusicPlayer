package com.xu.music.player.player;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 当前播放会话的原子容器。
 */
final class PlaybackSessionSlot {

    private final AtomicReference<PlaybackSession> current = new AtomicReference<>();

    PlaybackSession current() {
        return current.get();
    }

    PlaybackSession replace(PlaybackSession replacement) {
        return current.getAndSet(replacement);
    }

    PlaybackSession detach() {
        return current.getAndSet(null);
    }

    boolean complete(PlaybackSession session) {
        return current.compareAndSet(session, null);
    }
}
