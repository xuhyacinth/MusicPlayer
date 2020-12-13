package com.xu.music.player.modle;

import com.xu.music.player.entity.PlayerEntity;

/**
 * Java MusocPlayer 被观察者
 *
 * @Author: hyacinth
 * @ClassName: LyricyServer
 * @Description: TODO
 * @Date: 2019年12月26日 下午8:03:42
 * @Copyright: hyacinth
 */
public class ControllerServer implements Observed {

    @Override
    public void startLyricPlayer(Observer observer, PlayerEntity entity) {
        observer.start(entity);
    }

    @Override
    public void startSpectrumPlayer(Observer observer, PlayerEntity entity) {
        observer.start(entity);
    }

    @Override
    public void endLyricPlayer(Observer observer) {
        observer.end();
    }

    @Override
    public void endSpectrumPlayer(Observer observer) {
        observer.end();
    }

    @Override
    public void stopLyricPlayer(Observer observer) {
        observer.stop();
    }

    @Override
    public void stopSpectrumPlayer(Observer observer) {
        observer.stop();
    }

}

