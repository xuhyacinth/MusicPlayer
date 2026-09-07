package com.xu.music.player.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VolumeControlTest {

    @Test
    public void verticalTrackRunsFromFullVolumeAtTopToMuteAtBottom() {
        assertEquals(100, VolumeControl.percentageAt(10, 129));
        assertEquals(50, VolumeControl.percentageAt(64, 129));
        assertEquals(0, VolumeControl.percentageAt(118, 129));
    }

    @Test
    public void draggingOutsideTrackClampsToItsEndpoints() {
        assertEquals(100, VolumeControl.percentageAt(-100, 129));
        assertEquals(0, VolumeControl.percentageAt(500, 129));
        assertEquals(0, VolumeControl.percentageAt(0, 0));
        assertEquals(0, VolumeControl.percentageAt(10, 20));
    }
}
