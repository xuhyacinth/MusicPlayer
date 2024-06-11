package com.xu.music.player.window;

import cn.hutool.core.util.ArrayUtil;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * @author hyacinth
 * @date 2024年6月4日19点07分
 * @since SWT-V1.0.0.0
 */
public class SongChoose {

    public static void main(String[] args) {
        new SongChoose().open(new Shell());
    }

    /**
     * 歌曲选择
     *
     * @param shell 文件对话框
     * @date 2024年6月4日19点07分
     * @since idea
     */
    public void open(Shell shell) {
        try {
            FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            dialog.setFilterExtensions(new String[]{"*.mp3", "*.MP3", "*.wav", "*.WAV", "*.flac", "*.FLAC", "*.pcm", "*.PCM"});
            dialog.open();
            String[] files = dialog.getFileNames();
            if (ArrayUtil.isEmpty(files)) {
                return;
            }
            for (String file : files) {
                String paths = dialog.getFilterPath() + File.separator + file;
                System.out.println(paths);
            }
        } catch (Exception e) {
            throw new RuntimeException("参数错误");
        }
    }


}
