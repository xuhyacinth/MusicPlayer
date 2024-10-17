package com.xu.music.player.window

import cn.hutool.core.util.ArrayUtil
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.Shell
import java.io.File

/**
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
class SongChoose {

    /**
     * 歌曲选择
     *
     * @param shell 文件对话框
     * @date 2024年6月4日19点07分
     * @since idea
     */
    fun open(shell: Shell?) {
        try {
            val dialog = FileDialog(shell, SWT.OPEN or SWT.MULTI)
            dialog.filterExtensions = arrayOf("*.mp3", "*.MP3", "*.wav", "*.WAV", "*.flac", "*.FLAC", "*.pcm", "*.PCM")
            dialog.open()
            val files = dialog.fileNames
            if (ArrayUtil.isEmpty(files)) {
                return
            }
            for (file in files) {
                val paths = dialog.filterPath + File.separator + file
                println(paths)
            }
        } catch (e: Exception) {
            throw RuntimeException("参数错误")
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SongChoose().open(Shell())
        }
    }

}
