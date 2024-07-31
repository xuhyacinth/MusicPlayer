package com.xu.music.player

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.stage.Stage
import java.io.File
import kotlin.random.Random


class MusicPlayerController {

    lateinit var table: TableView<RandomNumber>

    @FXML
    private lateinit var play: Label

    @FXML
    private fun onMouseEntered() {
        play.text = "Welcome to JavaFX Application!"
    }

    fun play(event: MouseEvent) {
        // 创建 Media 对象
        val media = Media(File("D:\\Kugou\\梦涵 - 加减乘除.mp3").toURI().toString())
        // 创建 MediaPlayer 对象
        val player = MediaPlayer(media)
        // 播放音频
        player.play()
    }


    // 定义数据类
    data class RandomNumber(val number: Int)

     fun start(primaryStage: Stage) {
        // 初始化 TableView
        table = TableView()

        // 创建列
        val numberColumn = TableColumn<RandomNumber, Int>("Random Number")
        numberColumn.cellValueFactory = PropertyValueFactory("number")

        // 将列添加到表格
        table.columns.add(numberColumn)

        // 生成随机数数据
        val data: ObservableList<RandomNumber> = FXCollections.observableArrayList()
        repeat(10) {
            data.add(RandomNumber(Random.nextInt(100)))
        }

        // 将数据添加到表格
        table.items = data

        // 创建布局并将表格添加到布局中
        val vbox = VBox(table)
        val scene = Scene(vbox)
//
//        // 设置舞台
//        primaryStage.scene = scene
//        primaryStage.title = "TableView Example"
//        primaryStage.width = 300.0
//        primaryStage.height = 400.0
//        primaryStage.show()
    }
}