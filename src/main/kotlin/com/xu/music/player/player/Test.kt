package com.xu.music.player.player

fun main() {
    val player = SourceDataLinePlayer.createPlayer()
    player.load("E:\\KuGou\\鱼蛋 - 漂泊的情人.mp3")
    player.play()
}