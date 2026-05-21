package com.xu.music.player.wrapper.sql;

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
        if (type.contains("windows")) {
            return SysType.WINDOWS;
        } else if (type.contains("mac") || type.contains("osx")) {
            if (type.contains("x")) {
                return SysType.MAC_OS_X;
            }
            return SysType.MAC_OS;
        } else if (type.contains("linux")) {
            return SysType.LINUX;
        } else if (type.contains("os/2")) {
            return SysType.OS2;
        } else if (type.contains("solaris")) {
            return SysType.SOLARIS;
        } else if (type.contains("sunos")) {
            return SysType.SUN_OS;
        } else if (type.contains("mpe/ix")) {
            return SysType.MPEIX;
        } else if (type.contains("hp-ux")) {
            return SysType.HP_UX;
        } else if (type.contains("aix")) {
            return SysType.AIX;
        } else if (type.contains("os/390")) {
            return SysType.OS390;
        } else if (type.contains("freebsd")) {
            return SysType.FREE_BSD;
        } else if (type.contains("irix")) {
            return SysType.IRIX;
        } else if (type.contains("netware")) {
            return SysType.NET_WARE;
        } else if (type.contains("osf1")) {
            return SysType.OSF1;
        } else if (type.contains("openvms")) {
            return SysType.OPEN_VMS;
        }
        return SysType.OTHERS;
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
