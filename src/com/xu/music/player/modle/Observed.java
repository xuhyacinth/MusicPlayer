package com.xu.music.player.modle;

/**
 * Java MusicPlayer 抽象 被观察者 接口
 *
 * @Author: hyacinth
 * @ClassName: Observed
 * @Description: TODO
 * @Date: 2019年12月26日 下午8:02:38
 * @Copyright: hyacinth
 */
public interface Observed {

    void startLyricPlayer(Observer observer);

    void startSpectrumPlayer(Observer observer);

    void endLyricPlayer(Observer observer);

    void endSpectrumPlayer(Observer observer);

    void stopLyricPlayer(Observer observer);

    void stopSpectrumPlayer(Observer observer);

}

