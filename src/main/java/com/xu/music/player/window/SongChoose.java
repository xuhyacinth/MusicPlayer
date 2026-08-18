package com.xu.music.player.window;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import com.xu.music.player.entity.SongEntity;
import com.xu.music.player.wrapper.InsertWrapper;
import com.xu.music.player.wrapper.QueryWrapper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author hyacinth
 * @since 2024年6月4日19点07分
 * @version swt-java/v1.0.0
 */
public class SongChoose {

    public static void main(String[] args) {
        new SongChoose().open(new Shell());
    }

    /**
     * 歌曲选择并插入数据库
     *
     * @param shell 文件对话框
     * @since 2024年6月4日19点07分
     * @return 成功导入的歌曲数
     * @since idea
     */
    public int open(Shell shell) {
        try {
            FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            dialog.setFilterExtensions(new String[]{"*.mp3", "*.MP3", "*.wav", "*.WAV", "*.flac", "*.FLAC", "*.pcm", "*.PCM"});
            if (dialog.open() == null) {
                return 0;
            }
            String[] files = dialog.getFileNames();
            if (ArrayUtil.isEmpty(files)) {
                return 0;
            }

            int currentCount = 0;
            try {
                QueryWrapper<SongEntity> queryWrapper = new QueryWrapper<>(SongEntity.class, "song");
                List<SongEntity> existing = queryWrapper.list();
                if (existing != null) {
                    currentCount = existing.size();
                }
            } catch (Exception e) {
                // 忽略异常，默认从0开始排序
            }

            int importedCount = 0;
            for (String file : files) {
                String paths = dialog.getFilterPath() + File.separator + file;
                File audioFile = new File(paths);
                if (!audioFile.exists()) {
                    continue;
                }

                // 获取音频长度
                double duration = getAudioDuration(audioFile);

                SongEntity song = new SongEntity();
                song.setId(IdUtil.fastSimpleUUID());
                String songName = FileUtil.mainName(audioFile);
                song.setName(songName);
                song.setSongPath(paths);
                song.setLength(duration);

                // 解析歌手
                if (songName.contains(" - ")) {
                    String[] parts = songName.split(" - ", 2);
                    song.setAuthor(parts[0].trim());
                } else {
                    song.setAuthor("未知歌手");
                }

                // 尝试匹配同名歌词
                String lyricPath = dialog.getFilterPath() + File.separator + songName + ".lrc";
                if (FileUtil.exist(lyricPath)) {
                    song.setLyricPath(lyricPath);
                } else {
                    lyricPath = dialog.getFilterPath() + File.separator + songName + ".LRC";
                    if (FileUtil.exist(lyricPath)) {
                        song.setLyricPath(lyricPath);
                    }
                }

                song.setFlag(1);
                song.setIndex(currentCount + importedCount + 1);
                song.setCreateTime(new Date());
                song.setUpdateTime(new Date());

                // 插入数据库
                InsertWrapper<SongEntity> insertWrapper = new InsertWrapper<>(song, "song");
                insertWrapper.insert();
                importedCount++;
            }
            return importedCount;
        } catch (Exception e) {
            throw new RuntimeException("导入歌曲失败", e);
        }
    }

    /**
     * 获取音频时长
     *
     * @param file 音频文件
     * @return 时长(秒)
     */
    private double getAudioDuration(File file) {
        // 优先通过文件格式元数据获取属性(主要支持 MP3 SPI)
        try {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            Map<String, Object> properties = fileFormat.properties();
            if (properties != null && properties.containsKey("duration")) {
                Object durationObj = properties.get("duration");
                if (durationObj instanceof Long) {
                    return ((Long) durationObj) / 1_000_000.0;
                } else if (durationObj instanceof Number) {
                    return ((Number) durationObj).doubleValue() / 1_000_000.0;
                }
            }
        } catch (Exception e) {
            // 忽略，尝试其它方式
        }

        // 通过音频输入流获取帧数与帧率计算(支持 WAV, FLAC 等)
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(file)) {
            javax.sound.sampled.AudioFormat format = audioStream.getFormat();
            long frameLength = audioStream.getFrameLength();
            float frameRate = format.getFrameRate();
            if (frameLength != AudioSystem.NOT_SPECIFIED && frameRate > 0) {
                return (double) frameLength / frameRate;
            }
        } catch (Exception e) {
            // 忽略
        }

        return 0.0;
    }

}
