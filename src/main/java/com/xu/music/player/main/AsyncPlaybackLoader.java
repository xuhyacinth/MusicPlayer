package com.xu.music.player.main;

import com.xu.music.player.player.Player;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 音频解码、声卡打开和旧会话释放均不占用 SWT 线程。 */
final class AsyncPlaybackLoader implements AutoCloseable {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("music-loader").factory());
    private final Supplier<Player> factory;
    // 仅加载线程访问，先释放上一条音频行再打开下一条。
    private Player active;

    AsyncPlaybackLoader(Supplier<Player> factory) {
        this.factory = factory;
    }

    void load(String path, BooleanSupplier current, Consumer<Player> prepare,
              Consumer<Player> ready, Consumer<Exception> failed) {
        worker.execute(() -> {
            if (!current.getAsBoolean()) return;
            Player candidate = null;
            try {
                if (active != null) {
                    active.close();
                    active = null;
                }
                candidate = factory.get();
                candidate.load(path);
                if (!current.getAsBoolean()) { candidate.close(); return; }
                prepare.accept(candidate);
                if (!current.getAsBoolean()) { candidate.close(); return; }
                candidate.play();
                if (current.getAsBoolean()) {
                    active = candidate;
                    ready.accept(candidate);
                } else candidate.close();
            } catch (Exception error) {
                if (candidate != null) {
                    try { candidate.close(); } catch (RuntimeException cleanup) { error.addSuppressed(cleanup); }
                    if (active == candidate) active = null;
                }
                if (current.getAsBoolean()) failed.accept(error);
            }
        });
    }

    static void release(Player player) {
        if (player != null) Thread.ofVirtual().name("music-release").start(player::close);
    }

    @Override public void close() {
        // 已提交任务仍会检查请求代次并释放过期资源，不能直接丢弃队列。
        worker.execute(() -> {
            if (active != null) { active.close(); active = null; }
        });
        worker.shutdown();
    }
}
