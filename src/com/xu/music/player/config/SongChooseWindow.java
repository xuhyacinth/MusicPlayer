package com.xu.music.player.config;

import java.io.File;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import com.xu.music.player.sql.QueryWrapper;
import com.xu.music.player.sql.SongEntity;
import com.xu.music.player.sql.UpdateWrapper;
import com.xu.music.player.system.Constant;

import cn.hutool.core.collection.CollectionUtil;

/**
 * @author Administrator
 */
public class SongChooseWindow {

    public LinkedList<String> openChooseWindows(Shell shell) throws Exception {
        FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        dialog.setFilterNames(new String[]{"*.mp3", "*.MP3", "*.wav", "*.WAV", "*.flac", "*.FLAC", "*.pcm", "*.PCM"});
        dialog.open();
        String[] lists = dialog.getFileNames();
        String paths;
        int index = getMaxIndex() + 1;
        for (int i = 0, len = lists.length; i < len; i++) {
            paths = lists[i];
            if (paths.toLowerCase().endsWith(".mp3") || paths.toLowerCase().endsWith(".flac") || paths.toLowerCase().endsWith(".wav") || paths.toLowerCase().endsWith(".pcm")) {
                paths = dialog.getFilterPath() + File.separator + lists[i];

                SongEntity entity = new SongEntity();
                entity.setFlag(1);
                entity.setIndex(index);
                entity.setSongPath(paths);
                entity.setId((index + 1) + "");
                entity.setCreateBy("hyacinth");
                entity.setLyric(haveLyric(paths));
                entity.setCreateTime(LocalDateTime.now());
                entity.setName(getSongInfo(paths, true));
                entity.setLength((double) getSongLength(paths));
                entity.setAuthor(getSongInfo(paths, false));
                entity.setLyricPath(StringUtils.substring(paths, paths.lastIndexOf(".")) + ".lrc");

                UpdateWrapper wrapper = new UpdateWrapper<>(entity, "info");
                wrapper.insert();
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

    private int getMaxIndex() {
        QueryWrapper<SongEntity> wrapper = new QueryWrapper<>(SongEntity.class, "player");
        List<SongEntity> entities = wrapper.last("order by id desc limit 1").list();
        if (CollectionUtil.isEmpty(entities)) {
            return entities.get(0).getIndex();
        }
        return 0;
    }

}
