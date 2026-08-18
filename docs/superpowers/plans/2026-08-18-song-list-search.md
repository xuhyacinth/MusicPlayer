# Song List Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a search box above the song list that filters visible songs by song name or author.

**Architecture:** Keep search matching in a small pure helper so it can be unit tested without SWT. `MusicPlayer` owns the full database-backed playlist, applies the current search text to produce the visible playlist, and continues using `applyPlaylist(...)` to keep playback indices aligned with the table.

**Tech Stack:** Java 25, SWT, JUnit 4, Maven.

---

## File Structure

- Create `src/main/java/com/xu/music/player/main/SongSearch.java`: package-private pure filtering helper for song name and author matching.
- Create `src/test/java/com/xu/music/player/main/SongSearchTest.java`: unit tests for empty search, song-name match, author match, case-insensitive match, and null fields.
- Modify `src/main/java/com/xu/music/player/main/MusicPlayer.java`: add search input, keep the full playlist in memory, apply the current search when loading/reloading.

## Task 1: Add Search Filtering Helper

**Files:**
- Create: `src/main/java/com/xu/music/player/main/SongSearch.java`
- Create: `src/test/java/com/xu/music/player/main/SongSearchTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/xu/music/player/main/SongSearchTest.java`:

```java
package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SongSearchTest {

    @Test
    public void blankKeywordReturnsAllSongs() {
        SongEntity first = song("Beyond - 长城", "Beyond");
        SongEntity second = song("酷爱", "张敬轩");

        List<SongEntity> result = SongSearch.filter(List.of(first, second), "   ");

        assertEquals(List.of(first, second), result);
    }

    @Test
    public void keywordMatchesSongName() {
        SongEntity match = song("酷爱", "张敬轩");
        SongEntity other = song("加减乘除", "梦涵");

        List<SongEntity> result = SongSearch.filter(List.of(match, other), "酷");

        assertEquals(List.of(match), result);
    }

    @Test
    public void keywordMatchesAuthor() {
        SongEntity match = song("长城", "Beyond");
        SongEntity other = song("酷爱", "张敬轩");

        List<SongEntity> result = SongSearch.filter(List.of(match, other), "beyond");

        assertEquals(List.of(match), result);
    }

    @Test
    public void keywordMatchingIgnoresCase() {
        SongEntity match = song("Dream Song", "Singer");
        SongEntity other = song("Night Track", "Artist");

        List<SongEntity> result = SongSearch.filter(List.of(match, other), "dream");

        assertEquals(List.of(match), result);
    }

    @Test
    public void nullFieldsDoNotFailMatching() {
        SongEntity empty = song(null, null);
        SongEntity match = song(null, "张敬轩");

        List<SongEntity> result = SongSearch.filter(List.of(empty, match), "敬轩");

        assertEquals(List.of(match), result);
    }

    private SongEntity song(String name, String author) {
        SongEntity song = new SongEntity();
        song.setName(name);
        song.setAuthor(author);
        return song;
    }
}
```

- [ ] **Step 2: Run the new tests to verify they fail**

Run:

```powershell
mvn -Dtest=SongSearchTest test
```

Expected: compilation fails because `SongSearch` does not exist.

- [ ] **Step 3: Implement the helper**

Create `src/main/java/com/xu/music/player/main/SongSearch.java`:

```java
package com.xu.music.player.main;

import com.xu.music.player.entity.SongEntity;

import java.util.List;
import java.util.Locale;

final class SongSearch {

    private SongSearch() {
    }

    static List<SongEntity> filter(List<SongEntity> songs, String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.isBlank()) {
            return songs;
        }

        return songs.stream()
                .filter(song -> contains(song.getName(), normalizedKeyword)
                        || contains(song.getAuthor(), normalizedKeyword))
                .toList();
    }

    private static boolean contains(String value, String keyword) {
        return value != null && normalize(value).contains(keyword);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
```

- [ ] **Step 4: Run the helper tests**

Run:

```powershell
mvn -Dtest=SongSearchTest test
```

Expected: all `SongSearchTest` tests pass.

- [ ] **Step 5: Commit the helper**

Run:

```powershell
git add src\main\java\com\xu\music\player\main\SongSearch.java src\test\java\com\xu\music\player\main\SongSearchTest.java
git commit -m "功能：增加歌曲搜索过滤逻辑"
```

## Task 2: Wire Search Into the Song List UI

**Files:**
- Modify: `src/main/java/com/xu/music/player/main/MusicPlayer.java`

- [ ] **Step 1: Add full playlist and search input fields**

In `MusicPlayer.java`, add the imports:

```java
import java.util.ArrayList;
```

Add fields near the existing `lists` field:

```java
    // 数据库中的完整歌曲列表，用于搜索过滤后恢复
    private List<SongEntity> allSongs = new ArrayList<>();
    // 歌曲列表搜索输入框
    private Text songSearch;
```

- [ ] **Step 2: Replace the toolbar layout with add button plus search field**

In `createContents()`, replace the current toolbar block:

```java
        ToolBar toolBar = new ToolBar(composite1, SWT.FLAT);
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolItem addMusic = new ToolItem(toolBar, SWT.PUSH);
        addMusic.setImage(Utils.getImage("addMusic.png"));
        addMusic.setToolTipText("添加歌曲");
        addMusic.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                addSongs();
            }
        });
```

with:

```java
        Composite playlistTools = new Composite(composite1, SWT.NONE);
        playlistTools.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        playlistTools.setLayout(new GridLayout(2, false));

        ToolBar toolBar = new ToolBar(playlistTools, SWT.FLAT);
        toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        ToolItem addMusic = new ToolItem(toolBar, SWT.PUSH);
        addMusic.setImage(Utils.getImage("addMusic.png"));
        addMusic.setToolTipText("添加歌曲");
        addMusic.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                addSongs();
            }
        });

        songSearch = new Text(playlistTools, SWT.SEARCH | SWT.ICON_SEARCH | SWT.CANCEL);
        songSearch.setMessage("搜索歌曲或歌手");
        songSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        songSearch.addModifyListener(event -> applyCurrentSearch());
```

- [ ] **Step 3: Preserve the full playlist and apply the current search**

Replace `initPlayer(...)` with:

```java
    public void initPlayer(Shell shell, Table table) {
        try {
            allSongs = querySongs();

            if (CollUtil.isEmpty(allSongs)) {
                // 当本地没有存放数据时，自动唤起文件选择窗口添加歌曲
                var choice = new SongChoose();
                Toolkit.getDefaultToolkit().beep();
                choice.open(shell);
                allSongs = querySongs();
            }
            applyCurrentSearch(table);
        } catch (RuntimeException exception) {
            log.error("初始化播放列表异常", exception);
            showError("无法读取或更新播放列表，请检查数据库和文件权限。");
            return;
        }
    }
```

Replace `reloadPlaylist()` with:

```java
    private void reloadPlaylist() {
        allSongs = querySongs();
        applyCurrentSearch();
    }
```

Add these helper methods below `reloadPlaylist()`:

```java
    private void applyCurrentSearch() {
        applyCurrentSearch(lists);
    }

    private void applyCurrentSearch(Table table) {
        applyPlaylist(SongSearch.filter(allSongs, currentSearchKeyword()), table);
    }

    private String currentSearchKeyword() {
        if (songSearch == null || songSearch.isDisposed()) {
            return "";
        }
        return songSearch.getText();
    }
```

- [ ] **Step 4: Run the full test suite**

Run:

```powershell
mvn test
```

Expected: all tests pass.

- [ ] **Step 5: Commit the UI wiring**

Run:

```powershell
git add src\main\java\com\xu\music\player\main\MusicPlayer.java
git commit -m "功能：歌曲列表增加搜索框"
```

## Task 3: Manual Smoke Test and Final Verification

**Files:**
- No source changes expected.

- [ ] **Step 1: Run the application**

Run:

```powershell
mvn package
java --enable-native-access=ALL-UNNAMED -jar target\MusicPlayer-2.0.0.0.jar
```

Expected: the MusicPlayer window opens, and the left song-list toolbar shows the add-song icon plus a search input whose empty-state message is `搜索歌曲或歌手`.

- [ ] **Step 2: Verify the search interactions manually**

Use the running app:

```text
1. Type a known song-name fragment, such as 酷.
2. Confirm the list only shows matching songs.
3. Clear the search field.
4. Confirm the full song list returns.
5. Type a known author fragment, such as Beyond.
6. Confirm author matching works even if the visible table still displays only song names.
7. Double-click a filtered result and confirm it plays that visible song.
8. Use previous/next while filtered and confirm navigation stays within visible filtered results.
```

- [ ] **Step 3: Check final repository state**

Run:

```powershell
git status --short
git log --oneline -3
```

Expected: working tree is clean except ignored build artifacts, and recent commits include:

```text
功能：歌曲列表增加搜索框
功能：增加歌曲搜索过滤逻辑
文档：添加歌曲列表搜索框设计
```

## Self-Review

- Spec coverage: search box placement, song-name and author filtering, empty keyword restore, current visible list playback behavior, and test coverage are all mapped to tasks above.
- Marker scan: no incomplete-work markers or undefined later work remains.
- Type consistency: `SongSearch.filter(List<SongEntity>, String)`, `allSongs`, `songSearch`, `applyCurrentSearch(...)`, and `currentSearchKeyword()` are used consistently across tasks.
