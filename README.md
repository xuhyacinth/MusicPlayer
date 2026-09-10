# MusicPlayer

基于 **Kotlin + JavaFX + FXML + CSS** 的本地音乐播放器，使用 Maven 构建，SQLite 保存歌曲列表。

## 运行

当前构建配置：JDK 25、Kotlin 2.4.10、JavaFX 26.0.2。设置 `JAVA_HOME` 指向 JDK 25，然后在项目根目录执行：

```powershell
.\mvnw.cmd clean javafx:run
```

界面保留歌曲导入、歌曲列表、上一曲/下一曲、播放/暂停、歌词、频谱和系统托盘。关闭主窗口或从托盘退出时释放播放与定时刷新资源。

## 界面与操作

JavaFX 界面参考 `swt-java/v1.1.0` 分支，保留系统标题栏的最小化、最大化和关闭按钮，不引入 SWT 控件。

- 左侧歌曲列表默认约占四分之一宽度，可拖动分隔条调整；右侧显示歌词，正在播放的句子以蓝字、灰底高亮并保持上下居中；首尾补足留白，跳转进度、切歌和窗口缩放后同步定位。
- 单击歌曲只选中，双击歌曲行或按 Enter 开始播放。FLAC 在后台解码，底栏显示加载/失败状态；连续切歌只采用最后一次请求，关闭窗口会取消加载。
- 添加图标旁支持按歌曲名或歌手即时搜索，忽略大小写及首尾空格，点击清空按钮或按 Esc 恢复完整列表。
- 搜索不打断当前播放；上一曲、下一曲及自动续播使用筛选后的队列。筛选无结果时不切歌，也不弹出导入提示。
- 底部使用 32px 播放图标和 6px 细进度条，点击进度区域可定位，保留播放/暂停状态，并同步时间和歌词。
- 点击右侧音量图标弹出竖向滑块；再次点击图标、点击外部或按 Esc 收起。音量在切换歌曲时保留，不写入数据库，也不跨应用重启保存。
- 频谱绘制在底栏背景，不再额外占一行；双击底栏空白处切换频谱颜色。

## Windows 11 任务栏歌词

托盘右键菜单使用 Swing 绘制中文，避免 Windows UTF-8 环境下 AWT 原生菜单显示方框；支持按 Esc 或点击外部收起，不修改全局编码。

右键系统托盘中的播放器图标，勾选 **任务栏歌词**（默认关闭）。它是任务栏空白区域上的独立透明窗口，不修改 Explorer，也不会自动挤开任务栏图标。

- 正常播放显示当前句；没有歌词、歌词尚未开始或当前句为空时显示歌曲名和歌手。暂停时保留当前句，切歌或加载失败时清除旧句。
- 默认锁定位置，鼠标点击穿透，不抢焦点，不新增任务栏按钮或 Alt+Tab 项目。
- 取消 **锁定歌词位置（鼠标穿透）** 后出现蓝色调整背景：可横向拖动，滚轮调整宽度。调好后重新锁定。
- 托盘另有 **歌词变窄 / 歌词变宽 / 重置歌词位置和宽度**。超长文字省略显示，位置和宽度仅在本次运行内保留。
- 只覆盖主任务栏，按主屏显示缩放换算坐标。任务栏隐藏到屏幕边缘、Explorer 暂时不可用或前台窗口覆盖整屏时隐藏歌词，并每 500ms 检查恢复。
- 右侧预留托盘区域，但**不自动识别所有任务栏图标的空隙**。如与图标、其他播放器歌词重叠，请解锁后调整位置或缩窄；不需要时关闭。
- 主屏 Windows 11 的窗口创建、位置、焦点、穿透样式及退出释放有原生测试；多屏混合缩放、自动隐藏动画和各类独占全屏应用仍需实际环境验收。

原生接口使用 JNA 的 JPMS 模块。通过 Maven 启动已配置 `--enable-native-access=com.sun.jna`；手动使用模块路径启动时，请保留此授权及原有 JavaFX、SQLite 原生访问参数。非 Windows 不显示此菜单，不影响主播放器。

## 代码结构

```text
src/main/
├── java/module-info.java                       # JVM 模块声明
├── kotlin/com/xu/music/player/
│   ├── MusicPlayer.kt                          # FXML 加载与应用生命周期
│   ├── controller/MusicPlayerController.kt      # 事件绑定与动态界面更新
│   ├── player/                                 # 音频播放
│   ├── taskbar/                                # Windows 透明任务栏歌词及原生定位
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

测试覆盖 FXML/CSS/图片加载、正常/放大/最小尺寸布局、搜索与清空、播放队列切换、进度定位与歌词同步、百行歌词顺序播放及首中末行跳转居中、跨界面刷新周期的同句防抖、单行歌词与窗口缩放居中、音量弹窗、Kotlin Wrapper 增删改查，以及静音 WAV 原生播放、暂停定位和切歌音量保留。另覆盖 FLAC 样本边界、截断/无样本数文件、异步加载失败、过期请求和关闭取消。JavaFX 测试需要可用的桌面和媒体运行环境。

测试工作目录固定为 `target`，测试数据库只写入 `target/sqlite/db/MusicPlayer.db`，不会修改项目的真实数据库。测试还会生成界面快照 `target/fxml-preview.png`、`target/fxml-wide-preview.png`、`target/fxml-small-preview.png` 、`target/volume-preview.png` 和 `target/lyrics-center-preview.png`（使用测试数据，不包含系统标题栏）。任务栏歌词测试另生成 `target/taskbar-lyrics-preview.png`，覆盖文本回退、缩放坐标、穿透切换、窗口显隐及资源释放。

测试用 `playback-tone.flac` 是 FFmpeg 生成的 0.35 秒、44.1kHz 双声道 16-bit 正弦波，不包含真实歌曲。FLAC 转码按文件声明的样本数结束；没有有效总样本数的文件会明确报错，不进行无边界解码。

## 数据与迁移范围

- 真实数据库仍为项目根目录下的 `sqlite/db/MusicPlayer.db`，原有数据和表结构不变。
- 启动时以项目根目录为工作目录，保持数据库相对路径有效。
- 本次界面对齐保留 JavaFX 播放引擎和 SQLite 实现，不新增音频格式支持，也未验证独立安装包或 `jlink` 分发。
