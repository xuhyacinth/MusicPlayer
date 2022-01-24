package com.xu.music.player.sql;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Administrator
 */
public enum OperateSystemType {
    /**
     * Windows
     */
    WINDOWS(1, "Windows"), MAC_OS(2, "Mac OS"), MAC_OS_X(3, "Mac OS X"),
    LINUX(4, "Linux"), OS2(5, "OS/2"), SOLARIS(6, "Solaris"), SUN_OS(7, "SunOS"),
    MPEiX(8, "MPE/iX"), HP_UX(9, "HP-UX"), Aix(10, "AIX"), OS390(11, "OS/390"),
    FreeBSD(12, "FreeBSD"), Irix(13, "Irix"), DigitalUnix(14, "Digital Unix"),
    NetWare(15, "NetWare"), OSF1(16, "OSF1"), OpenVMS(17, "OpenVMS"), Any(18, "Any"), Others(19, "Others");

    public final int type;
    public final String name;

    OperateSystemType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static OperateSystemType getSystemType() {
        String type = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (StringUtils.containsOnly("digital", "unix")) {
            return OperateSystemType.DigitalUnix;
        } else if (StringUtils.containsAny(type, "mac", "os") && !StringUtils.equals(type, "x")) {
            return OperateSystemType.MAC_OS;
        } else if (StringUtils.containsAny(type, "mac", "os") && StringUtils.equals(type, "x")) {
            return OperateSystemType.MAC_OS_X;
        }
        switch (type) {
            case "linux":
                return OperateSystemType.LINUX;
            case "windows":
                return OperateSystemType.WINDOWS;
            case "os/2":
                return OperateSystemType.OS2;
            case "solaris":
                return OperateSystemType.SOLARIS;
            case "sunos":
                return OperateSystemType.SUN_OS;
            case "mpe/ix":
                return OperateSystemType.MPEiX;
            case "hp-ux":
                return OperateSystemType.HP_UX;
            case "aix":
                return OperateSystemType.Aix;
            case "os/390":
                return OperateSystemType.OS390;
            case "freebsd":
                return OperateSystemType.FreeBSD;
            case "irix":
                return OperateSystemType.Irix;
            case "netware":
                return OperateSystemType.NetWare;
            case "osf1":
                return OperateSystemType.OSF1;
            case "openvms":
                return OperateSystemType.OpenVMS;
            default:
                return OperateSystemType.Others;
        }
    }

    public static OperateSystemType getSystemMainType() {
        OperateSystemType type = getSystemType();
        if (type.type == 1) {
            return OperateSystemType.WINDOWS;
        } else if (type.type >= 2 && type.type <= 3) {
            return OperateSystemType.MAC_OS;
        } else if (type.type >= 5 && type.type <= 17) {
            return OperateSystemType.LINUX;
        } else {
            return OperateSystemType.Others;
        }
    }

}
