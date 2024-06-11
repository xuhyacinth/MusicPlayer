package com.xu.music.player.sql;


import cn.hutool.core.util.StrUtil;

import java.util.Locale;

/**
 * 系统类型
 *
 * @author Administrator
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public enum SystemType {

    WINDOWS(1, "Windows"), MAC_OS(2, "Mac OS"), AIX(10, "AIX"),
    LINUX(4, "Linux"), OS2(5, "OS/2"), SOLARIS(6, "Solaris"),
    SUN_OS(7, "SunOS"), MPEIX(8, "MPE/iX"), HP_UX(9, "HP-UX"),
    OS390(11, "OS/390"), FREE_BSD(12, "FreeBSD"), IRIX(13, "Irix"),
    DIGITAL_UNIX(14, "Digital Unix"), NET_WARE(15, "NetWare"),
    OPEN_VMS(17, "OpenVMS"), ANY(18, "Any"), OTHERS(19, "Others"),
    MAC_OS_X(3, "Mac OS X"), OSF1(16, "OSF1");

    public final int type;
    public final String name;

    SystemType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static SystemType getSystemType() {
        String type = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (StrUtil.equals("digital", "unix")) {
            return SystemType.DIGITAL_UNIX;
        } else if (StrUtil.containsAny(type, "mac", "os") && !StrUtil.equals(type, "x")) {
            return SystemType.MAC_OS;
        } else if (StrUtil.containsAny(type, "mac", "os") && StrUtil.equals(type, "x")) {
            return SystemType.MAC_OS_X;
        }
        switch (type) {
            case "linux":
                return SystemType.LINUX;
            case "windows":
                return SystemType.WINDOWS;
            case "os/2":
                return SystemType.OS2;
            case "solaris":
                return SystemType.SOLARIS;
            case "sunos":
                return SystemType.SUN_OS;
            case "mpe/ix":
                return SystemType.MPEIX;
            case "hp-ux":
                return SystemType.HP_UX;
            case "aix":
                return SystemType.AIX;
            case "os/390":
                return SystemType.OS390;
            case "freebsd":
                return SystemType.FREE_BSD;
            case "irix":
                return SystemType.IRIX;
            case "netware":
                return SystemType.NET_WARE;
            case "osf1":
                return SystemType.OSF1;
            case "openvms":
                return SystemType.OPEN_VMS;
            default:
                return SystemType.OTHERS;
        }
    }

    public static SystemType getSystemMainType() {
        SystemType type = getSystemType();
        if (type.type == 1) {
            return SystemType.WINDOWS;
        } else if (type.type >= 2 && type.type <= 3) {
            return SystemType.MAC_OS;
        } else if (type.type >= 5 && type.type <= 17) {
            return SystemType.LINUX;
        } else {
            return SystemType.OTHERS;
        }
    }

}
