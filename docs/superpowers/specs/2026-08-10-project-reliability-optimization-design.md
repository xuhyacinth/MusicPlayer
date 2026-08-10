# 音乐播放器可靠性与跨平台优化设计

## 背景

当前项目能够在 Windows 上使用 Maven 构建，但代码审查发现播放索引越界、切歌线程竞态、频谱采样参数错误、暂停计时漂移、歌词状态残留、SQL 字符串拼接、测试被强制跳过和工程资源混放等问题。项目还固定依赖 Windows x64 SWT，仓库中的 macOS/Linux SQLite CLI 并不能使 UI 跨平台。

本次优化面向学习型项目，优先修复正确性和可验证性，再做与这些问题直接相关的结构清理。编译目标保持 Java 25，只使用稳定特性，不启用预览功能。

## 目标

1. 修复审查中确认的全部运行时问题。
2. 建立不会因快速切歌而串用资源的播放器生命周期。
3. 使用真实音频位置同步进度和歌词。
4. 使用参数化 SQL 处理歌曲信息和查询条件。
5. 让测试默认执行并覆盖关键纯逻辑和状态转换。
6. 支持 Windows x64、Linux x64、macOS x64 和 macOS ARM64 构建。
7. 清理未使用实现、实验源码、无效依赖配置和非标准资源位置。
8. 将最终架构、运行方法和知识点写入 README。

## 非目标

- 不重写 SWT 界面或引入完整 MVC 框架。
- 不加入在线播放、歌单同步、均衡器等新功能。
- 不使用 JDK 25 预览 API。
- 不声称在当前机器上完成 macOS 或 Linux 的本机 UI 运行验证。
- 不为所有 CPU 架构提供 SWT 构建；未列出的架构应明确失败。

## 方案选择

采用“可靠性优先的模块化优化”。保留现有 `Player` 抽象和 SWT 主窗口，重构播放器内部生命周期，提取需要独立测试的纯逻辑，并替换数据库 Wrapper 的值拼接方式。相比完整 MVC 重构，此方案能解决已知问题，同时控制行为变化和学习成本。

## 播放器设计

### PlaybackSession

`SdlFftPlayer` 仍是应用使用的播放器实现，但每次加载创建独立的 `PlaybackSession`。Session 持有：

- `AudioInputStream`；
- `SourceDataLine`；
- PCM 格式；
- 播放、暂停和停止状态；
- 播放与频谱任务引用；
- Session 自己的频谱缓冲区。

播放器只通过一个生命周期锁替换当前 Session。后台任务捕获自己的 Session，不再反复读取播放器实例上的可变 `audio` 和 `data` 字段。停止或切歌时按以下顺序执行：

1. 从播放器摘除旧 Session；
2. 标记停止并唤醒暂停等待；
3. 中断后台任务；
4. 关闭音频行和输入流；
5. 旧任务在 `finally` 中只清理自身，不修改新 Session 状态。

音频读取是阻塞式 I/O，使用命名虚拟线程。FFT 任务大部分时间等待刷新周期，也可使用独立虚拟线程，但同一 Session 始终只有一个 FFT 任务，不通过增加线程数量提高 CPU 计算速度。

### PCM 与频谱

输入统一转换为小端、16 位有符号 PCM。读取缓冲区长度必须是 `frameSize` 的整数倍。频谱分析接收真实的 `sampleSizeInBits` 和声道数，不再把采样率当作位深。

提取出的单声道样本进入 Session 私有环形缓冲区。FFT 计算完成后一次性发布不可变 `double[]` 快照，SWT 绘制线程只读取完整快照，避免并发 `clear/addAll` 产生中间状态。

### 时间与暂停

`Player.position()` 和 `duration()` 的单位统一为秒。当前位置按 `SourceDataLine.getLongFramePosition() / frameRate` 计算。暂停时停止音频行并等待，恢复时重新启动音频行并唤醒虚拟线程，因此 UI 进度和歌词不会继续前进。

## UI 与歌词

- 播放列表回绕统一使用可测试的索引计算函数，上一曲从第一首回到最后一首。
- 用户取消空播放列表的导入后重新检查列表，避免继续解引用空歌曲。
- 每次切歌先关闭歌词状态并清空旧歌词，再读取新文件。
- LRC 解析提取为无 SWT 依赖的纯函数，忽略非法时间标签并按时间排序。
- UI 由 SWT `timerExec` 定期读取播放器真实位置，避免 `Timer` 与 `asyncExec` 双层调度。
- 左侧显示当前时间，右侧显示总时长；暂停、停止和窗口销毁时停止刷新。

## 数据库设计

新增轻量的不可变 SQL 命令对象，包含 SQL 文本与参数列表。Query、Insert 和 Update Wrapper 只拼接受控的表名、字段名和操作符，所有值使用 `?` 占位符交给 `PreparedStatement`。

日期映射按目标字段类型转换：SQLite 文本时间解析为 `Date` 或 `java.time` 类型，不再把格式化字符串直接赋给日期字段。数据库位置使用 `Path.of("lib", "sqlite", "db", "MusicPlayer.db")` 构造。

Xerial SQLite JDBC 自己加载所需原生库，因此删除修改 `java.library.path` 的代码和三套未被调用的 SQLite CLI，只保留数据库文件。使用 SQLite CLI 对现有示例数据做一次结构化修正，将歌词路径改为 `lib/song/...`。

## 跨平台构建

Maven 通过操作系统与架构 Profile 设置 SWT artifact：

- `windows-x64`：`org.eclipse.swt.win32.win32.x86_64`；
- `linux-x64`：`org.eclipse.swt.gtk.linux.x86_64`；
- `macos-x64`：`org.eclipse.swt.cocoa.macosx.x86_64`；
- `macos-arm64`：`org.eclipse.swt.cocoa.macosx.aarch64`。

本机 Profile 自动激活，同时允许显式 `-P` 选择以验证依赖解析。Windows 完成编译、测试、打包和 UI 冒烟验证；其他 Profile 至少完成 Maven 依赖解析与编译验证，并在 README 标明本机验证边界。

## 工程清理

- 删除未完整实现且未被生产入口使用的 `ClipPlayer` 和重复的 `SdlPlayer`。
- 删除 `src/main/java/com/xu/Test.java` 及 `src/main/java/com/xu/music/player/test` 下的实验入口；有持续价值的纯逻辑由正式单元测试替代。
- 将 PNG 移到 `src/main/resources/com/xu/music/player/image`，删除把 `src/main/java` 当资源目录的 Maven 配置。
- 保留现有 `slf4j-simple`，删除不会生效的 `logback-spring.xml`，改用 `simplelogger.properties` 配置控制台日志。
- 移除未使用的版本属性和依赖，保留实际运行所需组件。

## 错误处理

- 文件不存在、格式不支持、音频设备不可用和数据库失败使用含上下文的领域异常。
- 正常停止、切歌导致的中断或资源关闭不记录为错误。
- 后台任务异常记录一次，并将对应 Session 转为停止状态。
- UI 捕获领域异常后记录日志并显示简短提示，不让 SWT 事件循环退出。

## 测试策略

先写失败测试，再修改实现。测试默认随 `mvn test` 和 `mvn package` 执行，覆盖：

1. 播放索引前后回绕和空列表。
2. LRC 合法、非法和乱序时间标签。
3. 单/双声道 16 位小端 PCM 转换与频谱输入。
4. 带单引号歌曲名的参数化 Insert、Query 和 Update。
5. SQLite 文本日期到实体日期字段的映射。
6. 旧 Session 结束不能改变新 Session 状态。
7. 暂停时位置不前进、停止后资源只关闭一次。
8. 示例数据库歌词路径存在。

音频硬件相关代码通过窄接口隔离；单元测试使用内存流和测试替身，不依赖开发机声卡。最终保留一个 Windows UI 冒烟检查，但不把人工检查伪装成自动化测试。

## 验收标准

- 所有新增与现有测试默认执行并通过。
- `mvn clean package` 在 Windows x64 成功，Shade JAR 包含主类、SWT 当前平台实现和图片资源。
- 快速连续切歌不会出现旧任务读取新资源或覆盖新状态。
- 第一首上一曲、暂停、无歌词切歌和单引号文件名导入行为正确。
- Windows 之外的三个目标 Profile 能解析 SWT 依赖并完成可执行的交叉编译检查。
- SQLite CLI 从仓库移除后数据库查询仍正常。
- README 与最终代码、命令和验证结果一致。
