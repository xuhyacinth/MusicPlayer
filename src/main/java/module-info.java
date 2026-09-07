module com.xu.music.player.musicplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.media;
    requires kotlin.stdlib;
    requires java.desktop;
    requires cn.hutool;
    requires java.sql;
    requires org.slf4j;

    exports com.xu.music.player;
    opens com.xu.music.player.controller to javafx.fxml;
}
