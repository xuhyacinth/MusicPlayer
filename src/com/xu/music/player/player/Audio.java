package com.xu.music.player.player;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Administrator
 */

public enum Audio {
    /**
     * Specifies a WAVE file.
     */
    WAVE(1, "WAVE", "wav"),
    /**
     * Specifies an AU file.
     */
    AU(2, "AU", "au"),
    /**
     * Specifies an AIFF file.
     */
    AIFF(3, "AIFF", "aif"),
    /**
     * Specifies an AIFF-C file.
     */
    AIFF_C(4, "AIFF-C", "aifc"),
    /**
     * Specifies an SND file.
     */
    SND(5, "SND", "snd"),
    /**
     * Specifies an MP3 file.
     */
    MP3(6, "MP3", "mp3"),
    /**
     * Specifies an FLAC file.
     */
    FLAC(7, "FLAC", "flac");

    private int index;
    private String type;
    private String suffix;

    Audio(int index, String type, String suffix) {
        this.index = index;
        this.type = type;
        this.suffix = suffix;
    }

    public static int getIndex(String name) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("文件名称为空！");
        }
        name = StringUtils.substring(name, StringUtils.lastIndexOf(name, ".") + 1);
        for (Audio audio : values()) {
            if (StringUtils.equalsIgnoreCase(audio.suffix, name)) {
                return audio.index;
            }
        }
        return -1;
    }

    public static boolean isSupport(String name) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("文件名称为空！");
        }
        name = StringUtils.substring(name, StringUtils.lastIndexOf(name, ".") + 1);
        for (Audio audio : values()) {
            if (StringUtils.equalsIgnoreCase(audio.suffix, name)) {
                return true;
            }
        }
        return false;
    }

    public int getIndex() {
        return index;
    }

}
