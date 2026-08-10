package com.xu.music.player.main;

import com.xu.music.player.hander.MusicPlayerError;
import com.xu.music.player.player.Player;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PlaybackStarterTest {

    @Test
    public void stopsPartiallyInitializedPlayerWhenLoadFails() {
        var stopCount = new AtomicInteger();
        var player = player((method, arguments) -> {
            if ("load".equals(method)) {
                throw new IOException("broken audio");
            }
            if ("stop".equals(method)) {
                stopCount.incrementAndGet();
            }
        });

        assertThrows(MusicPlayerError.class, () -> PlaybackStarter.start(player, "broken.mp3"));
        assertEquals(1, stopCount.get());
    }

    @Test
    public void playsAfterSuccessfulLoad() {
        var loadCount = new AtomicInteger();
        var playCount = new AtomicInteger();
        var player = player((method, arguments) -> {
            if ("load".equals(method)) {
                loadCount.incrementAndGet();
            }
            if ("play".equals(method)) {
                playCount.incrementAndGet();
            }
        });

        PlaybackStarter.start(player, "song.mp3");

        assertEquals(1, loadCount.get());
        assertEquals(1, playCount.get());
    }

    private static Player player(Invocation invocation) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, arguments) -> {
                    invocation.call(method.getName(), arguments);
                    return primitiveDefault(method.getReturnType());
                });
    }

    private static Object primitiveDefault(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        return switch (type.getName()) {
            case "boolean" -> false;
            case "char" -> '\0';
            case "byte" -> (byte) 0;
            case "short" -> (short) 0;
            case "int" -> 0;
            case "long" -> 0L;
            case "float" -> 0F;
            default -> 0D;
        };
    }

    @FunctionalInterface
    private interface Invocation {
        void call(String method, Object[] arguments) throws Exception;
    }
}
