package com.xu.music.player.player;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class PlaybackCompletionNotifier {

    private static final Runnable NO_OP = () -> {
    };

    private final AtomicReference<Runnable> listener = new AtomicReference<>(NO_OP);

    void setListener(Runnable listener) {
        this.listener.set(Objects.requireNonNullElse(listener, NO_OP));
    }

    void notifyIfNatural(boolean reachedEof, boolean completedCurrentSession) {
        if (reachedEof && completedCurrentSession) {
            listener.get().run();
        }
    }
}
