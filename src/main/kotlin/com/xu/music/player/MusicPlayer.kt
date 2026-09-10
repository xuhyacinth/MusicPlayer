package com.xu.music.player

import com.xu.music.player.controller.MusicPlayerController
import com.xu.music.player.tray.MusicPlayerTray
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

/** JavaFX 音乐播放器入口，界面由 FXML 加载。 */
class MusicPlayer : Application() {
    private var controller: MusicPlayerController? = null

    override fun start(stage: Stage) {
        val loader = FXMLLoader(MusicPlayer::class.java.getResource("view/music-player.fxml"))
        val root = loader.load<Parent>()
        val playerController = loader.getController<MusicPlayerController>()
        controller = playerController
        stage.scene = Scene(root, 900.0, 486.0)
        stage.title = "MusicPlayer"
        stage.minWidth = 640.0
        stage.minHeight = 360.0
        stage.centerOnScreen()
        stage.show()
        MusicPlayerTray.tray(stage)
        playerController.attach(stage)
    }

    override fun stop() {
        controller?.dispose()
        MusicPlayerTray.dispose()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(MusicPlayer::class.java, *args)
        }
    }
}
