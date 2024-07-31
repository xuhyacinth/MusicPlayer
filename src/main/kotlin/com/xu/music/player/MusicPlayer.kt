package com.xu.music.player

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class HelloApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("music-player.fxml"))
        val scene = Scene(fxmlLoader.load(), 1000.0, 600.0)
        stage.title = "Hello!"
        stage.scene = scene
        var v = MusicPlayerController()
        v.start(stage)
        stage.show()
    }
}

fun main() {
    Application.launch(HelloApplication::class.java)
}