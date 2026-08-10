package com.xu.music.player.tray;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class MusicPlayerTrayTest {

    @Test
    public void closeDelegatesToWindowOwner() {
        var closeCount = new AtomicInteger();
        var tray = new MusicPlayerTray(null, null, closeCount::incrementAndGet);

        tray.close();

        assertEquals(1, closeCount.get());
    }
}
