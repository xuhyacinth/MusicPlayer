package com.xu.music.player.player;

import org.junit.Test;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** 显式启用才打开真实设备；测试静音播放，不访问数据库或用户歌曲。 */
public class AudioOutputDeviceTest {
    @Test public void defaultPolicyAndExplicitCompatibilityMatchRealDeviceCapabilities() throws Exception {
        assumeTrue(Boolean.getBoolean("musicplayer.audioTests"));
        var target = new AudioFormat(48000, 24, 2, true, false);
        var sample = getClass().getResource("/audio/stereo-48000-24bit.flac");
        boolean supported = AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, target));
        try (var player = SdlFftPlayer.create()) {
            if (supported) {
                player.load(sample);
                assertTrue(player.audioFormatDescription().contains("输出 48kHz/24bit/2ch"));
                assertFalse(player.audioFormatDescription().contains("兼容"));
            } else {
                var error = assertThrows(UnsupportedAudioOutputException.class, () -> player.load(sample));
                assertFalse(player.playing());
                assertEquals("", player.audioFormatDescription());
                assertTrue(error.compatibilityAvailable());
            }
        }
        try (var player = SdlFftPlayer.create(true)) {
            player.load(sample);
            String expected = supported ? "24bit" : "16bit";
            assertTrue(player.audioFormatDescription().contains("输出 48kHz/" + expected));
            player.setVolume(0);
            var completed = new CountDownLatch(1);
            player.onNaturalCompletion(completed::countDown);
            player.play();
            assertTrue("静音样本应自然播放结束", completed.await(5, TimeUnit.SECONDS));
            assertFalse(player.playing());
        }
    }
}
