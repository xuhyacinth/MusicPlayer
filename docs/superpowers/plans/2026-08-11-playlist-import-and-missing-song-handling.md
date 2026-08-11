# Playlist Import and Missing Song Handling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an always-visible song import control, make double-click the only direct-play gesture, skip missing files during relative and automatic navigation, and offer database-only deletion for a missing song selected by the user.

**Architecture:** Keep `MusicPlayer` as the SWT composition root, but move bounded index scanning, path validation, playlist rebuilding, playback-completion filtering, and request-generation checks into small package-local units. `SdlFftPlayer` reports only natural EOF from the current Session; the UI marshals that signal back to the SWT thread before advancing.

**Tech Stack:** Java 25, SWT/JFace, Java Sound, virtual threads, SQLite JDBC, existing SQL wrappers, JUnit 4, Maven Surefire.

---

## File Map

- Modify `src/main/java/com/xu/music/player/main/PlaylistNavigator.java`: bounded forward/backward playable-index scanning.
- Modify `src/test/java/com/xu/music/player/main/PlaylistNavigatorTest.java`: scan direction, wrap, empty and one-pass coverage.
- Create `src/main/java/com/xu/music/player/main/SongFileAvailability.java`: treat regular files as playable and malformed paths as missing.
- Create `src/test/java/com/xu/music/player/main/SongFileAvailabilityTest.java`: real temporary-file and invalid-path coverage.
- Create `src/main/java/com/xu/music/player/player/PlaybackCompletionNotifier.java`: thread-safe natural-completion listener storage and filtering.
- Create `src/test/java/com/xu/music/player/player/PlaybackCompletionNotifierTest.java`: natural/current versus stopped/replaced outcomes.
- Modify `src/main/java/com/xu/music/player/player/Player.java`: natural-completion callback contract without SWT types.
- Modify `src/main/java/com/xu/music/player/player/SdlFftPlayer.java`: detect EOF and notify only after identity-safe Session completion.
- Create `src/main/java/com/xu/music/player/main/PlaybackRequestGate.java`: reject queued EOF work after a newer explicit playback request.
- Create `src/test/java/com/xu/music/player/main/PlaybackRequestGateTest.java`: current, stale and already-playing decisions.
- Modify `src/main/java/com/xu/music/player/wrapper/UpdateWrapper.java`: allow test-only helper injection while preserving the existing constructor.
- Modify `src/test/java/com/xu/music/player/wrapper/DatabaseMappingTest.java`: prove record deletion leaves the audio file untouched.
- Create `src/main/java/com/xu/music/player/main/PlaylistSnapshot.java`: rebuild contiguous indexes and restore the current song by ID.
- Create `src/test/java/com/xu/music/player/main/PlaylistSnapshotTest.java`: reindex and missing-current behavior.
- Modify `src/main/java/com/xu/music/player/main/MusicPlayer.java`: toolbar, import refresh, double-click, deletion confirmation, bounded navigation and EOF wiring.
- Modify `README.md`: document the new interaction, concurrency boundary and test coverage.

### Task 1: Make Missing-File Navigation Pure and Bounded

**Files:**
- Modify: `src/main/java/com/xu/music/player/main/PlaylistNavigator.java`
- Modify: `src/test/java/com/xu/music/player/main/PlaylistNavigatorTest.java`
- Create: `src/main/java/com/xu/music/player/main/SongFileAvailability.java`
- Create: `src/test/java/com/xu/music/player/main/SongFileAvailabilityTest.java`

- [ ] **Step 1: Write the failing navigation and file-availability tests**

Replace `PlaylistNavigatorTest` with:

```java
package com.xu.music.player.main;

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PlaylistNavigatorTest {

    @Test
    public void previousWrapsFromFirstToLast() {
        assertEquals(3, PlaylistNavigator.move(0, 4, -1));
    }

    @Test
    public void nextWrapsFromLastToFirst() {
        assertEquals(0, PlaylistNavigator.move(3, 4, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPlaylistIsRejectedByMove() {
        PlaylistNavigator.move(0, 0, 1);
    }

    @Test
    public void forwardScanSkipsUnavailableEntries() {
        var result = PlaylistNavigator.findPlayable(0, 4, 1, Set.of(3)::contains);
        assertEquals(3, result.orElseThrow());
    }

    @Test
    public void backwardScanSkipsUnavailableEntries() {
        var result = PlaylistNavigator.findPlayable(0, 4, -1, Set.of(2)::contains);
        assertEquals(2, result.orElseThrow());
    }

    @Test
    public void scanChecksAtMostOnePlaylistCycle() {
        var checks = new AtomicInteger();
        var result = PlaylistNavigator.findPlayable(1, 4, 1, ignored -> {
            checks.incrementAndGet();
            return false;
        });

        assertFalse(result.isPresent());
        assertEquals(4, checks.get());
    }

    @Test
    public void forwardScanWithoutCurrentStartsAtFirst() {
        assertEquals(0, PlaylistNavigator.findPlayable(null, 3, 1, ignored -> true).orElseThrow());
    }

    @Test
    public void backwardScanWithoutCurrentStartsAtLast() {
        assertEquals(2, PlaylistNavigator.findPlayable(null, 3, -1, ignored -> true).orElseThrow());
    }
}
```

Create `SongFileAvailabilityTest`:

```java
package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SongFileAvailabilityTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void regularFileIsPlayable() throws Exception {
        var song = song(temporaryFolder.newFile("track.wav").getAbsolutePath());
        assertTrue(SongFileAvailability.isPlayable(song));
    }

    @Test
    public void missingBlankInvalidAndNullPathsAreUnavailable() {
        assertFalse(SongFileAvailability.isPlayable(song("missing.wav")));
        assertFalse(SongFileAvailability.isPlayable(song(" ")));
        assertFalse(SongFileAvailability.isPlayable(song("\0")));
        assertFalse(SongFileAvailability.isPlayable(null));
    }

    private static SongEntity song(String path) {
        var song = new SongEntity();
        song.setSongPath(path);
        return song;
    }
}
```

- [ ] **Step 2: Run the focused tests and verify red**

Run:

```powershell
mvn -q -Dtest=PlaylistNavigatorTest,SongFileAvailabilityTest test
```

Expected: test compilation fails because `findPlayable` and `SongFileAvailability` do not exist.

- [ ] **Step 3: Implement the bounded scan and path check**

Add this method to `PlaylistNavigator`:

```java
public static OptionalInt findPlayable(
        Integer current, int size, int delta, IntPredicate playable) {
    if (size <= 0) {
        return OptionalInt.empty();
    }
    if (delta != -1 && delta != 1) {
        throw new IllegalArgumentException("播放方向必须是 -1 或 1");
    }
    Objects.requireNonNull(playable, "playable");

    var candidate = current == null
            ? (delta > 0 ? 0 : size - 1)
            : move(current, size, delta);
    for (var checked = 0; checked < size; checked++) {
        if (playable.test(candidate)) {
            return OptionalInt.of(candidate);
        }
        candidate = move(candidate, size, delta);
    }
    return OptionalInt.empty();
}
```

Add imports for `Objects`, `OptionalInt` and `IntPredicate`. Create `SongFileAvailability`:

```java
package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;

import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;

final class SongFileAvailability {

    private SongFileAvailability() {
    }

    static boolean isPlayable(SongEntity song) {
        if (song == null || song.getSongPath() == null || song.getSongPath().isBlank()) {
            return false;
        }
        try {
            return Files.isRegularFile(Path.of(song.getSongPath()));
        } catch (InvalidPathException exception) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Run the focused tests and verify green**

Run:

```powershell
mvn -q -Dtest=PlaylistNavigatorTest,SongFileAvailabilityTest test
```

Expected: 10 tests pass.

- [ ] **Step 5: Commit the navigation unit**

```powershell
git add src/main/java/com/xu/music/player/main/PlaylistNavigator.java src/main/java/com/xu/music/player/main/SongFileAvailability.java src/test/java/com/xu/music/player/main/PlaylistNavigatorTest.java src/test/java/com/xu/music/player/main/SongFileAvailabilityTest.java
git commit -m "功能：支持跳过缺失歌曲"
```

### Task 2: Report Natural EOF Without Leaking SWT Into the Player

**Files:**
- Create: `src/main/java/com/xu/music/player/player/PlaybackCompletionNotifier.java`
- Create: `src/test/java/com/xu/music/player/player/PlaybackCompletionNotifierTest.java`
- Modify: `src/main/java/com/xu/music/player/player/Player.java`
- Modify: `src/main/java/com/xu/music/player/player/SdlFftPlayer.java`

- [ ] **Step 1: Write the failing completion-filter tests**

Create `PlaybackCompletionNotifierTest`:

```java
package com.xu.music.player.player;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class PlaybackCompletionNotifierTest {

    @Test
    public void notifiesNaturalCompletionOfCurrentSession() {
        var calls = new AtomicInteger();
        var notifier = new PlaybackCompletionNotifier();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(true, true);

        assertEquals(1, calls.get());
    }

    @Test
    public void ignoresNonEofTermination() {
        var calls = new AtomicInteger();
        var notifier = new PlaybackCompletionNotifier();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(false, true);

        assertEquals(0, calls.get());
    }

    @Test
    public void ignoresCompletionOfReplacedSession() {
        var calls = new AtomicInteger();
        var notifier = new PlaybackCompletionNotifier();
        notifier.setListener(calls::incrementAndGet);

        notifier.notifyIfNatural(true, false);

        assertEquals(0, calls.get());
    }
}
```

- [ ] **Step 2: Run the test and verify red**

Run:

```powershell
mvn -q -Dtest=PlaybackCompletionNotifierTest test
```

Expected: test compilation fails because `PlaybackCompletionNotifier` does not exist.

- [ ] **Step 3: Implement the notifier and Player callback contract**

Create `PlaybackCompletionNotifier`:

```java
package com.xu.music.player.player;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class PlaybackCompletionNotifier {

    private static final Runnable NO_OP = () -> { };
    private final AtomicReference<Runnable> listener = new AtomicReference<>(NO_OP);

    void setListener(Runnable replacement) {
        listener.set(Objects.requireNonNullElse(replacement, NO_OP));
    }

    void notifyIfNatural(boolean reachedEof, boolean completedCurrentSession) {
        if (reachedEof && completedCurrentSession) {
            listener.get().run();
        }
    }
}
```

Add this default method to `Player` before `spectrumSnapshot()`:

```java
/**
 * 注册当前播放会话自然到达 EOF 时的回调。
 */
default void onNaturalCompletion(Runnable listener) {
}
```

Add a notifier field and callback implementation to `SdlFftPlayer`:

```java
private final PlaybackCompletionNotifier completionNotifier = new PlaybackCompletionNotifier();

@Override
public void onNaturalCompletion(Runnable listener) {
    completionNotifier.setListener(listener);
}
```

Replace `runPlayback` with:

```java
private void runPlayback(PlaybackSession session) {
    var reachedEof = false;
    try {
        var format = session.format();
        var frameSize = format.getFrameSize();
        var bufferSize = Math.max(frameSize, 4096 / frameSize * frameSize);
        var buffer = new byte[bufferSize];

        while (session.playing() && !Thread.currentThread().isInterrupted()) {
            session.awaitIfPaused();
            if (!session.playing()) {
                break;
            }

            var read = session.audio().read(buffer);
            if (read == -1) {
                reachedEof = true;
                session.line().drain();
                break;
            }
            var alignedLength = read - read % frameSize;
            if (alignedLength == 0) {
                continue;
            }
            session.analyzer().accept(buffer, 0, alignedLength, format);
            session.line().write(buffer, 0, alignedLength);
        }
    } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
    } catch (Exception exception) {
        reachedEof = false;
        if (session.playing()) {
            log.error("音频播放任务异常", exception);
        }
    } finally {
        session.markStopped();
        var completedCurrentSession = sessions.complete(session);
        session.close();
        try {
            completionNotifier.notifyIfNatural(reachedEof, completedCurrentSession);
        } catch (RuntimeException exception) {
            log.error("播放完成回调异常", exception);
        }
    }
}
```

- [ ] **Step 4: Run completion and Session tests**

Run:

```powershell
mvn -q -Dtest=PlaybackCompletionNotifierTest,PlaybackSessionTest test
```

Expected: 6 tests pass. The existing replacement-Session test must remain green.

- [ ] **Step 5: Commit the natural-completion boundary**

```powershell
git add src/main/java/com/xu/music/player/player/Player.java src/main/java/com/xu/music/player/player/SdlFftPlayer.java src/main/java/com/xu/music/player/player/PlaybackCompletionNotifier.java src/test/java/com/xu/music/player/player/PlaybackCompletionNotifierTest.java
git commit -m "功能：通知歌曲自然播放结束"
```

### Task 3: Reject Stale Queued Completion Work

**Files:**
- Create: `src/main/java/com/xu/music/player/main/PlaybackRequestGate.java`
- Create: `src/test/java/com/xu/music/player/main/PlaybackRequestGateTest.java`

- [ ] **Step 1: Write the failing request-generation tests**

Create `PlaybackRequestGateTest`:

```java
package com.xu.music.player.main;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackRequestGateTest {

    @Test
    public void acceptsCurrentCompletionWhenNothingIsPlaying() {
        var gate = new PlaybackRequestGate();
        assertTrue(gate.accepts(gate.snapshot(), false));
    }

    @Test
    public void newerExplicitRequestInvalidatesQueuedCompletion() {
        var gate = new PlaybackRequestGate();
        var queuedGeneration = gate.snapshot();
        gate.beginRequest();
        assertFalse(gate.accepts(queuedGeneration, false));
    }

    @Test
    public void runningReplacementPlaybackRejectsCompletion() {
        var gate = new PlaybackRequestGate();
        assertFalse(gate.accepts(gate.snapshot(), true));
    }
}
```

- [ ] **Step 2: Run the test and verify red**

Run:

```powershell
mvn -q -Dtest=PlaybackRequestGateTest test
```

Expected: test compilation fails because `PlaybackRequestGate` does not exist.

- [ ] **Step 3: Implement the atomic request gate**

Create `PlaybackRequestGate`:

```java
package com.xu.music.player.main;

import java.util.concurrent.atomic.AtomicLong;

final class PlaybackRequestGate {

    private final AtomicLong generation = new AtomicLong();

    long beginRequest() {
        return generation.incrementAndGet();
    }

    long snapshot() {
        return generation.get();
    }

    boolean accepts(long completedGeneration, boolean playerIsPlaying) {
        return !playerIsPlaying && generation.get() == completedGeneration;
    }
}
```

- [ ] **Step 4: Run the test and verify green**

Run:

```powershell
mvn -q -Dtest=PlaybackRequestGateTest test
```

Expected: 3 tests pass.

- [ ] **Step 5: Commit the concurrency guard**

```powershell
git add src/main/java/com/xu/music/player/main/PlaybackRequestGate.java src/test/java/com/xu/music/player/main/PlaybackRequestGateTest.java
git commit -m "功能：防止过期播放完成回调"
```

### Task 4: Prove Database-Only Deletion

**Files:**
- Modify: `src/main/java/com/xu/music/player/wrapper/UpdateWrapper.java`
- Modify: `src/test/java/com/xu/music/player/wrapper/DatabaseMappingTest.java`

- [ ] **Step 1: Write the failing injected-database deletion test**

Add this test to `DatabaseMappingTest`:

```java
@Test
public void deletingSongRecordDoesNotDeleteAudioFile() throws Exception {
    var database = Files.createTempFile("music-player-delete-", ".db");
    var audio = Files.createTempFile("music-player-audio-", ".wav");
    try {
        var helper = new NewHelper(database);
        helper.update("create table song (id text primary key, song_path text)");
        helper.update("insert into song(id, song_path) values(?, ?)", "missing", audio.toString());
        helper.update("insert into song(id, song_path) values(?, ?)", "keep", "keep.wav");

        var song = new SongEntity();
        song.setId("missing");
        var deleted = new UpdateWrapper<>(song, "song", helper)
                .eq("id", song.getId())
                .delete();

        assertEquals(1, deleted);
        assertEquals(1, helper.select("select id from song where id = ?", "keep").size());
        assertTrue(Files.isRegularFile(audio));
    } finally {
        Files.deleteIfExists(database);
        Files.deleteIfExists(audio);
    }
}
```

- [ ] **Step 2: Run the test and verify red**

Run:

```powershell
mvn -q -Dtest=DatabaseMappingTest test
```

Expected: test compilation fails because `UpdateWrapper(T, String, Helper)` does not exist.

- [ ] **Step 3: Inject the SQL helper without changing existing callers**

Add a field and delegating constructors to `UpdateWrapper`:

```java
private final T data;
private final Helper helper;

public UpdateWrapper(T data, String table) {
    this(data, table, new NewHelper());
}

public UpdateWrapper(T data, String table, Helper helper) {
    if (data == null || StrUtil.isBlank(table) || helper == null) {
        throw new DataBaseError("参数错误");
    }
    this.data = data;
    this.table = requireIdentifier(table);
    this.helper = helper;
}
```

Replace `execute` with:

```java
private int execute(SqlCommand command) {
    return helper.update(command.sql(), command.parameterArray());
}
```

- [ ] **Step 4: Run database and SQL wrapper tests**

Run:

```powershell
mvn -q -Dtest=DatabaseMappingTest,SqlWrapperTest test
```

Expected: all tests in both classes pass and the temporary audio file survives record deletion.

- [ ] **Step 5: Commit the deletion boundary**

```powershell
git add src/main/java/com/xu/music/player/wrapper/UpdateWrapper.java src/test/java/com/xu/music/player/wrapper/DatabaseMappingTest.java
git commit -m "测试：验证歌曲记录删除边界"
```

### Task 5: Rebuild the Playlist by Song ID and Add the Toolbar

**Files:**
- Create: `src/main/java/com/xu/music/player/main/PlaylistSnapshot.java`
- Create: `src/test/java/com/xu/music/player/main/PlaylistSnapshotTest.java`
- Modify: `src/main/java/com/xu/music/player/main/MusicPlayer.java`

- [ ] **Step 1: Write the failing playlist-rebuild tests**

Create `PlaylistSnapshotTest`:

```java
package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PlaylistSnapshotTest {

    @Test
    public void restoresCurrentSongByIdAfterEarlierRecordIsRemoved() {
        var snapshot = PlaylistSnapshot.from(List.of(song("b"), song("c")), "c");
        assertEquals(Integer.valueOf(1), snapshot.playingIndex());
        assertEquals("c", snapshot.songs().get(1).getId());
    }

    @Test
    public void missingCurrentSongIsNotRestored() {
        var snapshot = PlaylistSnapshot.from(List.of(song("b")), "removed");
        assertNull(snapshot.playingIndex());
    }

    @Test
    public void nullCurrentIdDoesNotSelectSongWithNullId() {
        var snapshot = PlaylistSnapshot.from(List.of(song(null)), null);
        assertNull(snapshot.playingIndex());
    }

    private static SongEntity song(String id) {
        var song = new SongEntity();
        song.setId(id);
        song.setName(id);
        return song;
    }
}
```

- [ ] **Step 2: Run the snapshot test and verify red**

Run:

```powershell
mvn -q -Dtest=PlaylistSnapshotTest test
```

Expected: test compilation fails because `PlaylistSnapshot` does not exist.

- [ ] **Step 3: Implement immutable indexed playlist rebuilding**

Create `PlaylistSnapshot`:

```java
package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record PlaylistSnapshot(Map<Integer, SongEntity> songs, Integer playingIndex) {

    static PlaylistSnapshot from(List<SongEntity> source, String playingSongId) {
        var indexed = new LinkedHashMap<Integer, SongEntity>();
        Integer restoredIndex = null;
        for (var index = 0; index < source.size(); index++) {
            var song = source.get(index);
            indexed.put(index, song);
            if (playingSongId != null && Objects.equals(playingSongId, song.getId())) {
                restoredIndex = index;
            }
        }
        return new PlaylistSnapshot(
                Collections.unmodifiableMap(indexed), restoredIndex);
    }
}
```

- [ ] **Step 4: Add the icon toolbar above the table**

In `MusicPlayer`, replace the left-list `FillLayout` block with:

```java
Composite composite1 = new Composite(sashForm1, SWT.NONE);
composite1.setBackgroundMode(SWT.INHERIT_FORCE);
composite1.setLayout(new GridLayout(1, false));

var playlistTools = new ToolBar(composite1, SWT.FLAT);
playlistTools.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
var addSong = new ToolItem(playlistTools, SWT.PUSH);
addSong.setImage(Utils.getImage("addMusic.png"));
addSong.setToolTipText("添加歌曲");
addSong.addListener(SWT.Selection, event -> addSongs());

lists = new Table(composite1, SWT.FULL_SELECTION);
lists.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
lists.setHeaderVisible(true);
```

Add imports for `GridData`, `GridLayout`, `ToolBar` and `ToolItem`.

- [ ] **Step 5: Replace list loading with refresh-and-restore methods**

Keep the public `initPlayer` entry, and replace its loading/table helpers with:

```java
public void initPlayer(Shell shell, Table table) {
    try {
        var songs = querySongs();
        if (songs.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            new SongChoose().open(shell);
            songs = querySongs();
        }
        applyPlaylist(songs, table);
    } catch (RuntimeException exception) {
        log.error("初始化播放列表异常", exception);
        showError("无法读取或更新播放列表，请检查数据库和文件权限。");
    }
}

private void addSongs() {
    try {
        new SongChoose().open(shell);
        reloadPlaylist();
    } catch (RuntimeException exception) {
        log.error("添加歌曲异常", exception);
        showError("添加歌曲失败，请检查文件和数据库权限。");
    }
}

private void reloadPlaylist() {
    applyPlaylist(querySongs(), lists);
}

private List<SongEntity> querySongs() {
    return new QueryWrapper<>(SongEntity.class, "song").list();
}

private void applyPlaylist(List<SongEntity> songs, Table table) {
    var previousSong = Constant.PLAYING_SONG;
    var previousId = previousSong == null ? null : previousSong.getId();
    var snapshot = PlaylistSnapshot.from(songs, previousId);

    table.removeAll();
    Constant.PLAYING_LIST.clear();
    Constant.PLAYING_LIST.putAll(snapshot.songs());
    snapshot.songs().forEach((index, song) -> {
        var item = new TableItem(table, SWT.NONE);
        item.setText(new String[]{String.valueOf(index), song.getName()});
    });

    Constant.PLAYING_INDEX = snapshot.playingIndex();
    if (snapshot.playingIndex() == null) {
        if (previousSong != null) {
            player.stop();
            clearCurrentSong();
            resetPlaybackUi();
        }
        return;
    }

    Constant.PLAYING_SONG = snapshot.songs().get(snapshot.playingIndex());
    Constant.PLAYING_SONG_LENGTH = Constant.PLAYING_SONG.getLength();
    updateSongSelection(table);
}

private void clearCurrentSong() {
    Constant.PLAYING_SONG = null;
    Constant.PLAYING_INDEX = null;
    Constant.PLAYING_SONG_LENGTH = 0;
}
```

Extract table color and scrolling from `updateSongListsColor` into:

```java
private void updateSongSelection(Table table) {
    var activeIndex = Constant.PLAYING_INDEX;
    var items = table.getItems();
    for (var index = 0; index < items.length; index++) {
        items[index].setBackground(Utils.getColor(
                activeIndex != null && index == activeIndex
                        ? SWT.COLOR_GRAY : SWT.COLOR_WHITE));
    }
    if (activeIndex != null) {
        table.setTopIndex(Math.max(0, activeIndex - 7));
    }
}
```

Keep playback-time initialization in `updateSongListsColor`, but replace its color/scroll block with `updateSongSelection(table)`.

- [ ] **Step 6: Run focused and full tests**

Run:

```powershell
mvn -q -Dtest=PlaylistSnapshotTest test
mvn -q test
```

Expected: 3 snapshot tests pass, then the complete suite passes.

- [ ] **Step 7: Commit the list UI and refresh behavior**

```powershell
git add src/main/java/com/xu/music/player/main/MusicPlayer.java src/main/java/com/xu/music/player/main/PlaylistSnapshot.java src/test/java/com/xu/music/player/main/PlaylistSnapshotTest.java
git commit -m "功能：增加歌曲列表添加入口"
```

### Task 6: Wire Double-Click, Deletion, Relative Navigation and EOF

**Files:**
- Modify: `src/main/java/com/xu/music/player/main/MusicPlayer.java`

- [ ] **Step 1: Add the request gate and register the natural-completion callback**

Add the field:

```java
private final PlaybackRequestGate playbackRequests = new PlaybackRequestGate();
```

Immediately after creating `player`, register:

```java
player.onNaturalCompletion(() -> {
    var completedGeneration = playbackRequests.snapshot();
    var currentDisplay = display;
    if (currentDisplay == null || currentDisplay.isDisposed()) {
        return;
    }
    currentDisplay.asyncExec(() -> {
        if (shell == null || shell.isDisposed()
                || !playbackRequests.accepts(completedGeneration, player.playing())) {
            return;
        }
        playRelative(1, true);
    });
});
```

- [ ] **Step 2: Make double-click the only table gesture that starts playback**

Delete the `chose` field, the selection listener and the current mouse listener. Add:

```java
lists.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseDoubleClick(MouseEvent event) {
        var selectedIndex = lists.getSelectionIndex();
        if (selectedIndex >= 0) {
            playSelected(selectedIndex);
        }
    }
});
```

Change previous and next button `mouseUp` handlers to:

```java
playRelative(-1, false);
```

and:

```java
playRelative(1, false);
```

- [ ] **Step 3: Replace the mixed `next` method with explicit playback methods**

Delete `next(String index, boolean next)` and add:

```java
private void playSelected(int index) {
    playbackRequests.beginRequest();
    var song = Constant.PLAYING_LIST.get(index);
    if (!SongFileAvailability.isPlayable(song)) {
        confirmDeleteMissingSong(song);
        return;
    }
    playSong(index, song);
}

private void playRelative(int direction, boolean playbackAlreadyEnded) {
    playbackRequests.beginRequest();
    var result = PlaylistNavigator.findPlayable(
            Constant.PLAYING_INDEX,
            Constant.PLAYING_LIST.size(),
            direction,
            index -> SongFileAvailability.isPlayable(Constant.PLAYING_LIST.get(index)));
    if (result.isEmpty()) {
        if (playbackAlreadyEnded || !player.playing()) {
            resetPlaybackUi();
        }
        showInfo("没有可播放的歌曲。");
        return;
    }

    var index = result.getAsInt();
    playSong(index, Constant.PLAYING_LIST.get(index));
}

private void playSong(int index, SongEntity song) {
    Constant.PLAYING_INDEX = index;
    Constant.PLAYING_SONG = song;
    Constant.PLAYING_SONG_LENGTH = song.getLength();
    try {
        PlaybackStarter.start(player, song.getSongPath());
    } catch (MusicPlayerError exception) {
        log.error("选择歌曲播放异常", exception);
        resetPlaybackUi();
        showError("无法播放所选歌曲，请检查文件格式和音频设备。");
        return;
    }

    try {
        Constant.MUSIC_PLAYER_PLAYING_STATE = true;
        initLyric();
        startRefresh(foot, timeLabel1);
        updateSongListsColor(lists, song);
    } catch (RuntimeException exception) {
        player.stop();
        log.error("播放界面更新异常", exception);
        resetPlaybackUi();
        showError("歌曲已停止，播放器界面更新失败。");
    }
}
```

- [ ] **Step 4: Implement database-only missing-song confirmation**

Add the `UpdateWrapper` import and these methods:

```java
private void confirmDeleteMissingSong(SongEntity song) {
    if (song == null) {
        showInfo("歌曲记录不存在。");
        return;
    }
    var confirmation = Utils.tips(
            shell, "MusicPlayer", "歌曲文件不存在，是否从播放列表删除？");
    if (confirmation.open() != SWT.YES) {
        return;
    }

    try {
        new UpdateWrapper<>(song, "song")
                .eq("id", song.getId())
                .delete();
        reloadPlaylist();
    } catch (RuntimeException exception) {
        log.error("删除歌曲记录异常", exception);
        showError("无法删除歌曲记录，请检查数据库权限。");
    }
}

private void showInfo(String message) {
    var box = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
    box.setText("MusicPlayer");
    box.setMessage(message);
    box.open();
}
```

There is deliberately no call to `Files.delete`, `File.delete` or lyric-file deletion in this path.

- [ ] **Step 5: Run all automated tests and package the Windows application**

Run:

```powershell
mvn clean test
mvn clean package
```

Expected: 14 test classes and 42 tests pass; `target/MusicPlayer-2.0.0.0.jar` is created.

- [ ] **Step 6: Commit the SWT wiring**

```powershell
git add src/main/java/com/xu/music/player/main/MusicPlayer.java
git commit -m "功能：接入缺失歌曲处理与自动下一首"
```

### Task 7: Document and Smoke-Test the User Workflow

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update README behavior and learning notes**

Add these points to “功能概览”:

```markdown
- 通过歌曲列表上方的添加按钮随时导入本地歌曲；
- 单击只选择歌曲，双击播放；双击缺失文件时可只删除播放列表记录；
- 上一曲、下一曲和自然播放结束会按方向跳过缺失文件；
```

In the virtual-thread section, explain that only the current Session's natural EOF emits a completion callback, and that the callback returns to SWT through `Display.asyncExec`. Change the test count to “14 个测试类、42 个测试用例”, add bounded missing-file scan, EOF filtering and database-only deletion to the coverage list, and remove “播放完成后自动下一曲” from “可继续练习”.

- [ ] **Step 2: Run formatting, test and package checks**

Run:

```powershell
git diff --check
mvn clean test
mvn clean package
```

Expected: no whitespace errors, 42 tests pass, and the shaded JAR is rebuilt.

- [ ] **Step 3: Perform the Windows SWT smoke test**

Run the application:

```powershell
java --enable-native-access=ALL-UNNAMED -jar target/MusicPlayer-2.0.0.0.jar
```

Verify all eight cases, then close through the application UI:

1. The add icon appears above the song table and opens the multi-file chooser.
2. Cancelling import does not change the list.
3. Single-click does not play; double-click on an existing song plays.
4. Double-click on a missing song shows Yes/No deletion confirmation.
5. Choosing Yes removes only the SQLite record; the audio and lyric paths are not deleted.
6. Previous and next skip missing entries in the correct direction.
7. Natural EOF advances to the next available song.
8. An all-missing list shows one message and does not loop or freeze the UI.

- [ ] **Step 4: Commit the functional documentation**

```powershell
git add README.md
git commit -m "文档：补充歌曲列表交互说明"
```

- [ ] **Step 5: Record the functional-plan checkpoint**

Run:

```powershell
git status --short --branch
git log -7 --oneline
```

Expected: the worktree is clean and the seven functional commits are visible before dependency upgrades begin.
