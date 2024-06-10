package com.xu.music.player

import javafx.fxml.FXML
import javafx.scene.control.Label

class MusicPlayerController {
    @FXML
    private lateinit var welcomeText: Label

    @FXML
    private fun onHelloButtonClick() {
        welcomeText.text = "Welcome to JavaFX Application!"
    }
}