# Music Player Reliability and Cross-Platform Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (<code>- [ ]</code>) syntax for tracking.

**Goal:** Fix the confirmed runtime, concurrency, data-access, testing, packaging, and documentation problems while keeping the SWT application recognizable and targeting Java 25.

**Architecture:** Keep the SWT window and <code>Player</code> boundary, but isolate pure playlist/LRC/PCM logic, make each playback load own an independent Session, and represent database work as parameterized immutable SQL commands. Platform-specific SWT dependencies live in Maven profiles; production resources and tests move to their standard Maven locations.

**Tech Stack:** Java 25, SWT/JFace, Java Sound, virtual threads, JTransforms, SQLite JDBC, Hutool, SLF4J Simple, JUnit 4, Maven Surefire and Shade.

---

## File Map

- Create <code>src/main/java/com/xu/music/player/main/PlaylistNavigator.java</code>: pure next/previous index wrapping.
- Create <code>src/main/java/com/xu/music/player/lyric/LrcLine.java</code>: immutable timed lyric value.
- Create <code>src/main/java/com/xu/music/player/lyric/LrcParser.java</code>: UTF-8 LRC parsing without SWT.
- Create <code>src/main/java/com/xu/music/player/player/PcmSpectrumAnalyzer.java</code>: PCM frame decoding, ring buffer, FFT snapshot publication.
- Create <code>src/main/java/com/xu/music/player/player/PlaybackSession.java</code>: one load's resources, pause state, position and idempotent close.
- Create <code>src/main/java/com/xu/music/player/player/PlaybackSessionSlot.java</code>: atomic replacement and identity-safe completion.
- Modify <code>src/main/java/com/xu/music/player/player/Player.java</code>: define position/duration seconds and closeable lifecycle.
- Rewrite <code>src/main/java/com/xu/music/player/player/SdlFftPlayer.java</code>: Session-owned resources and virtual-thread tasks.
- Create <code>src/main/java/com/xu/music/player/main/PlaybackProgress.java</code>: pure bounded progress calculation.
- Modify <code>src/main/java/com/xu/music/player/main/MusicPlayer.java</code>: corrected navigation, lyrics, real position, lifecycle-safe SWT refresh.
- Create <code>src/main/java/com/xu/music/player/wrapper/sql/SqlCommand.java</code>: immutable SQL plus parameters.
- Modify Wrapper and helper classes: build and execute prepared commands and convert dates correctly.
- Modify <code>pom.xml</code>: enable tests, add SWT platform profiles, normalize resources, remove unused settings.
- Create focused tests under <code>src/test/java/com/xu/music/player</code>.
- Move images to <code>src/main/resources/com/xu/music/player/image</code>.
- Create <code>src/main/resources/simplelogger.properties</code>; remove dead Logback configuration.
- Delete unused players, demo main sources, and SQLite CLI directories.
- Rewrite <code>README.md</code> as the approved learning guide.

### Task 1: Make Tests Real and Add Playlist Navigation

**Files:**
- Modify: <code>pom.xml</code>
- Create: <code>src/main/java/com/xu/music/player/main/PlaylistNavigator.java</code>
- Create: <code>src/test/java/com/xu/music/player/main/PlaylistNavigatorTest.java</code>
- Modify: <code>src/main/java/com/xu/music/player/main/MusicPlayer.java</code>
- Delete: <code>src/test/java/com/xu/music/player/AppTest.java</code>

- [ ] **Step 1: Enable Surefire and write the failing navigation tests**

Remove the hard-coded <code>&lt;skipTests&gt;true&lt;/skipTests&gt;</code>. Add:

~~~java
public class PlaylistNavigatorTest {
    @Test public void previousWrapsFromFirstToLast() {
        assertEquals(3, PlaylistNavigator.move(0, 4, -1));
    }

    @Test public void nextWrapsFromLastToFirst() {
        assertEquals(0, PlaylistNavigator.move(3, 4, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPlaylistIsRejected() {
        PlaylistNavigator.move(0, 0, 1);
    }
}
~~~

- [ ] **Step 2: Run the focused test and verify it fails**

Run: <code>mvn -q -Dtest=PlaylistNavigatorTest test</code>

Expected: compilation failure because <code>PlaylistNavigator</code> does not exist.

- [ ] **Step 3: Implement the pure navigator**

~~~java
public final class PlaylistNavigator {
    private PlaylistNavigator() {}

    public static int move(int current, int size, int delta) {
        if (size <= 0) {
            throw new IllegalArgumentException("播放列表不能为空");
        }
        return Math.floorMod(current + delta, size);
    }
}
~~~

Replace manual boundary code in <code>MusicPlayer.next</code> with <code>PlaylistNavigator.move</code>, and return if the list is still empty after the add-song dialog closes.

- [ ] **Step 4: Run tests and commit**

Run: <code>mvn -q -Dtest=PlaylistNavigatorTest test</code>

Expected: 3 tests pass.

Commit:

~~~powershell
git add pom.xml src/main/java/com/xu/music/player/main src/test/java/com/xu/music/player/main src/test/java/com/xu/music/player/AppTest.java
git commit -m "修复：启用测试并修正播放列表回绕"
~~~

### Task 2: Extract and Correct LRC Handling

**Files:**
- Create: <code>src/main/java/com/xu/music/player/lyric/LrcLine.java</code>
- Create: <code>src/main/java/com/xu/music/player/lyric/LrcParser.java</code>
- Create: <code>src/test/java/com/xu/music/player/lyric/LrcParserTest.java</code>
- Modify: <code>src/main/java/com/xu/music/player/main/MusicPlayer.java</code>

- [ ] **Step 1: Write failing parser tests**

~~~java
public class LrcParserTest {
    @Test public void parsesAndSortsTimedLines() {
        var lines = LrcParser.parse(List.of("[00:02.50]后", "[00:01.00]前", "[ar:作者]"));
        assertEquals(List.of(
                new LrcLine(1.0, "[00:01.00]", "前"),
                new LrcLine(2.5, "[00:02.50]", "后")), lines);
    }

    @Test public void ignoresMalformedTime() {
        assertTrue(LrcParser.parse(List.of("[bad]歌词", "纯文本")).isEmpty());
    }
}
~~~

- [ ] **Step 2: Verify red**

Run: <code>mvn -q -Dtest=LrcParserTest test</code>

Expected: compilation failure for missing lyric classes.

- [ ] **Step 3: Implement parser and integrate reset-first behavior**

~~~java
public record LrcLine(double seconds, String tag, String text) {}

public final class LrcParser {
    private static final Pattern LINE = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)$");

    public static List<LrcLine> parse(List<String> source) {
        return source.stream().map(LrcParser::parseLine)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(LrcLine::seconds))
                .toList();
    }

    private static Optional<LrcLine> parseLine(String value) {
        var matcher = LINE.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            var seconds = Double.parseDouble(matcher.group(1)) * 60
                    + Double.parseDouble(matcher.group(2));
            return Optional.of(new LrcLine(
                    seconds, value.substring(0, value.indexOf(']') + 1), matcher.group(3)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
~~~

At the start of <code>MusicPlayer.initLyric</code>, always set <code>PLAYING_LYRIC = false</code> and call <code>lyrics.removeAll()</code>. Populate the table from <code>LrcParser</code> only when the path exists.

- [ ] **Step 4: Verify and commit**

Run: <code>mvn -q -Dtest=LrcParserTest,PlaylistNavigatorTest test</code>

Expected: all focused tests pass.

Commit:

~~~powershell
git add src/main/java/com/xu/music/player/lyric src/main/java/com/xu/music/player/main/MusicPlayer.java src/test/java/com/xu/music/player/lyric
git commit -m "修复：清理并统一歌词解析状态"
~~~

### Task 3: Parameterize SQL and Preserve Date Types

**Files:**
- Create: <code>src/main/java/com/xu/music/player/wrapper/sql/SqlCommand.java</code>
- Modify: <code>src/main/java/com/xu/music/player/wrapper/BasicWrapper.java</code>
- Modify: <code>src/main/java/com/xu/music/player/wrapper/QueryWrapper.java</code>
- Modify: <code>src/main/java/com/xu/music/player/wrapper/InsertWrapper.java</code>
- Modify: <code>src/main/java/com/xu/music/player/wrapper/UpdateWrapper.java</code>
- Modify: <code>src/main/java/com/xu/music/player/wrapper/sql/Helper.java</code>
- Modify: <code>src/main/java/com/xu/music/player/wrapper/sql/NewHelper.java</code>
- Create: <code>src/test/java/com/xu/music/player/wrapper/SqlWrapperTest.java</code>
- Create: <code>src/test/java/com/xu/music/player/wrapper/DatabaseMappingTest.java</code>

- [ ] **Step 1: Write failing command and mapping tests**

~~~java
@Test public void insertKeepsApostropheAsParameter() {
    var song = new SongEntity();
    song.setName("Don't Stop");
    var command = new InsertWrapper<>(song, "song").command();
    assertTrue(command.sql().contains("values(?)"));
    assertEquals(List.of("Don't Stop"), command.parameters());
}

@Test public void sqliteTextTimestampMapsToDate() throws Exception {
    var path = Files.createTempFile("music-player-", ".db");
    try {
        var helper = new NewHelper(path);
        helper.update("create table song (id text, create_time text)");
        helper.update("insert into song(id, create_time) values(?, ?)",
                "1", "2024-06-06 20:47:02");
        var songs = helper.select("select * from song", SongEntity.class);
        assertNotNull(songs.getFirst().getCreateTime());
    } finally {
        Files.deleteIfExists(path);
    }
}
~~~

- [ ] **Step 2: Verify red**

Run: <code>mvn -q -Dtest=SqlWrapperTest,DatabaseMappingTest test</code>

Expected: missing <code>command()</code>/<code>SqlCommand</code> and date assertion failure.

- [ ] **Step 3: Implement immutable commands and typed conversion**

~~~java
public record SqlCommand(String sql, List<Object> parameters) {
    public SqlCommand {
        parameters = List.copyOf(parameters);
    }
}
~~~

Each Wrapper stores SQL fragments separately from values. <code>eq</code> appends <code>field = ?</code>; <code>like</code> appends <code>field like ?</code> with the wildcard in the parameter. Insert/update field values become placeholders. <code>NewHelper</code> always prepares the statement and binds <code>SqlCommand.parameters()</code>. Add <code>NewHelper(Path database)</code> for isolated tests while the no-argument constructor delegates to the project database path.

For a <code>Date</code> target and SQLite string source, parse <code>yyyy-MM-dd HH:mm:ss</code> to <code>LocalDateTime</code>, apply the system zone, and assign <code>Date.from(...)</code>.

- [ ] **Step 4: Verify and commit**

Run: <code>mvn -q -Dtest=SqlWrapperTest,DatabaseMappingTest test</code>

Expected: all SQL and mapping tests pass, including the apostrophe case.

Commit: <code>git commit -m "重构：使用参数化SQL并修复日期映射"</code>.

### Task 4: Decode PCM Correctly and Publish FFT Snapshots

**Files:**
- Create: <code>src/main/java/com/xu/music/player/player/PcmSpectrumAnalyzer.java</code>
- Create: <code>src/test/java/com/xu/music/player/player/PcmSpectrumAnalyzerTest.java</code>
- Modify: <code>src/main/java/com/xu/music/player/player/SdlFftPlayer.java</code>

- [ ] **Step 1: Write failing PCM tests**

~~~java
@Test public void decodesStereo16BitLittleEndianToMono() {
    var analyzer = new PcmSpectrumAnalyzer(512);
    assertEquals(0.5, analyzer.decodeFrame(
            new byte[]{0x00, 0x40, 0x00, 0x40}, 0, 2, 16), 0.0001);
}

@Test public void snapshotIsDefensive() {
    var analyzer = new PcmSpectrumAnalyzer(8);
    var first = analyzer.spectrumSnapshot();
    first[0] = 99;
    assertNotEquals(99, analyzer.spectrumSnapshot()[0], 0.0);
}
~~~

- [ ] **Step 2: Verify red**

Run: <code>mvn -q -Dtest=PcmSpectrumAnalyzerTest test</code>

Expected: compilation failure because analyzer does not exist.

- [ ] **Step 3: Implement frame-aware analysis**

<code>PcmSpectrumAnalyzer.accept(byte[], int, int, AudioFormat)</code> iterates by <code>format.getFrameSize()</code>, averages channels, stores normalized samples in a private ring, applies FFT only after the ring fills, and publishes a cloned <code>double[]</code>. Remove the old static <code>TRANS</code> deque and the <code>sampleRate == 16</code> branch.

- [ ] **Step 4: Verify and commit**

Run: <code>mvn -q -Dtest=PcmSpectrumAnalyzerTest test</code>

Expected: PCM tests pass.

Commit: <code>git commit -m "修复：按PCM帧生成频谱快照"</code>.

### Task 5: Make Playback Sessions Race-Free

**Files:**
- Modify: <code>src/main/java/com/xu/music/player/player/Player.java</code>
- Create: <code>src/main/java/com/xu/music/player/player/PlaybackSession.java</code>
- Create: <code>src/main/java/com/xu/music/player/player/PlaybackSessionSlot.java</code>
- Rewrite: <code>src/main/java/com/xu/music/player/player/SdlFftPlayer.java</code>
- Create: <code>src/test/java/com/xu/music/player/player/PlaybackSessionTest.java</code>

- [ ] **Step 1: Write failing lifecycle tests with a fake audio output**

~~~java
@Test public void oldCompletionCannotDetachReplacementSession() {
    var slot = new PlaybackSessionSlot();
    var oldSession = sessionWithLine(0, new AtomicInteger(), new AtomicBoolean());
    var replacement = sessionWithLine(0, new AtomicInteger(), new AtomicBoolean());
    slot.replace(oldSession);
    slot.replace(replacement);
    assertFalse(slot.complete(oldSession));
    assertSame(replacement, slot.current());
}

@Test public void positionUsesFrameRateAndCloseIsIdempotent() {
    var closeCount = new AtomicInteger();
    var session = sessionWithLine(44_100, closeCount, new AtomicBoolean());
    assertEquals(1.0, session.positionSeconds(), 0.001);
    session.close();
    session.close();
    assertEquals(1, closeCount.get());
}

@Test public void pauseStopsTheAudioLine() {
    var running = new AtomicBoolean(true);
    var session = sessionWithLine(0, new AtomicInteger(), running);
    session.pause();
    assertFalse(running.get());
    assertTrue(session.paused());
}

private static PlaybackSession sessionWithLine(
        long frames, AtomicInteger closeCount, AtomicBoolean running) {
    var format = new AudioFormat(44_100, 16, 2, true, false);
    var line = (SourceDataLine) Proxy.newProxyInstance(
            SourceDataLine.class.getClassLoader(),
            new Class<?>[]{SourceDataLine.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "getLongFramePosition" -> frames;
                case "getFormat" -> format;
                case "start" -> {
                    running.set(true);
                    yield null;
                }
                case "stop" -> {
                    running.set(false);
                    yield null;
                }
                case "close" -> {
                    closeCount.incrementAndGet();
                    yield null;
                }
                default -> primitiveDefault(method.getReturnType());
            });
    var audio = new AudioInputStream(InputStream.nullInputStream(), format, 0);
    return new PlaybackSession(audio, line, format, new PcmSpectrumAnalyzer(512));
}

private static Object primitiveDefault(Class<?> type) {
    if (!type.isPrimitive() || type == void.class) return null;
    if (type == boolean.class) return false;
    if (type == char.class) return '\0';
    if (type == byte.class) return (byte) 0;
    if (type == short.class) return (short) 0;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0F;
    return 0D;
}
~~~

- [ ] **Step 2: Verify red**

Run: <code>mvn -q -Dtest=PlaybackSessionTest test</code>

Expected: lifecycle abstraction missing or assertions fail.

- [ ] **Step 3: Implement Session ownership and virtual threads**

Create one package-private <code>PlaybackSession</code> per load. It owns its stream, line, format, analyzer, state flags and task references. <code>PlaybackSessionSlot</code> wraps an <code>AtomicReference</code>; <code>replace</code> uses <code>getAndSet</code> and <code>complete</code> uses identity-based <code>compareAndSet</code>. Start blocking playback with:

~~~java
session.playbackThread = Thread.ofVirtual()
        .name("music-playback-" + session.id)
        .start(() -> runPlayback(session));
~~~

All loop state and resources come from the captured Session. <code>stop()</code> atomically detaches the current Session before stopping it. Completion only clears <code>current</code> with <code>compareAndSet(session, null)</code>. <code>PlaybackSession.close()</code> is idempotent through <code>AtomicBoolean.compareAndSet</code>. Position returns <code>line.getLongFramePosition() / format.getFrameRate()</code>.

Update <code>Player</code> with stable seconds semantics and defaults needed until obsolete implementations are removed:

~~~java
double position();
double duration();

default double[] spectrumSnapshot() {
    return new double[0];
}

default void close() {
    stop();
}
~~~

- [ ] **Step 4: Verify and commit**

Run: <code>mvn -q -Dtest=PlaybackSessionTest,PcmSpectrumAnalyzerTest test</code>

Expected: lifecycle and PCM tests pass without an audio device.

Commit: <code>git commit -m "重构：使用虚拟线程隔离播放会话"</code>.

### Task 6: Integrate Real Playback State into SWT

**Files:**
- Modify: <code>src/main/java/com/xu/music/player/main/MusicPlayer.java</code>
- Create: <code>src/main/java/com/xu/music/player/main/PlaybackProgress.java</code>
- Create: <code>src/test/java/com/xu/music/player/main/PlaybackProgressTest.java</code>

- [ ] **Step 1: Add failing pure progress tests**

~~~java
@Test public void calculatesBoundedPercentage() {
    assertEquals(50, PlaybackProgress.percentage(5.0, 10.0));
    assertEquals(0, PlaybackProgress.percentage(1.0, 0.0));
    assertEquals(100, PlaybackProgress.percentage(11.0, 10.0));
}
~~~

- [ ] **Step 2: Verify red and implement the helper**

Run: <code>mvn -q -Dtest=PlaybackProgressTest test</code>.

Implement:

~~~java
public final class PlaybackProgress {
    private PlaybackProgress() {}

    public static int percentage(double position, double duration) {
        if (duration <= 0) {
            return 0;
        }
        return (int) Math.clamp(Math.round(position * 100 / duration), 0, 100);
    }
}
~~~

- [ ] **Step 3: Replace synthetic Timer progress**

Use <code>display.timerExec(100, refreshRunnable)</code>. Each refresh reads <code>player.position()</code>, updates lyrics and progress only while playing, and reschedules only while the shell and Session are active. Set current time on the left label and total duration on the right. Read the spectrum through <code>spectrumSnapshot()</code>.

- [ ] **Step 4: Run all tests and commit**

Run: <code>mvn -q test</code>

Expected: all tests execute; no “Tests are skipped” line.

Commit: <code>git commit -m "修复：使用真实播放位置刷新界面"</code>.

### Task 7: Add SWT Platform Profiles and Clean Production Sources

**Files:**
- Modify: <code>pom.xml</code>
- Move: <code>src/main/java/com/xu/music/player/image/*.png</code> to <code>src/main/resources/com/xu/music/player/image/</code>
- Create: <code>src/main/resources/simplelogger.properties</code>
- Delete: <code>src/main/resources/logback-spring.xml</code>
- Delete: <code>src/main/java/com/xu/Test.java</code>
- Delete: <code>src/main/java/com/xu/music/player/test/</code>
- Delete: <code>src/main/java/com/xu/music/player/player/ClipPlayer.java</code>
- Delete: <code>src/main/java/com/xu/music/player/player/SdlPlayer.java</code>

- [ ] **Step 1: Add four profile-specific SWT dependencies**

Define <code>windows-x64</code>, <code>linux-x64</code>, <code>macos-x64</code>, and <code>macos-arm64</code> profiles with the approved SWT artifacts. OS/architecture activation selects the native profile. Remove the unconditional Windows dependency and the Java-source resource directory.

- [ ] **Step 2: Move resources and remove dead code**

Move all PNG files to the standard resources path. Keep SLF4J Simple and configure:

~~~properties
org.slf4j.simpleLogger.defaultLogLevel=info
org.slf4j.simpleLogger.showThreadName=true
org.slf4j.simpleLogger.showShortLogName=true
~~~

Delete the unused players and demo entry points after verifying <code>rg "ClipPlayer|SdlPlayer|com.xu.Test"</code> has no production references.

- [ ] **Step 3: Verify profiles and package contents**

Run:

~~~powershell
mvn -q clean test
mvn -q package
mvn -q dependency:resolve -Pwindows-x64
mvn -q dependency:resolve -Plinux-x64
mvn -q dependency:resolve -Pmacos-x64
mvn -q dependency:resolve -Pmacos-arm64
~~~

Expected: tests/package pass; each profile resolves its SWT artifact. Inspect the shaded JAR for the main class, PNG and current Windows SWT classes.

- [ ] **Step 4: Commit**

Commit: <code>git commit -m "构建：支持多平台SWT并清理工程资源"</code>.

### Task 8: Remove Unused SQLite Tools, Repair Data, and Finish README

**Files:**
- Modify: <code>src/main/java/com/xu/music/player/wrapper/sql/NewHelper.java</code>
- Modify: <code>lib/sqlite/db/MusicPlayer.db</code>
- Delete: <code>lib/sqlite/sqlite-tools-win-x64-3460000/</code>
- Delete: <code>lib/sqlite/sqlite-tools-linux-x64-3460000/</code>
- Delete: <code>lib/sqlite/sqlite-tools-osx-x64-3460000/</code>
- Rewrite: <code>README.md</code>

- [ ] **Step 1: Add a database path regression test**

Add a test that opens <code>MusicPlayer.db</code>, asserts four songs load through <code>QueryWrapper</code>, and asserts each nonblank lyric path exists relative to the project root.

- [ ] **Step 2: Verify the current lyric path test fails**

Run: <code>mvn -q -Dtest=DatabaseMappingTest test</code>

Expected: failure for <code>song/张敬轩 - 酷爱.lrc</code>.

- [ ] **Step 3: Repair the SQLite row and remove CLI coupling**

Before deleting the CLI, run a parameterized SQLite update changing the path to <code>lib/song/张敬轩 - 酷爱.lrc</code>. Remove the CLI constants and <code>java.library.path</code> mutation from <code>NewHelper</code>, then delete the three exact tool directories. Keep only <code>lib/sqlite/db</code>.

- [ ] **Step 4: Rewrite README as a verified learning guide**

Cover prerequisites, platform profiles, commands, directory map, playback flow, virtual-thread Session lifecycle, PCM/FFT, LRC, prepared SQL, database schema, tests, packaging, limitations, and validation boundaries. Remove the obsolete statement that tests are skipped.

- [ ] **Step 5: Run final verification**

Run:

~~~powershell
mvn clean test
mvn package
git diff --check
git status --short
~~~

Expected: all tests pass by default, package succeeds, no whitespace errors, and only intentional task files are changed.

Perform a bounded Windows UI smoke test from the project root. Verify startup, first-song previous wrap, pause/resume, MP3/FLAC playback, lyric clearing, rapid switching, tray restore and exit. Record any hardware-dependent limitation honestly.

- [ ] **Step 6: Commit**

Commit all verified implementation and documentation with:

~~~powershell
git add README.md lib src pom.xml
git commit -m "优化：完成播放器可靠性与跨平台改造"
~~~
