package com.xu.music.player.modle;

import com.xu.music.player.entity.PlayerEntity;
import com.xu.music.player.lyric.LyricyNotify;
import com.xu.music.player.system.Constant;
import com.xu.music.player.thread.LyricyThread;
import com.xu.music.player.thread.SpectrumThread;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * Java MusicPlayer 观察者
 *
 * @Author: hyacinth
 * @ClassName: LyricPlayer
 * @Description: TODO
 * @Date: 2019年12月26日 下午8:04:08
 * @Copyright: hyacinth
 */
@SuppressWarnings(value = "all")
public class Controller implements Observer {

    private static LyricyThread lyricy = null;// 歌词
    private static SpectrumThread spectrum = null;// 频谱
    private int merchant = 0;
    private int remainder = 0;
    private String format = "";

    @Override
    public void start(PlayerEntity entity) {
        if (Constant.PLAYING_SONG_HAVE_LYRIC && Constant.MUSIC_PLAYER_SYSTEM_START_LYRIC) {
            startLyricPlayer(entity);
        }
        if (Constant.MUSIC_PLAYER_SYSTEM_START_SPECTRUM) {
            startSpectrumPlayer(entity);
        }
    }

    private String format(int time) {
        merchant = time / 60;
        remainder = time % 60;
        if (time < 10) {
            format = "00:0" + time;
        } else if (time < 60) {
            format = "00:" + time;
        } else {
            if (merchant < 10 && remainder < 10) {
                format = "0" + merchant + ":0" + remainder;
            } else if (merchant < 10 && remainder < 60) {
                format = "0" + merchant + ":" + remainder;
            } else if (merchant >= 10 && remainder < 10) {
                format = merchant + ":0" + remainder;
            } else if (merchant >= 10 && remainder < 60) {
                format = merchant + ":0" + remainder;
            }
        }
        return format;
    }

    @Override
    public void end() {
        if (Constant.PLAYING_SONG_HAVE_LYRIC && Constant.MUSIC_PLAYER_SYSTEM_START_LYRIC) {
            System.out.println("观察者 结束所有 歌词线程");
            endLyricPlayer();
        }
        if (Constant.MUSIC_PLAYER_SYSTEM_START_SPECTRUM) {
            System.out.println("观察者 结束所有 频谱线程");
            endSpectrumPlayer();
        }
        System.gc();
    }

    @Override
    public void stop() {
        stopLyricPlayer();
        stopSpectrumPlayer();
    }

    public void startLyricPlayer(PlayerEntity entity) {
        int length = Integer.parseInt(Constant.PLAYING_SONG_NAME.split(Constant.MUSIC_PLAYER_SYSTEM_SPLIT)[3]);
        PlayerEntity.getBar().setMaximum(length);
        PlayerEntity.getBar().setSelection(0);
        if (lyricy != null) {
            //TODO:
        } else {
            lyricy = new LyricyThread(new LyricyNotify() {
                @Override
                public void lyric(double lrc, double pro) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            TableItem[] items = PlayerEntity.getTable().getItems();
                            for (int i = 0, len = items.length; i < len; i++) {
                                if (i == lrc) {
                                    items[i].setBackground(SWTResourceManager.getColor(SWT.COLOR_GRAY));//将选中的行的颜色变为蓝色
                                    if (i > 7) {
                                        PlayerEntity.getTable().setTopIndex(i - 7);
                                    }
                                } else {
                                    items[i].setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));//将选中的行的颜色变为蓝色
                                }
                            }
                            PlayerEntity.getBar().setSelection((int) pro);
                            PlayerEntity.getText().setText(format((int) pro));
                        }
                    });
                }
            });
            lyricy.setDaemon(true);
            lyricy.start();
        }
    }

    public void startSpectrumPlayer(PlayerEntity entity) {
        if (Constant.SPECTRUM_TOTAL_HEIGHT == 0) {
            Constant.SPECTRUM_TOTAL_HEIGHT = PlayerEntity.getSpectrum().getClientArea().height;
            Constant.SPECTRUM_TOTAL_WIDTH = PlayerEntity.getSpectrum().getClientArea().width;
            if (Constant.SPECTRUM_STYLE == 0) {
                Constant.SPECTRUM_TOTAL_NUMBER = PlayerEntity.getSpectrum().getClientArea().width / 5;
                Constant.SPECTRUM_SPLIT_WIDTH = 5;
            } else if (Constant.SPECTRUM_STYLE == 1) {
                Constant.SPECTRUM_TOTAL_NUMBER = PlayerEntity.getSpectrum().getClientArea().width / 20;
                Constant.SPECTRUM_SPLIT_WIDTH = 20;
            }
            Constant.SPECTRUM_SAVE_INIT_SIZE = Constant.SPECTRUM_TOTAL_NUMBER;
        }
        if (spectrum == null) {
            spectrum = new SpectrumThread(PlayerEntity.getSpectrum());
            spectrum.setDaemon(true);
            spectrum.start();
        }
    }

    public void endLyricPlayer() {
        if (lyricy != null) {
            lyricy.stop();
            lyricy = null;
        }
    }

    public void endSpectrumPlayer() {
        if (spectrum != null) {
            spectrum.stop();
            spectrum = null;
        }
    }

    public void stopLyricPlayer() {
        if (lyricy != null) {
            try {
                lyricy.wait(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSpectrumPlayer() {
        if (spectrum != null) {
            try {
                spectrum.wait(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

