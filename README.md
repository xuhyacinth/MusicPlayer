# MusicPlayer

基于 **Kotlin + JavaFX + FXML + CSS** 的本地音乐播放器，使用 Maven 构建，SQLite 保存歌曲列表。

## 运行

当前构建配置：JDK 25、Kotlin 2.4.10、JavaFX 26.0.2。设置 `JAVA_HOME` 指向 JDK 25，然后在项目根目录执行：

```powershell
.\mvnw.cmd clean javafx:run
```

界面保留歌曲导入、歌曲列表、上一曲/下一曲、播放/暂停、歌词、频谱和系统托盘。关闭主窗口或从托盘退出时释放播放与定时刷新资源。

## 代码结构

```text
src/main/
├── java/module-info.java                       # JVM 模块声明
├── kotlin/com/xu/music/player/
│   ├── MusicPlayer.kt                          # FXML 加载与应用生命周期
│   ├── controller/MusicPlayerController.kt      # 事件绑定与动态界面更新
│   ├── player/                                 # 音频播放
│   ├── window/SongChoose.kt                     # 系统文件选择器与导入
│   ├── sql/                                    # SQLite 访问
│   └── wrapper/                                # Kotlin 数据库操作封装
└── resources/com/xu/music/player/
    ├── view/music-player.fxml                  # 界面布局、控件和事件声明
    ├── css/music-player.css                    # 样式与歌词高亮
    └── image/                                  # 原有图片资源
```

修改静态布局使用 FXML，修改样式使用 CSS。FXML 中的 `fx:id` 对应控制器中的 `@FXML` 字段，`#方法名` 对应事件处理函数。歌词解析、播放状态和频谱绘制等动态逻辑仍使用 Kotlin。`SongChoose` 使用系统原生文件选择器，不另建 FXML 窗口。

`module-info.java` 是保留的唯一生产 Java 文件，用于声明模块依赖，并向 `javafx.fxml` 开放控制器包。原来的代码式主窗口和四个 Java Wrapper 已替换删除。

## 验证

```powershell
.\mvnw.cmd clean verify
```

测试覆盖 FXML/CSS/图片加载、尺寸绑定、播放按钮事件、上下曲与歌词加载、Kotlin Wrapper 增删改查，以及静音 WAV 原生播放。JavaFX 测试需要可用的桌面和媒体运行环境。

测试工作目录固定为 `target`，测试数据库只写入 `target/sqlite/db/MusicPlayer.db`，不会修改项目的真实数据库。测试还会生成界面快照 `target/fxml-preview.png`。

## 数据与迁移范围

- 真实数据库仍为项目根目录下的 `sqlite/db/MusicPlayer.db`，原有数据和表结构不变。
- 启动时以项目根目录为工作目录，保持数据库相对路径有效。
- 本次迁移保留既有布局和播放实现，不新增音频格式支持，也未验证独立安装包或 `jlink` 分发。
