package com.xu.music.player.config;

import java.io.File;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import com.xu.music.player.sql.SongEntity;
import com.xu.music.player.system.Constant;

/**
 * @author Administrator
 */
public class SongChooseWindow {

    public LinkedList<String> openChooseWindows(Shell shell) {
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
                SongEntity entity = new SongEntity();
                entity.setAuthor("hyacinth");
                entity.setSongPath(paths);
                entity.setAuthor(getSongInfo(paths, false));
                entity.setName(getSongInfo(paths, true));
                entity.setLyric(haveLyric(paths));
                entity.setLyricPath(StringUtils.substring(paths, paths.lastIndexOf(".")) + ".lrc");
                entity.setCreateTime(LocalDateTime.now());
                Constant.MUSIC_PLAYER_SONGS_LIST.add(entity);
            }
        }
        return Constant.MUSIC_PLAYER_SONGS_TEMP_LIST;
    }

    private int haveLyric(String path) {
        path = path.substring(0, path.lastIndexOf("."));
        path += ".lrc";
        if (!(new File(path).exists())) {
            return 0;
        } else {
            return 1;
        }
    }

    private int getSongLength(String path) {
        File file = new File(path);
        AudioFile mp3 = null;
        try {
            mp3 = AudioFileIO.read(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(mp3).getAudioHeader().getTrackLength();
    }

    private String getSongInfo(String path, boolean name) {
        path = StringUtils.substring(path, path.lastIndexOf("\\") + 1);
        if (name) {
            return path.split(" - ")[1];
        } else {
            return path.split(" - ")[0];
        }
    }

}
