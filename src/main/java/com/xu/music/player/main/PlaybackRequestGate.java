package com.xu.music.player.main;

import java.util.concurrent.atomic.AtomicLong;

final class PlaybackRequestGate {

    private final AtomicLong generation = new AtomicLong();

    long beginRequest() {
        return generation.incrementAndGet();
    }

    long snapshot() {
        return generation.get();
    }

    boolean accepts(long completedGeneration, boolean playerIsPlaying) {
        return !playerIsPlaying && generation.get() == completedGeneration;
    }
}
