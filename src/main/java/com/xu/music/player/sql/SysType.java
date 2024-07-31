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
public enum SysType {

    WINDOWS(1, "Windows"), MAC_OS(2, "Mac OS"), AIX(10, "AIX"),
    LINUX(4, "Linux"), OS2(5, "OS/2"), SOLARIS(6, "Solaris"),
    SUN_OS(7, "SunOS"), MPEIX(8, "MPE/iX"), HP_UX(9, "HP-UX"),
    OS390(11, "OS/390"), FREE_BSD(12, "FreeBSD"), IRIX(13, "Irix"),
    DIGITAL_UNIX(14, "Digital Unix"), NET_WARE(15, "NetWare"),
    OPEN_VMS(17, "OpenVMS"), ANY(18, "Any"), OTHERS(19, "Others"),
    MAC_OS_X(3, "Mac OS X"), OSF1(16, "OSF1");

    public final int type;
    public final String name;

    SysType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static SysType getSystemType() {
        String type = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (StrUtil.equals("digital", "unix")) {
            return SysType.DIGITAL_UNIX;
        } else if (StrUtil.containsAny(type, "mac", "os") && !StrUtil.equals(type, "x")) {
            return SysType.MAC_OS;
        } else if (StrUtil.containsAny(type, "mac", "os") && StrUtil.equals(type, "x")) {
            return SysType.MAC_OS_X;
        }
        switch (type) {
            case "linux":
                return SysType.LINUX;
            case "windows":
                return SysType.WINDOWS;
            case "os/2":
                return SysType.OS2;
            case "solaris":
                return SysType.SOLARIS;
            case "sunos":
                return SysType.SUN_OS;
            case "mpe/ix":
                return SysType.MPEIX;
            case "hp-ux":
                return SysType.HP_UX;
            case "aix":
                return SysType.AIX;
            case "os/390":
                return SysType.OS390;
            case "freebsd":
                return SysType.FREE_BSD;
            case "irix":
                return SysType.IRIX;
            case "netware":
                return SysType.NET_WARE;
            case "osf1":
                return SysType.OSF1;
            case "openvms":
                return SysType.OPEN_VMS;
            default:
                return SysType.OTHERS;
        }
    }

    public static SysType getSystemMainType() {
        SysType type = getSystemType();
        if (type.type == 1) {
            return SysType.WINDOWS;
        } else if (type.type >= 2 && type.type <= 3) {
            return SysType.MAC_OS;
        } else if (type.type >= 5 && type.type <= 17) {
            return SysType.LINUX;
        } else {
            return SysType.OTHERS;
        }
    }

}
