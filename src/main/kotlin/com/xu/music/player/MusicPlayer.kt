package com.xu.music.player

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import javafx.stage.StageStyle
import com.xu.music.player.main.MusicPlayerWindow
import com.xu.music.player.tray.MusicPlayerTray

/**
 * JavaFX 音乐播放器入口
 *
 * @date 2024年6月10日15点30分
 * @since V1.0.0.0
 */
class MusicPlayer : Application() {

    override fun start(stage: Stage) {
        // 无边框窗口，模拟 SWT 版本的自绘标题栏
        stage.initStyle(StageStyle.UNDECORATED)
        val window = MusicPlayerWindow(stage)
        val root = window.createContents()
        val scene = Scene(root, 900.0, 486.0)
        stage.scene = scene
        stage.title = "MusicPlayer"
        // 居中显示
        stage.x = (java.awt.Toolkit.getDefaultToolkit().screenSize.width - 900) / 2.0
        stage.y = (java.awt.Toolkit.getDefaultToolkit().screenSize.height - 486) / 2.0

        stage.setOnCloseRequest { e ->
            // 关闭时同时释放托盘与播放器
            MusicPlayerTray.dispose()
            window.exit()
        }

        stage.show()

        // 初始化系统托盘
        MusicPlayerTray.tray(stage)
    }

    override fun stop() {
        MusicPlayerTray.dispose()
        Platform.exit()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(MusicPlayer::class.java, *args)
        }
    }
}
