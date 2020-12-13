package com.xu.music.player.config;

import com.xu.music.player.system.Constant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import java.io.File;
import java.util.LinkedList;

public class SongChoiceWindow {

    public static void updateSongList(Table table, LinkedList<String> lists) {
        table.removeAll();
        TableItem tableItem;
        for (int i = 0, len = lists.size(); i < len; i++) {
            tableItem = new TableItem(table, SWT.NONE);
            tableItem.setText(new String[]{(i + 1) + "", lists.get(i).split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[1]});
        }
    }

    public LinkedList<String> openChoiseWindows(Shell shell) {
        FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        dialog.setFilterNames(new String[]{"*.mp3", "*.MP3", "*.wav", "*.WAV", "*.flac", "*.FLAC", "*.pcm", "*.PCM"});
        dialog.open();
        String[] lists = dialog.getFileNames();
        String paths;
        Constant.MUSIC_PLAYER_SONGS_TEMP_LIST.clear();
        for (int i = 0, len = lists.length; i < len; i++) {
            paths = lists[i];
            if (paths.toLowerCase().endsWith(".mp3") || paths.toLowerCase().endsWith(".flac") || paths.toLowerCase().endsWith(".wav") || paths.toLowerCase().endsWith(".pcm")) {
                paths = dialog.getFilterPath() + File.separator + lists[i];
                paths = paths + Constant.MUSIC_PLAYER_SYSTEM_SPLIT + haveLyric(paths);
                Constant.MUSIC_PLAYER_SONGS_TEMP_LIST.add(paths);
            }
        }
        new Writing().write(Constant.MUSIC_PLAYER_SONGS_TEMP_LIST);
        return Constant.MUSIC_PLAYER_SONGS_TEMP_LIST;
    }

    private String haveLyric(String path) {
        path = path.substring(0, path.lastIndexOf("."));
        path += ".lrc";
        if (!(new File(path).exists())) {
            return "N";
        } else {
            return "Y";
        }
    }

}
