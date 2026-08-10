package com.xu.music.player.utils

import java.util.*


/**
 * 系统类型
 *
 * @author Administrator
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
enum class SysType(val type: Int, val names: String) {

    WINDOWS(1, "Windows"), MAC_OS(2, "Mac OS"), AIX(10, "AIX"),
    LINUX(4, "Linux"), OS2(5, "OS/2"), SOLARIS(6, "Solaris"),
    SUN_OS(7, "SunOS"), MPEIX(8, "MPE/iX"), HP_UX(9, "HP-UX"),
    OS390(11, "OS/390"), FREE_BSD(12, "FreeBSD"), IRIX(13, "Irix"),
    DIGITAL_UNIX(14, "Digital Unix"), NET_WARE(15, "NetWare"),
    OPEN_VMS(17, "OpenVMS"), ANY(18, "Any"), OTHERS(19, "Others"),
    MAC_OS_X(3, "Mac OS X"), OSF1(16, "OSF1");

    companion object {
        fun getType(): SysType {
            val type = System.getProperty("os.name").lowercase(Locale.ROOT)
            if (type.equals("digital", ignoreCase = true)) {
                return DIGITAL_UNIX
            } else if (type.contains("mac", ignoreCase = true) || type.contains(
                    "os",
                    ignoreCase = true
                ) && !type.equals("x", ignoreCase = true)
            ) {
                return MAC_OS
            } else if (type.contains("mac", ignoreCase = true) || type.contains("os", ignoreCase = true) && type.equals(
                    "x",
                    ignoreCase = true
                )
            ) {
                return MAC_OS_X
            }
            return when (type) {
                "linux" -> LINUX
                "windows" -> WINDOWS
                "os/2" -> OS2
                "solaris" -> SOLARIS
                "sunos" -> SUN_OS
                "mpe/ix" -> MPEIX
                "hp-ux" -> HP_UX
                "aix" -> AIX
                "os/390" -> OS390
                "freebsd" -> FREE_BSD
                "irix" -> IRIX
                "netware" -> NET_WARE
                "osf1" -> OSF1
                "openvms" -> OPEN_VMS
                else -> OTHERS
            }
        }

        fun getMainType(): SysType {
            val type = getType()
            return when (type.type) {
                1 -> WINDOWS
                in 2..3 -> MAC_OS
                in 5..17 -> LINUX
                else -> OTHERS
            }
        }
    }

}