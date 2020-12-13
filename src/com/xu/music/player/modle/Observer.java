package com.xu.music.player.modle;

import com.xu.music.player.entity.PlayerEntity;

/**
 * Java MusocPlayer 抽象 观察者 接口
 *
 * @Author: hyacinth
 * @ClassName: Observer
 * @Description: TODO
 * @Date: 2019年12月26日 下午8:02:58
 * @Copyright: hyacinth
 */
public interface Observer {

    void start(PlayerEntity entity);

    void stop();

    void end();

}

