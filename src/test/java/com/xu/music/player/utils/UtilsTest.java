package com.xu.music.player.utils;

import org.eclipse.swt.SWT;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void confirmationStyleIncludesYesNoCancelAndWarningIcon() {
        int style = Utils.CONFIRMATION_STYLE;

        assertEquals(SWT.YES, style & SWT.YES);
        assertEquals(SWT.NO, style & SWT.NO);
        assertEquals(SWT.CANCEL, style & SWT.CANCEL);
        assertEquals(SWT.ICON_WARNING, style & SWT.ICON_WARNING);
    }
}
